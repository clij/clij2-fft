import threading

import numpy as np
import pyopencl as cl

from clij2fft.libs import getlib
from clij2fft.pad import pad, get_pad_size, get_next_fft_size, unpad

# what the common error codes from the native calls mean
_ERRORS = {
    -1001: "no OpenCL platform found: the GPU driver isn't available (check nvidia-smi)",
    -4: "the GPU ran out of memory (CL_MEM_OBJECT_ALLOCATION_FAILURE)",
    -5: "CL_OUT_OF_RESOURCES, usually the GPU running out of memory",
    -6: "the computer ran out of memory (CL_OUT_OF_HOST_MEMORY)",
}

def _check(ret, call):
    """ raise if a native call failed; otherwise its output is not a result """
    if ret != 0:
        raise RuntimeError(f"clij2fft {call} failed with error {ret}: {_ERRORS.get(ret, 'see the OpenCL error codes')}")

# One OpenCL context per (platform, device), made on first use and reused for the life of the
# process. The native functions that make their own context on every call leave it alive (clFFT
# keeps its compiled kernels per context until clfftTeardown), so memory grew with every call.
_contexts = {}
_contexts_lock = threading.Lock()

def _get_context(platform, device):
    """ the OpenCL context and device for (platform, device); made once, then reused """
    with _contexts_lock:
        if (platform, device) not in _contexts:
            try:
                dev = cl.get_platforms()[platform].get_devices()[device]
            except cl.Error as e:
                code = getattr(e, "code", None)
                raise RuntimeError(f"clij2fft OpenCL setup failed with error {code}: {_ERRORS.get(code, str(e))}") from None
            except IndexError:
                raise RuntimeError(f"clij2fft: there is no OpenCL device {device} on platform {platform}") from None
            _contexts[(platform, device)] = (cl.Context([dev]), dev)
        return _contexts[(platform, device)]

def _gpu_buffers(ctx, arrays):
    return [cl.Buffer(ctx, cl.mem_flags.READ_WRITE | cl.mem_flags.COPY_HOST_PTR, hostbuf=np.ascontiguousarray(a, np.float32)) for a in arrays]

def _convcorr(lib, image, psf, out, correlate, platform, device):
    """ out = image convolved (correlate=False) or correlated (True) with psf, on the GPU """
    ctx, dev = _get_context(platform, device)
    queue = cl.CommandQueue(ctx, dev)
    buffers = _gpu_buffers(ctx, (image, psf, out))
    try:
        n2, n1, n0 = out.shape
        _check(lib.conv3d_32f_lp(n0, n1, n2, *(b.int_ptr for b in buffers), correlate, ctx.int_ptr, queue.int_ptr, dev.int_ptr), "conv3d_32f_lp")
        cl.enqueue_copy(queue, out, buffers[2])
    finally:
        for b in buffers:
            b.release()

def _deconv(lib, iterations, regularizationfactor, image, psf, estimate, normal, platform, device):
    """ iterations of Richardson-Lucy on the GPU, starting from estimate and writing the result into it """
    ctx, dev = _get_context(platform, device)
    queue = cl.CommandQueue(ctx, dev)
    buffers = _gpu_buffers(ctx, (image, psf, estimate, normal))
    try:
        n2, n1, n0 = image.shape
        _check(lib.deconv3d_32f_lp_tv(iterations, regularizationfactor, n0, n1, n2, *(b.int_ptr for b in buffers), ctx.int_ptr, queue.int_ptr, dev.int_ptr), "deconv3d_32f_lp_tv")
        cl.enqueue_copy(queue, estimate, buffers[2])
    finally:
        for b in buffers:
            b.release()

