#@ OpService ops
#@ UIService ui
#@ Dataset data
#@ Dataset psf

from java.lang import System

# init CLIJ and GPU
from net.haesleinhuepf.clij import CLIJ;
from net.haesleinhuepf.clij2 import CLIJ2;
from net.haesleinhuepf.clijx.plugins import DeconvolveFFT;

# show installed OpenCL devices
print CLIJ.getAvailableDeviceNames();

# initialize a device with a given name
clij2 = CLIJ2.getInstance("RTX");
clij2.clear();

print "Using GPU: " + clij2.getGPUName();

# transfer image to the GPU
gpuImg = clij2.push(data);
gpuPSF = clij2.push(psf);

# measure start time
start = System.currentTimeMillis();

# create memory for the output image first
gpuEstimate = clij2.create(gpuImg.getDimensions(), clij2.Float);

# submit deconvolution task
DeconvolveFFT.deconvolveFFT(clij2, gpuImg, gpuPSF, gpuEstimate);

# measure end time
finish = System.currentTimeMillis();

print('CLIJ decon time ', (finish-start));
clij2.show(gpuEstimate, "GPU Decon Result");

# clean up memory
clij2.clear();

