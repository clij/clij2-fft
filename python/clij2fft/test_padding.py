from clij2fft.libs import getlib
from skimage import io
import matplotlib.pyplot as plt
import numpy as np
from pad import pad, get_pad_size, get_next_smooth, unpad

# open image and psf
imgName='D:\\images/images/Bars-G10-P15-stack-cropped.tif'
psfName='D:\\images/images/PSF-Bars-stack-cropped.tif'

img=io.imread(imgName)
psf=io.imread(psfName)

print(img.shape)
print(psf.shape)

padsize=get_pad_size(img,psf)
padsize=get_next_smooth(padsize)

padded = pad(img, padsize, 'constant')
cropped = unpad(padded, img.shape)

fig, ax = plt.subplots(1,2)
ax[0].imshow(padded.max(axis=0))
ax[1].imshow(cropped.max(axis=0))

plt.show()

