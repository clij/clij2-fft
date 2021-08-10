# Clij FFT Python

Currently we are working on an experimental python build.  The python example requires the native (c/c++) libraries to be pre-built.  Pre-build libraries can be found on github [here](https://github.com/clij/clij2-fft/tree/master/lib) or libraries can be built using these [instructions](https://clij.github.io/clij2-fft/docs/buildlibs/build).

The Python wrapper uses [cyptes](https://docs.python.org/3/library/ctypes.html)

The code to create the wrapper is [here](https://github.com/clij/clij2-fft/blob/master/python/clij2fft/libs.py)

And an example that uses the wrapper is [here](https://github.com/clij/clij2-fft/blob/master/python/clij2fft/test_richardson_lucy.py)