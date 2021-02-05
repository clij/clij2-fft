# Release Log

CLIJ2-FFT is still in beta so the release process is somewhat informal.    

The release log is updated when major changes are merged to github.  The changes may take a couple of weeks to get on the Fiji update site depending on the release schedule of Clij2.  If you would like to test the newest changes post a request on the [Imagesc Forum](https://forum.image.sc/).  

## 2.2.0.14

1.  Total Variation regularization option added.  This option is useful when deconvoling noisy images.  
2.  Non-ciculant deconvolution option added.  This option is useful when deconvolving extended objects and structure near edges.   
3.  Update some pointers from 'long' to 'long long' to address issues that could occur addressing 64 bit memory locations.  

## Pre 2.2.0.14 

Implementation of core RL algorithm within OpenCL and CLIJ framework. 