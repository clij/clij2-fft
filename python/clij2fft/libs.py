from ctypes import *
import numpy as np
import numpy.ctypeslib as npct
from skimage import io
import matplotlib.pyplot as plt
import time
import os

def getlib():
    """
    
    returns the cdll that contains the clij2 functions and sets up the function argument types using ctypes

    follow this example https://github.com/koschink/PyYacuDecu

    Returns:
        [cdll]: cdll containing clij2 functions
    """

    if (os.name=='posix'):
        clij2fft=CDLL('libclij2fft.so', mode=RTLD_GLOBAL)
    # if not posix assume windows
    else:
        clij2fft=CDLL('clij2fft.dll', mode=RTLD_GLOBAL)
    # TODO Mac and Mac M1
    
    array_3d_float = npct.ndpointer(dtype=np.float32, ndim=3 , flags='CONTIGUOUS')

    clij2fft.deconv3d_32f.argtypes = [c_int, c_int, c_int, c_int, array_3d_float, array_3d_float, array_3d_float, array_3d_float]

    return clij2fft


