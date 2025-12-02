## Install from PIP

```pip install clij2-fft```  

## Test and verify installation  

Test images can be found [here](https://www.dropbox.com/sh/v3g5zln64e0uhk7/AABcYksPoawlTBO9ELCyqfPOa?dl=0) 

If testing in a fresh environment install  

conda install -c anaconda scikit-image
conda install matplotlib

then got to the ```python``` directory and run

```python TestInstallation.py```

# Advanced examples 

In halfway to i2k 2023 a workshop was presented on deconvolution and deep learning which contains a number of interesting examples using clij2fft.  See [here](https://github.com/True-North-Intelligent-Algorithms/deconvolution-gpu-dl-course).

Some other nice examples were developed for the POL 2023 GPU bio-image-analysis course, see [here](https://github.com/BiAPoL/PoL-BioImage-Analysis-TS-GPU-Accelerated-Image-Analysis).

In I2K2022 a number of advanced deconvolution examples, that used clij2-fft were developed, these examples are best run in an Anaconda enviroment with devbio-napari and other dependencies installed.  The examples can be found [here](https://github.com/haesleinhuepf/I2K2022-napari-workshop/tree/main/restoration)  

# Build and develop
If you would like to develop and improve the Python version, you will require the native (C/C++) libraries to be pre-built. Pre-built libraries can be found on GitHub [here](./lib) or libraries can be built using [these instructions](./docs/buildlibs/build).

The Python wrapper uses [ctypes](https://docs.python.org/3/library/ctypes.html).

The code to create the wrapper is [here](./python/clij2fft/libs.py).

An example that uses the wrapper is [here](./python/clij2fft/test_richardson_lucy.py).