def richardson_lucy(img, psf, numiterations, regularizationfactor=0, first_guess=None, lib=None, platform=0, device=0):
    """ perform Richardson-Lucy on img using psf.  The image is extended to the next smooth size 
    because clfft only works on smooth sizes.  If additional extension is desired the image should 
    be extended before calling this function.  

    Args:
        img ([type]): image to be deconvolved 
        psf ([type]): point spread function 
        numiterations ([type]): [description]
        regularizationfactor ([type]): used for total varation noise regularization
        lib ([type], optional): pass in if clfft lib is already initialized

    Returns:
        [type]: deconvolved image
    """

    # native code only works with 32 bit floats
    img=img.astype(np.float32)
    psf=psf.astype(np.float32)

    original_size = img.shape

    padded_size=[max(x,y) for x,y in zip(img.shape, psf.shape)]
    
    # native code only works with 7-smooth sizes, so extend to a length from the FFT size table
    extended_size = get_next_fft_size(padded_size)

    # pad image and psf to next smooth size
    img, _ = pad(img, extended_size,'reflect')
    psf, _ = pad(psf, extended_size, 'constant')    

    # shift psf so center is at 0,0
    shifted_psf = np.fft.ifftshift(psf)
    
    # normalization factor for edge smoothing (all ones means no edge normalization)
    normal=np.ones(img.shape).astype(np.float32)
    
    # memory for result
    if (first_guess is None):
        result = np.copy(img)
    else:
        print('copy first guess')
        result = np.copy(normal)

    # if the lib wasn't passed get it
    if (lib==None):
        print('get lib')
        lib = getlib()
        cleanup = True
    else:
        cleanup = False

    # deconvolution using clij2fft
    try:
        if regularizationfactor==0:
            _deconv(lib, numiterations, 0.0, img, shifted_psf, result, normal, platform, device)
        else:
            _deconv(lib, numiterations, regularizationfactor, img, shifted_psf, result, normal, platform, device)
    finally:
        if cleanup==True:
            lib.cleanup()

    # unpad and return
    return unpad(result, original_size)

def richardson_lucy_nc(img, psf, numiterations, regularizationfactor=0, lib=None, platform=0, device=0):
    """ perform non-circulant Richardson-Lucy on img using psf.  The image is extended to the size of
    image+psf in each dimension then to the next smooth size because clfft only works on smooth sizes.
    A non-circulant normalization factor is computed, as part of the Boundary condition handling scheme 
    described here
	http://bigwww.epfl.ch/deconvolution/challenge2013/index.html?p=doc_math_rl)

    Args:
        img ([type]): image to be deconvolved 
        psf ([type]): point spread function 
        numiterations ([type]): [description]
        regularizationfactor ([type]): used for total varation noise regularization
        lib ([type], optional): pass in if clfft lib is already initialized

    Returns:
        [type]: deconvolved image
    """

    # the native code only works with 32 bit floats
    img=img.astype(np.float32)
    psf=psf.astype(np.float32)

    psf=psf/psf.sum()

    # keep track of the original size
    original_size = img.shape

    # compute the extended size    
    extended_size = [img.shape[0]+2*int(psf.shape[0]/2), img.shape[1]+2*int(psf.shape[1]/2), img.shape[2]+2*int(psf.shape[2]/2)] 
    
    # the native code also only works with 7-smooth sizes so extend further to a length from the FFT size table
    extended_size = get_next_fft_size(extended_size)

    # pad image and psf to the extended size computed above
    img, padding = pad(img, extended_size,'constant')
    psf, _ = pad(psf, extended_size, 'constant')    

    # shift psf so center is at 0,0
    shifted_psf = np.fft.ifftshift(psf)

    # create result with initial guess as a constant with constant = to image mean
    result = np.zeros(img.shape).astype('float32')
    result[:,:,:]=img.mean()

    # create normalization factor for non-circulant deconvolution
    
    start = [0,0,0]
    end = [0,0,0]

	# calculate the start and end of the original image space within the extended image space
    # we call this the valid region 
    for d in range(3):
        start[d] = padding[d][0]#int((extended_size[d]-original_size[d])/2)
        end[d]=int(start[d]+original_size[d])

    valid=np.zeros(extended_size).astype(np.float32)
    valid[start[0]:end[0],start[1]:end[1],start[2]:end[2]]=1

    normal=np.zeros(extended_size).astype(np.float32)
    # if the lib wasn't passed get it
    if (lib==None):
        print('get lib')
        lib = getlib()
        cleanup = True
    else:
        cleanup = False

    try:
        print('calling convcorr',platform, device)
        # the normalization factor is the valid region correlated with the PSF
        _convcorr(lib, valid, shifted_psf, normal, True, platform, device)

        # get rid of any zeros in the normal to avoid divide by zero issues
        # TODO: clarify why this changed
        #normal[normal<0.00001]=1

        # deconvolution using clij2fft
        if regularizationfactor==0:
            _deconv(lib, numiterations, 0.0, img, shifted_psf, result, normal, platform, device)
        else:
            _deconv(lib, numiterations, regularizationfactor, img, shifted_psf, result, normal, platform, device)
    finally:
        if cleanup==True:
            lib.cleanup()

    # unpad and return
    return unpad(result, original_size)


