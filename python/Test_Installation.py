from skimage import io
from clij2fft.richardson_lucy import richardson_lucy
import matplotlib.pyplot as plt

imgName='D:\\images/images/Bars-G10-P15-stack-cropped.tif'
psfName='D:\\images/images/PSF-Bars-stack-cropped.tif'

#imgName='/home/bnorthan/code/images/Bars-G10-P15-stack-cropped.tif'
#psfName='/home/bnorthan/code/images/PSF-Bars-stack-cropped.tif'

img=io.imread(imgName)
psf=io.imread(psfName)

result = richardson_lucy(img, psf, 100, 0)
result_tv = richardson_lucy(img, psf, 100, 0.001)

fig, ax = plt.subplots(1,2)
ax[0].imshow(img.max(axis=0))
ax[0].set_title('img (max projection)')

ax[1].imshow(psf.max(axis=0))
ax[1].set_title('psf (max projection)')

fig, ax = plt.subplots(1,3)
ax[0].imshow(img.max(axis=0))
ax[0].set_title('img')

ax[1].imshow(result.max(axis=0))
ax[1].set_title('result rl')

ax[2].imshow(result_tv.max(axis=0))
ax[2].set_title('result rltv')

fig.show()

input("Hit enter to close")