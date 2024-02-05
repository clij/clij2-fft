from clij2fft.libs import getlib
from skimage import io
import matplotlib.pyplot as plt
import numpy as np
from clij2fft.pad import pad, get_pad_size, get_next_smooth, unpad

# create arrays to test padding
img= np.ones((256, 256, 128), dtype=np.float32)
psf = np.ones((128, 128, 64), dtype=np.float32)

print(img.shape)
print(psf.shape)

padsize=get_pad_size(img,psf)
padsize=get_next_smooth(padsize)

padded, padding = pad(img, padsize, 'constant')

print("padded shape", padded.shape)
print("padding", padding)

cropped = unpad(padded, img.shape)

fig, ax = plt.subplots(1,2)
ax[0].imshow(padded.max(axis=0))
ax[1].imshow(cropped.max(axis=0))

plt.show()