def richardson_lucy_interpolate(img, psf, numiterations, regularizationfactor=0, valid=None, firstguess=None, lib=None, platform=0, device=0):
    """ perform Richardson-Lucy interpolated deconvolution on img using psf. The user passes in a map of valid pixels and deconvolution
    is set up so non-valid values are 'interpolated'.  Non-valid values can be saturated areas, or areas between voxels (as to achieve sub-voxel resolution))
    Note: the interpolated values may not be representative of true structure.  The quality of results is signal dependent. 

    Note 2:  The algorithm is a general version of the non-circulant deconvolution algorithm implemented above.  If no valid is passed in, the algorithm
    will behave exactly as the non-circulant algorithm above.  If valid is passed in, the algorithm will interpolate values in the non-valid region.  

    Args:
        img (numpy array): image to be deconvolved 
        psf (numpy array): point spread function 
        numiterations (int): [description]
        regularizationfactor (float): used for total varation noise regularization
        valid (numpy array, optional): [description]. Defaults to None. Map of valid pixels in the image
        firstguess (numpy array, optional): [description]. Defaults to None. First guess for deconvolution
        lib (numpy array, optional): pass in if clfft lib is already initialized

    Returns:
        [numpy array]: deconvolved image
    """

    # native code only works with 32 bit floats
    img=img.astype(np.float32)
    psf=psf.astype(np.float32)

    original_size = img.shape

    # if no valid map passed in all pixels are valid 
    if valid is None:
        valid=np.ones(img.shape).astype(np.float32)
    
    # compute the extended size    
    extended_size = [img.shape[0]+2*int(psf.shape[0]/2), img.shape[1]+2*int(psf.shape[1]/2), img.shape[2]+2*int(psf.shape[2]/2)] 

    # the native code also only works with 7-smooth sizes so extend further to a length from the FFT size table
    extended_size = get_next_fft_size(extended_size)
    
    # pad image and psf and valid map to next smooth size
    img, _ = pad(img, extended_size,'constant')
    psf, _ = pad(psf, extended_size, 'constant')    
    valid, _ = pad(valid, extended_size, 'constant')    

    # shift psf so center is at 0,0
    shifted_psf = np.fft.ifftshift(psf)
    
    # create result array which will be initialized to the first guess if passed in, otherwise to the mean of the image 
    result = np.zeros(img.shape).astype('float32')

    if firstguess is None:
        result[:,:,:]=2*img.mean()
    else:
        result, _ = pad(firstguess, extended_size,'constant', constant_values=firstguess.mean())
   
    # if the lib wasn't passed get it
    if (lib==None):
        print('get lib')
        lib = getlib()

    normal=np.zeros(extended_size).astype(np.float32)
    
    # the normalization factor is the valid region correlated with the PSF
    _convcorr(lib, valid, shifted_psf, normal, True, platform, device)
    
    # deconvolution using clij2fft
    if regularizationfactor==0:
        _deconv(lib, numiterations, 0.0, img, shifted_psf, result, normal, platform, device)
    else:
        _deconv(lib, numiterations, regularizationfactor, img, shifted_psf, result, normal, platform, device)
    
    # unpad and return
    return unpad(result, original_size), normal, valid

