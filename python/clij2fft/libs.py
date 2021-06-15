from ctypes import *
import numpy as np
import numpy.ctypeslib as npct
from skimage import io
import matplotlib.pyplot as plt
import time

def getlib():
    """
    
    returns the cdll that contains the clij2 functions and sets up the function argument types using ctypes

    follow this example https://github.com/koschink/PyYacuDecu

    Returns:
        [cdll]: cdll containing clij2 functions
    """

    clij2fft=CDLL('clij2fft.dll', mode=RTLD_GLOBAL)

    array_3d_float = npct.ndpointer(dtype=np.float32, ndim=3 , flags='CONTIGUOUS')

    clij2fft.deconv3d_32f.argtypes = [c_int, c_int, c_int, c_int, array_3d_float, array_3d_float, array_3d_float, array_3d_float]

    return clij2fft


