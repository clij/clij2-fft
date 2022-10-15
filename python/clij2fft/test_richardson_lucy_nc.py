from clij2fft.libs import getlib
import numpy as np
from clij2fft.pad import pad, get_pad_size, get_next_smooth, unpad
from skimage.io import imread
from tnia.deconvolution.psfs import gibson_lanni_3D
from tnia.plotting.projections import show_xyz_max
from tnia.viewing.napari_helper import show_image

set = 1

if set==1:
    input_name="D:\\images\\ABRF LMRG Image Analysis Study\\nuclei\\nuclei4_out_c90_dr10_image.tif"
    input_name="D:\\images\\ABRF LMRG Image Analysis Study\\nuclei\\nuclei2_out_c90_dr90_image.tif"
    #im[:,:,:]=1

    #input_name='/home/bnorthan/code/images/Bars-G10-P15-stack-cropped.tif'

    xy_psf_dim=65
    z_psf_dim=50

    size=[z_psf_dim,xy_psf_dim,xy_psf_dim]
    xy_pixel_size = 0.124
    z_pixel_size = 0.2

    NA=1.4
    ni=1.5
    ns=1.4

    emission = 0.45

elif set==2:
    input_name="D:\\images\\images\\CElegans-CY3-crop.tif"

    xy_psf_dim=65
    z_psf_dim=50

    size=[z_psf_dim,xy_psf_dim,xy_psf_dim]
    xy_pixel_size = 0.0645
    z_pixel_size = 0.160

    NA=1.4
    ni=1.5
    ns=1.4

    emission = 0.654
   
im=imread(input_name).astype('float32')

psf = gibson_lanni_3D(NA, ni, ns, xy_pixel_size, z_pixel_size, xy_psf_dim, z_psf_dim, 0, emission)
show_xyz_max(psf)

# native code only works with 32 bit floats
img=im.astype(np.float32)
psf=psf.astype(np.float32)

original_size = img.shape

extended_size = [img.shape[0]+psf.shape[0], img.shape[1]+psf.shape[1], img.shape[2]+psf.shape[2]]
# native code only works with 7-smooth sizes
extended_size = get_next_smooth(extended_size)

# pad image and psf to next smooth size
img, _ = pad(img, extended_size,'constant')
psf, _ = pad(psf, extended_size, 'constant')    


# shift psf so center is at 0,0
shifted_psf = np.fft.ifftshift(psf)

# create normalization factor for non-circulant deconvolution

# memory for result
result = np.zeros(img.shape).astype('float32')
result[:,:,:]=img.mean()

start = [0,0,0]
end = [0,0,0]

# calculate the start and end of the original image within the extended image 
for d in range(3):
    start[d] = int((extended_size[d]-original_size[d])/2)
    end[d]=int(start[d]+original_size[d])

valid=np.zeros(extended_size).astype(np.float32)
valid[start[0]:end[0],start[1]:end[1],start[2]:end[2]]=1

normal=np.zeros(extended_size).astype(np.float32)
# if the lib wasn't passed get it
try:
    lib
except:
    print('get lib')
    lib = getlib()

lib.convcorr3d_32f(int(normal.shape[2]   ), int(normal.shape[1]), int(normal.shape[0]), valid, shifted_psf, normal,1)

normal[normal<0.00001]=1

# deconvolution using clij2fft
lib.deconv3d_32f(100, int(img.shape[2]), int(img.shape[1]), int(img.shape[0]), img, shifted_psf, result, normal)

# unpad and return
decon = unpad(result, original_size)

#viewer=show_image(normal, "normal")
viewer=show_image(im, "img")
show_image(decon, "decon", viewer=viewer)
