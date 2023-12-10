# clij2-fft: Fast OpenCL GPU FFT based image processing algorithms for Java, Python and C++. 

The goal of clij2-fft is to provide the bio-imaging community with a fast but simple to use implementations of 2D and 3D FFT and FFT based algorithms that are usable from Java, Python, and C++ and can be used to create FFT based plugins (such as convolution and deconvolution) for many platforms.

There are many GPU deconvolution projects in Python, which are probably easier to use for Python programmers.  Clij2-fft is available in Python, but also designed so the same code can be called from Java (or from c++).  Thus the build process is a bit more complex then for Python only implementations. 

Currently most of the focus has been on an implementation of Richardson Lucy Deconvolution that includes non-circulant edge handling and total variation regularization.  We found this particularly useful in deconvolving images from instruments with extended PSFs, for which the classic version of Richardson Lucy sometimes produces ringing at high iteration numbers.  See an example [here](https://forum.image.sc/t/deconvolution-minimizing-edge-artifacts/69828/2)

Latest features and updates are tracked in the [Release Log](https://clij.github.io/clij2-fft/docs/releaselog) though this code is still in very early development.  If you are experimenting with this code base please feel free to submit an issue or ping us on the [Imaging Forum](https://forum.image.sc/).

The clij2-fft project is built upon [clFFT](https://github.com/arrayfire/clFFT)  

## Python

The python version is now available on [pypi](https://pypi.org/project/clij2-fft/)

If you are a python programmer take a look at the ```python``` sub-directory for the experimental python distribution.  See [these installation instructions](https://github.com/clij/clij2-fft/blob/master/docs/python/clij-fft-python.md) and also this [python example](https://github.com/clij/clij2-fft/tree/master/python/clij2fft/test_richardson_lucy.py) may be helpful.  In addition if you are deconvolving large images this [dask example](https://github.com/clij/clij2-fft/blob/master/python/clij2fft/test_richardson_lucy_dask.py) may be useful.

Please note thus far only a small subset of potential algorithms have been implemented.  For example we have implemented 3D deconvolution but not 2D, and 2D FFT but not 3D.  If you are in need of a particular FFT flavor, or FFT based algorithm, please ask questions on the forum.  We can prioritize future work based on that feedback and also help others implement additional algorithms. 

## Java and ImageJ

clij2-fft is shipped as part of [clij2](https://clij.github.io).  Follow [these instructions](https://clij.github.io/clij2-docs/installationInFiji) to install clij2. You can then access Richardson Lucy Deconvolution via the CLIJ2-assistant as shown in [this video](https://clij.github.io/clij2-fft/docs/deconvolution/clij-decon.mp4).

To build the code follow [these instructions](https://clij.github.io/clij2-fft/docs/buildlibs/build)

Currently clij2-fft is a work in progress.  The best way to learn how to use clij2-fft is to study the examples then follow up with questions on the [Image.sc Forum](https://forum.image.sc/).   

If you are a java programmer the clij2-fft test [here](https://github.com/clij/clij2-fft/tree/master/src/test/java/net/haesleinhuepf/clijx/tests) may be helpful.   

If you are a script programmer these [scripting examples](https://github.com/clij/clij2-fft/tree/master/src/main/jython) may be helpful.  

