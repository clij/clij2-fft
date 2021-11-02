from clij2fft.libs import getlib
import numpy as np
from clij2fft.pad import pad, get_pad_size, get_next_smooth, unpad

def richardson_lucy(img, psf, numiterations, regularizationfactor=0, lib=None):
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
    
    # native code only works with 7-smooth sizes
    extended_size = get_next_smooth(img.shape)

    # pad image and psf to next smooth size
    img = pad(img, extended_size,'reflect')
    psf = pad(psf, extended_size, 'constant')    

    # shift psf so center is at 0,0
    shifted_psf = np.fft.ifftshift(psf)
    
    # memory for result
    result = np.copy(img);

    # normalization factor for edge smoothing (all ones means no edge normalization)
    normal=np.ones(img.shape).astype(np.float32)

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

    # keep track of the original size
    original_size = img.shape

    # compute the extended size    
    extended_size = [img.shape[0]+psf.shape[0], img.shape[1]+psf.shape[1], img.shape[2]+psf.shape[2]]
    
    # the native code also only works with 7-smooth sizes so extend further to the next smooth size
    extended_size = get_next_smooth(extended_size)

    # pad image and psf to the extended size computed above
    img = pad(img, extended_size,'constant')
    psf = pad(psf, extended_size, 'constant')    

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
        start[d] = int((extended_size[d]-original_size[d])/2)
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
    normal[normal<0.00001]=1

    # deconvolution using clij2fft
    if regularizationfactor==0:
        lib.deconv3d_32f(numiterations, int(img.shape[2]), int(img.shape[1]), int(img.shape[0]), img, shifted_psf, result, normal)
    else:
        lib.deconv3d_32f_tv(numiterations, regularizationfactor, int(img.shape[2]), int(img.shape[1]), int(img.shape[0]), img, shifted_psf, result, normal)
    
    # unpad and return
    return unpad(result, original_size)