[![Build Status](https://github.com/clij/clij2-actions/workflows/build.yml/badge.svg)](https://github.com/clij/clij2-actions/workflows/build.yml)

Update 2025-03-12: If you are looking to work with Clij2-fft deconvolution in Fiji, please follow [these instructions](./docs/fiji/test_installation.md).

# clij2-fft: Fast OpenCL GPU FFT based image processing algorithms for Java, Python and C++

The goal of clij2-fft is to provide the bio-imaging community with fast but simple-to-use implementations of 2D and 3D FFT and FFT-based algorithms that are usable from Java, Python, and C++. This allows the same code to be called from multiple languages and used to create FFT-based plugins (such as convolution and deconvolution) for many platforms.

There are many GPU deconvolution projects in Python, which are probably easier to use for Python programmers. clij2-fft is available in Python but is also designed so the same code can be called from Java (or from C++). Thus, the build process is a bit more complex than for Python-only implementations.

Currently, most of the focus has been on an implementation of Richardson-Lucy Deconvolution that includes non-circulant edge handling and total variation regularization. We found this particularly useful in deconvolving images from instruments with extended PSFs, for which the classic version of Richardson-Lucy sometimes produces ringing at high iteration numbers. See an example [here](https://forum.image.sc/t/deconvolution-minimizing-edge-artifacts/69828/2).

Latest features and updates are tracked in the [Release Log](https://clij.github.io/clij2-fft/docs/releaselog), though this code is still in very early development. If you are experimenting with this codebase, please feel free to submit an issue or ping us on the [Imaging Forum](https://forum.image.sc/).

The clij2-fft project is built upon [clFFT](https://github.com/arrayfire/clFFT).

## Python

The Python version is now available on [PyPI](https://pypi.org/project/clij2-fft/).

If you are a Python programmer, take a look at the `python` sub-directory for the experimental Python distribution. See [these installation instructions](./docs/python/clij-fft-python.md), and also this [Python example](./python/clij2fft/test_richardson_lucy.py) may be helpful. In addition, if you are deconvolving large images, this [Dask example](./python/clij2fft/test_richardson_lucy_dask.py) may be useful.

Please note that thus far, only a small subset of potential algorithms have been implemented. For example, we have implemented 3D deconvolution but not 2D, and 2D FFT but not 3D. If you are in need of a particular FFT flavor or FFT-based algorithm, please ask questions on the forum. We can prioritize future work based on that feedback and also help others implement additional algorithms.

## Java and ImageJ

clij2-fft requires [clij2](https://clij.github.io). Follow [these instructions](https://clij.github.io/clij2-docs/installationInFiji) to install clij2. You will also need to add the following update site https://sites.imagej.net/clijx-deconvolution/

You can then access Richardson-Lucy Deconvolution via the CLIJ2-assistant, as shown in [this video](./docs/deconvolution/clij-decon.mp4).

To build the code, follow [these instructions](./docs/buildlibs/build).

Currently, clij2-fft is a work in progress. The best way to learn how to use clij2-fft is to study the examples and then follow up with questions on the [Image.sc Forum](https://forum.image.sc/).

If you are a Java programmer, the clij2-fft test [here](./src/test/java/net/haesleinhuepf/clijx/tests) may be helpful.

If you are a script programmer, these [scripting examples](./src/main/jython) may be helpful.
