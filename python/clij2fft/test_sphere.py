import matplotlib.pyplot as plt
from deconsim import psfs
from deconsim import phantoms
from deconsim import forward
import numpy as np
import microscPSF as msPSF
from deconsim.richardson_lucy import richardson_lucy, richardson_lucy_cupy
from libs import getlib
from richardson_lucy import richardson_lucy 
import time
from pad import pad, get_pad_size, get_next_smooth, unpad

xy=101

size=[50,xy,xy]
pixel_size = 0.05

rv = np.arange(0.0, 3.01, pixel_size)
zv = np.arange(-size[0]*pixel_size/2, size[0]*pixel_size/2, pixel_size)

img = phantoms.sphere3d(size,10) #rg.sphere(size, 20).astype(np.float32)

plt.imshow(img[int(size[0]/2),:,:])

psf_xyz = psfs.gibson_lanni_3D(1.4, 1.53, 1.4, pixel_size, xy, zv, 0.1)
plt.imshow(psf_xyz[int(size[0]/2),:,:])

forward = forward.forward(img, psf_xyz, 100, 100)

lib = getlib()
start = time.time()
rl_clij = richardson_lucy(forward, psf_xyz, 100, 0,lib)
end = time.time()
clijtime = end-start

start = time.time()
rl_cupy = richardson_lucy_cupy(forward, psf_xyz, 100)
end = time.time()
cupytime = end-start

print('time cupy is',cupytime)
print('time clij is',clijtime)

fig = plt.figure()
fig.add_subplot(131)
plt.imshow(forward[int(size[0]/2),:,:])
fig.add_subplot(132)
plt.imshow(rl_cupy[int(size[0]/2),:,:])
fig.add_subplot(133)
plt.imshow(rl_clij[int(size[0]/2),:,:])

plt.show()

