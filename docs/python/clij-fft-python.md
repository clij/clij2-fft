## Install from PIP

```pip install clij2-fft```  

## Test and verify installation  

Test images can be found [here](https://www.dropbox.com/sh/v3g5zln64e0uhk7/AABcYksPoawlTBO9ELCyqfPOa?dl=0) 

If testing in a fresh environment, install:  

```
pip install scikit-image
pip install matplotlib
```

then go to the `python/interactive tests` directory and run

```bash
cd python/"interactive tests"
python test_richardson_lucy.py
```

# Advanced examples

In Halfway to I2K 2023, a workshop on deconvolution and deep learning featured several examples using clij2-fft. See [here](https://github.com/True-North-Intelligent-Algorithms/deconvolution-gpu-dl-course).

Additional examples were developed for the PoL 2023 GPU bioimage analysis course; see [here](https://github.com/BiAPoL/PoL-BioImage-Analysis-TS-GPU-Accelerated-Image-Analysis).

In I2K 2022, a number of advanced deconvolution examples that use clij2-fft were developed. These examples are best run in an Anaconda environment with devbio-napari and other dependencies installed. The examples can be found [here](https://github.com/haesleinhuepf/I2K2022-napari-workshop/tree/main/restoration).

# Build and develop
If you would like to develop and improve the Python version, you will require the native (C/C++) libraries to be pre-built. Pre-built libraries can be found on GitHub [here](../../lib) or libraries can be built using [these instructions](../buildlibs/build.md).

The Python wrapper uses [ctypes](https://docs.python.org/3/library/ctypes.html).

The code to create the wrapper is [here](./python/clij2fft/libs.py).

An example that uses the wrapper is [here](./python/interactive%20tests/test_richardson_lucy.py).
