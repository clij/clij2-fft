#@OpService ops
#@UIService ui
#@Dataset img

from java.lang import System

# init CLIJ and GPU
from net.haesleinhuepf.clij import CLIJ;
from net.haesleinhuepf.clij2 import CLIJ2;
from net.haesleinhuepf.clijx.plugins import Normalize;
from net.haesleinhuepf.clijx.plugins import DeconvolveRichardsonLucyFFT;
from net.imglib2.type.numeric.real import FloatType;
from net.imglib2 import FinalDimensions

numericalAperture = 0.95
wavelength = 461E-9
riSample = 1.33
riImmersion = 1.00
xySpacing = 108E-9
zSpacing = 1000E-9
depth = 0.0
psfDims = FinalDimensions([64, 64, 40]);
confocal = True;

psf =  ops.create().kernelDiffraction(psfDims, numericalAperture, wavelength, riSample, riImmersion, xySpacing,
		zSpacing, depth, FloatType());

if confocal:
	ops.math().multiply(psf, psf, psf)

ui.show(psf)

# show installed OpenCL devices
print CLIJ.getAvailableDeviceNames();

# initialize a device with a given name
clij2 = CLIJ2.getInstance("RTX");
clij2.clear();

print "Using GPU: " + clij2.getGPUName();

imgF=ops.convert().float32(img);
psfF=ops.convert().float32(psf);

# transfer image to the GPU
gpuImg = clij2.push(imgF);
gpuPSF = clij2.push(psfF);

# measure start time
start = System.currentTimeMillis();

# create memory for the output image first
gpuEstimate = clij2.create(gpuImg.getDimensions(), clij2.Float);

# submit deconvolution task
num_iterations = 100;
DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpuImg, gpuPSF, gpuEstimate, num_iterations);

# measure end time
finish = System.currentTimeMillis();

print('CLIJ decon time ', (finish-start));
clij2.show(gpuEstimate, "GPU Decon Result");

# clean up memory
clij2.clear();

