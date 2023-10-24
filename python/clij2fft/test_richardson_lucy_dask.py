from richardson_lucy_dask import richardson_lucy_dask
from skimage.io import imread
import numpy as np
from matplotlib import pyplot as plt

img_name=r'D:\\images/images/Bars-G10-P15-stack-cropped.tif'
psf_name=r'D:\\images/images/PSF-Bars-stack-cropped.tif'

img=imread(img_name)
print('image shape is',img.shape)

pad_z=50
pad_y=291
pad_x=700
mem_to_use=8

img = np.pad(img, [(pad_z,pad_z),(pad_y, pad_y),(pad_x, pad_x)], mode = 'constant', constant_values = 0)
print('image shape is',img.shape)
psf=imread(psf_name)

decon=richardson_lucy_dask(img, psf, 100, 0.0001, mem_to_use=mem_to_use)

plt.imshow(decon.max(axis=0))
plt.show()
