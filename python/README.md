# clij2-fft (featuring non-circulant rltv deconvolution)

clij2-fft is a prototype implementation of a framework for OpenCl based FFT algorithms.  The most used algorithm from this project is the OpenCL implementation of the non-circular Richardson Lucy deconvolution algorithm with total variation regularization, which can be called as follows

```
from clij2fft.richardson_lucy import richardson_lucy_nc
decon_clij2=richardson_lucy_nc(im,psf,100,0.0002)
    
```

If you need support for the library please post a question on the [Image.sc Forum](https://forum.image.sc/).

Long term we hope to integrate FFT based math more closely with the [Clic project](https://github.com/clEsperanto/CLIc_prototype).  The goal is to make it easy to write algorithms such as convolution, correlation, registration and deconvolution that consist of a series of FFTs combined with other math operations. 
