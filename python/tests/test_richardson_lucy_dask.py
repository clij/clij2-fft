from clij2fft.richardson_lucy_dask import richardson_lucy_dask
from skimage.io import imread
import numpy as np
from matplotlib import pyplot as plt
import pyopencl as cl
import os

def test_richardson_lucy_dask():

    img = np.ones((256, 256, 128), dtype=np.float32)
    psf = np.ones((128, 128, 64), dtype=np.float32)    

    pad_z=50
    pad_y=50
    pad_x=50
    mem_to_use=1

    img = np.pad(img, [(pad_z,pad_z),(pad_y, pad_y),(pad_x, pad_x)], mode = 'constant', constant_values = 0)
    print('image shape is',img.shape)

    platforms = cl.get_platforms()

    for platform in platforms:
        # print number of devices per platform
        print("Platform: {} has {} devices".format(platform.name, len(platform.get_devices())))
        for device in platform.get_devices():
            print('    ',device)

    platform_to_use = 0
    decon=richardson_lucy_dask(img, psf, 50, 0.0001, mem_to_use=mem_to_use, platform = 0, num_devices = len(platforms[0].get_devices()), debug = False)

