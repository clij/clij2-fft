from ctypes import *
import numpy as np
import numpy.ctypeslib as npct
import os
import sys

def getlib():
    """
    
    returns the cdll that contains the clij2 functions and sets up the function argument types using ctypes

    the code is inspired by this example https://github.com/koschink/PyYacuDecu

    note the clij2fft library must be on the system path for the CDLL to be set up properly

    Returns:
        [cdll]: cdll containing clij2 functions
    """

    # here we load the clij2fft library.  The library is 
    # expected to be somewhere on the library path of the 
    # OS and application.   
    if os.name=='posix' and sys.platform!='darwin':
        clij2fft=CDLL('libclij2fft.so', mode=RTLD_GLOBAL)
    elif os.name=='posix' and sys.platform =='darwin':
        clij2fft=CDLL('libclij2fft.dylib', mode=RTLD_GLOBAL)
    # if not posix assume windows
    else:
        clij2fft=CDLL('clij2fft.dll', mode=RTLD_GLOBAL)
    
    array_3d_float = npct.ndpointer(dtype=np.float32, ndim=3 , flags='CONTIGUOUS')

    clij2fft.deconv3d_32f.argtypes = [c_int, c_int, c_int, c_int, array_3d_float, array_3d_float, array_3d_float, array_3d_float]
    clij2fft.deconv3d_32f_tv.argtypes = [c_int, c_float, c_int, c_int, c_int, array_3d_float, array_3d_float, array_3d_float, array_3d_float]
    clij2fft.convcorr3d_32f.argtypes = [c_int, c_int, c_int, array_3d_float, array_3d_float, array_3d_float, c_bool]

    return clij2fft


