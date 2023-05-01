from clij2fft.libs import getlib
import numpy as np
from clij2fft.pad import pad, get_pad_size, get_next_smooth, unpad

def richardson_lucy(img, psf, numiterations, regularizationfactor=0, first_guess=None, lib=None):
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
    
    # native code only works with 7-smooth sizes
    extended_size = get_next_smooth(padded_size)

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

    # deconvolution using clij2fft
    if regularizationfactor==0:
        lib.deconv3d_32f(numiterations, int(img.shape[2]), int(img.shape[1]), int(img.shape[0]), img, shifted_psf, result, normal)
    else:
        lib.deconv3d_32f_tv(numiterations, regularizationfactor, int(img.shape[2]), int(img.shape[1]), int(img.shape[0]), img, shifted_psf, result, normal)
    
    # unpad and return
    return unpad(result, original_size)

def richardson_lucy_nc(img, psf, numiterations, regularizationfactor=0, lib=None):
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
    
    # the native code also only works with 7-smooth sizes so extend further to the next smooth size
    extended_size = get_next_smooth(extended_size)

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

    # the normalization factor is the valid region correlated with the PSF
    lib.convcorr3d_32f(int(normal.shape[2]), int(normal.shape[1]), int(normal.shape[0]), valid, shifted_psf, normal,1)

    # get rid of any zeros in the normal to avoid divide by zero issues
    #normal[normal<0.00001]=1

    # deconvolution using clij2fft
    if regularizationfactor==0:
        lib.deconv3d_32f(numiterations, int(img.shape[2]), int(img.shape[1]), int(img.shape[0]), img, shifted_psf, result, normal)
    else:
        lib.deconv3d_32f_tv(numiterations, regularizationfactor, int(img.shape[2]), int(img.shape[1]), int(img.shape[0]), img, shifted_psf, result, normal)
    
    # unpad and return
    return unpad(result, original_size)


def richardson_lucy_interpolate(img, psf, numiterations, regularizationfactor=0, valid=None, firstguess=None, lib=None):
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

    # the native code also only works with 7-smooth sizes so extend further to the next smooth size
    extended_size = get_next_smooth(extended_size)
    
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
    lib.convcorr3d_32f(int(normal.shape[2]), int(normal.shape[1]), int(normal.shape[0]), valid, shifted_psf, normal,1)
    
    # deconvolution using clij2fft
    if regularizationfactor==0:
        lib.deconv3d_32f(numiterations, int(img.shape[2]), int(img.shape[1]), int(img.shape[0]), img, shifted_psf, result, normal)
    else:
        lib.deconv3d_32f_tv(numiterations, regularizationfactor, int(img.shape[2]), int(img.shape[1]), int(img.shape[0]), img, shifted_psf, result, normal)
    
    # unpad and return
    return unpad(result, original_size), normal, valid

