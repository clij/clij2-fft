import dask.array as da
from clij2fft.richardson_lucy import richardson_lucy_nc 
import numpy as np

def richardson_lucy_nc_dask(img, psf, iterations, reg, x_chunk_size, y_chunk_size, overlap):

    dimg = da.from_array(img,chunks=(img.shape[0], y_chunk_size, x_chunk_size))
    out = dimg.map_overlap(richardson_lucy_nc, depth={0:0, 1:overlap, 2:overlap}, dtype=np.float32, psf=psf, numiterations=iterations, regularizationfactor=reg)
    decon = out.compute(num_workers=1)

    return decon

