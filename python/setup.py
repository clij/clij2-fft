from setuptools import setup, find_packages

setup(name='clij2-fft',
      version='0.1',
      description='A python wrapper around clij2 opencl FFT algorithms',
      url='https://github.com/clij/clij2-fft',
      author='Robert Haase, Brian Northan',
      author_email='bnorthan@gmail.com',
      license='BSD',
      packages=find_packages(),
      data_files=[('',['lib/win64/clij2fft.dll','lib/win64/clFFT.dll'])],
      zip_safe=False)
