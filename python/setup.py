from setuptools import setup, find_packages

setup(name='clij2-fft',
      version='0.18',
      description='A python wrapper around clij2 opencl FFT algorithms',
      url='https://github.com/clij/clij2-fft',
      author='Robert Haase, Brian Northan',
      author_email='bnorthan@gmail.com',
      license='BSD',
      packages=find_packages(),
      install_requires=['numpy','dask','dask-image'],
      data_files=[('Library/bin',['lib/win64/clij2fft.dll','lib/win64/clFFT.dll']), ('lib',['lib/linux64/libclij2fft.so', 'lib/linux64/libclFFT.so.2', 'lib/macosx-universal2/libclij2fft.dylib', 'lib/macosx-arm64/libclFFT.2.dylib'])],
      zip_safe=False)
