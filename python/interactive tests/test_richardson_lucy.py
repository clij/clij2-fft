from clij2fft.libs import getlib
from skimage.io import imread
from clij2fft.richardson_lucy import richardson_lucy, richardson_lucy_nc
import matplotlib.pyplot as plt
import numpy as np
import os

use_ones = True

# we have the option of just using arrays of ones to test that the code runs...
if use_ones:
    img = np.ones((256, 256, 128), dtype=np.float32)
    psf = np.ones((128, 128, 64), dtype=np.float32)    
# ...or we can use real images
else:
    clij2fft_images_path = r'/home/bnorthan/images/clij2-fft-images'

    img_name=os.path.join(clij2fft_images_path, 'Bars-G10-P15-stack.tif')
    psf_name=os.path.join(clij2fft_images_path, 'PSF-Bars-stack.tif')

    img=imread(img_name)
    print('image shape is',img.shape)
    psf=imread(psf_name)

try:
    lib
except:
    print('get lib')
    lib = getlib()

result = richardson_lucy(img, psf, 100, 0, lib=lib,platform=0,device=0)
result_nc = richardson_lucy_nc(img, psf, 100, 0, lib=lib)
result_tv = richardson_lucy(img, psf, 100, 0.001, lib=lib)


if not use_ones:

    fig, ax = plt.subplots(1,2)
    ax[0].imshow(img.max(axis=0))
    ax[0].set_title('img (max projection)')

    ax[1].imshow(psf.max(axis=0))
    ax[1].set_title('psf (max projection)')

    fig, ax = plt.subplots(1,4)
    ax[0].imshow(img.max(axis=0))
    ax[0].set_title('img')

    ax[1].imshow(result.max(axis=0))
    ax[1].set_title('result rl')

    ax[2].imshow(result_tv.max(axis=0))
    ax[2].set_title('result rltv')

    ax[3].imshow(result_nc.max(axis=0))
    ax[3].set_title('result nc')

    plt.show()

lib.cleanup()
