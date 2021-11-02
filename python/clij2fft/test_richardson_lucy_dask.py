import dask_image.imread
from skimage import io
import numpy as np
from richardson_lucy import richardson_lucy
import matplotlib.pyplot as plt
from clij2fft.libs import getlib

# example inspired by code from https://github.com/psobolewskiPhD
# and this forum discussion https://forum.image.sc/t/migrating-from-clij-to-pyclesperanto/54985/20

# define image paths (change these to local paths)
imgName='D:\\images/images/Bars-G10-P15-stack-cropped.tif'
psfName='D:\\images/images/PSF-Bars-stack-cropped.tif'

# imgName='/home/bnorthan/code/images/Bars-G10-P15-stack-cropped.tif'
# psfName='/home/bnorthan/code/images/PSF-Bars-stack-cropped.tif'

# create a dask image
dimage = dask_image.imread.imread(imgName)

# chunk the image... the bars image likely fits into (most) GPUs memory so this
# example is just illustrative of the API.  In a real scenario we want to make 
# the chunk size the maximum size for which the image and temp buffers will fit in the 
# the GPU.  (For Richardson Lucy we do calculations in 32 bit floating point and need
# 6 copies of the image solve 6*S = GPU_Memory, and S will be the chunk size, then we (often)
# fix z and choose x and y for S). s
dimage_r = dimage.rechunk(chunks=(128, 128, 128)).astype(np.float32)

# open the PSF, in this case don't make it a dask image
# PSF should be much smaller than image
psf=io.imread(psfName)

# define the PSF XY half size and the XY overlap, we want the PSF half size to be smaller than the overlap
psfHalfSize = 16
overlap = 24

# crop PSF using PSFHalfSize
psf=psf[:,int(psf.shape[1]/2)-psfHalfSize:int(psf.shape[1]/2)+psfHalfSize-1,int(psf.shape[2]/2)-psfHalfSize:int(psf.shape[2]/2)+psfHalfSize-1]

lib = getlib() 
i=0

def deconv_ocl(stack, psf=psf, iter=100, lib=lib):
    print(stack.shape,psf.shape)
    result = richardson_lucy(stack, psf, iter, 0, lib)
    return result
    #return stack

out = dimage_r.map_overlap(deconv_ocl, depth={0:0, 1:overlap, 2:overlap}, boundary='reflect', dtype=np.float32)
#out = dimage_r.map_overlap(deconv_ocl, depth={0:0, 1:0, 2:0}, boundary='reflect', dtype=np.float32)
#out_b = dimage_r.map_blocks(deconv_ocl, dtype=np.float32)

# compute with 1 worker (to avoid accessing the GPU via different threads)
test = out.compute(num_workers=1)

#max_projection = out_b.max(axis=0)

plt.imshow(test.max(axis=0))
plt.show()