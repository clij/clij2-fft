from clij2fft.libs import getlib
import numpy as np
from clij2fft.pad import pad, get_pad_size, get_next_smooth, unpad

def richardson_lucy(img, psf, numiterations, regularizationfactor, lib=None):
    """ perform Richardson-Lucy on img using psf.  The image is extended to the next
    smooth size because clfft only works on smooth sizes

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
    lib.deconv3d_32f(numiterations, int(img.shape[2]), int(img.shape[1]), int(img.shape[0]), img, shifted_psf, result, normal)
    
    # unpad and return
    return unpad(result, original_size)