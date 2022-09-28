## Install

(if testing in new conda environment install pip ```conda install -c anaconda pip```)

```pip install --index-url https://test.pypi.org/simple/ --no-deps clij2-fft```  

## Test and verify installation  

Test images can be found here  

https://www.dropbox.com/sh/v3g5zln64e0uhk7/AABcYksPoawlTBO9ELCyqfPOa?dl=0  

If testing in a fresh environment install  

conda install -c anaconda scikit-image
conda install matplotlib

then got to the ```python``` directory and run

```python TestInstallation.py```

# Advanced examples 

In I2K2022 a number of advanced deconvolution examples, that used clij2-fft were developed, these examples are best run in an Anaconda enviroment with devbio-napari and other dependencies installed.  The examples can be found [here](https://github.com/haesleinhuepf/I2K2022-napari-workshop/tree/main/restoration)  

# Build and develop

If you would like to develop and improve the python version you will require the native (c/c++) libraries to be pre-built.  Pre-build libraries can be found on github [here](https://github.com/clij/clij2-fft/tree/master/lib) or libraries can be built using these [instructions](https://clij.github.io/clij2-fft/docs/buildlibs/build).

The Python wrapper uses [ctypes](https://docs.python.org/3/library/ctypes.html)

The code to create the wrapper is [here](https://github.com/clij/clij2-fft/blob/master/python/clij2fft/libs.py)

And an example that uses the wrapper is [here](https://github.com/clij/clij2-fft/blob/master/python/clij2fft/test_richardson_lucy.py)