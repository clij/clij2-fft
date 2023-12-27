from richardson_lucy_dask import richardson_lucy_dask
from skimage.io import imread
import numpy as np
from matplotlib import pyplot as plt
import pyopencl as cl

img_name=r'D:\\images/images/Bars-G10-P15-stack-cropped.tif'
psf_name=r'D:\\images/images/PSF-Bars-stack-cropped.tif'

img_name=r'/home/bnorthan/images/deconvolution/Bars-G10-P15-stack.tif'
psf_name=r'/home/bnorthan/images/deconvolution/PSF-Bars-stack.tif'

#img_name = r'C:\Users\Administrator\data\Bars-G10-P15-stack.tif'
#psf_name = r'C:\Users\Administrator\data\PSF-Bars-stack.tif'

img=imread(img_name)
print('image shape is',img.shape)

pad_z=50
pad_y=50
pad_x=50
mem_to_use=1

img = np.pad(img, [(pad_z,pad_z),(pad_y, pad_y),(pad_x, pad_x)], mode = 'constant', constant_values = 0)
print('image shape is',img.shape)
psf=imread(psf_name)

platforms = cl.get_platforms()

for platform in platforms:
    # print number of devices per platform
    print("Platform: {} has {} devices".format(platform.name, len(platform.get_devices())))
    for device in platform.get_devices():
        print('    ',device)

platform_to_use = 0
decon=richardson_lucy_dask(img, psf, 50, 0.0001, mem_to_use=mem_to_use, platform = 0, num_devices = len(platforms[0].get_devices()), debug = False)

fig, ax = plt.subplots(1,2)
ax[0].imshow(img.max(axis=0))
ax[0].set_title('img')

ax[1].imshow(decon.max(axis=0))
ax[1].set_title('deconvolution')

plt.show()
