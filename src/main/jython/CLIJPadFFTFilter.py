#@ IOService io 
#@ OpService op
#@ UIService ui

from net.haesleinhuepf.clij2 import CLIJ2;
from net.haesleinhuepf.clijx.plugins import DeconvolveFFT;

from net.haesleinhuepf.clijx.plugins import ImageUtility;
from net.imglib2.view import Views;
from net.imglib2 import FinalDimensions;
from net.imagej.ops.filter.pad import DefaultPadInputFFT;
from net.imglib2.outofbounds import OutOfBoundsMirrorFactory;
from net.imglib2.util import Intervals;
from net.imagej.ops import Ops;

# test names
testData = io.open("C:\\Users\\bnort\\ImageJ2018\\ops-experiments\\images\\Bars-G10-P15-stack-cropped.tif")
psf = io.open("C:\\Users\\bnort\\ImageJ2018\\ops-experiments\\images\\PSF-Bars-stack-cropped-64.tif")

# 0 - deconvolve, 1 convolve 
filterType=0

# open the test data
imgF = op.convert().float32(testData.getImgPlus().getImg());
psfF = op.convert().float32(psf.getImgPlus());

# crop PSF - the image will be extended using PSF size
# if the PSF size is too large it will explode image size, memory needed and processing speed
# so crop just small enough to capture significant signal of PSF 
psfF = ImageUtility.cropSymmetric(psfF, [ 64, 64, 41 ], op);

# subtract min from PSF		
psfF = Views.zeroMin(ImageUtility.subtractMin(psfF, op));

# normalize PSF
psfF = Views.zeroMin(ImageUtility.normalize(psfF, op));

# compute extended dimensions based on image and PSF dimensions
extendedSize = range(0, imgF.numDimensions());

for d in range( 0, imgF.numDimensions()):
	extendedSize[d] = imgF.dimension(d) + psfF.dimension(d);

extendedDimensions = FinalDimensions(extendedSize);

# extend image
extended = op.run(DefaultPadInputFFT, imgF, extendedDimensions, False,
		OutOfBoundsMirrorFactory(OutOfBoundsMirrorFactory.Boundary.SINGLE));

# show image and PSF
ui.show("img ", imgF);
ui.show("psf ", psfF);

# get clij
clij2 = CLIJ2.getInstance("RTX");

# push extended image and psf to GPU
inputGPU = clij2.push(extended);
psfGPU = clij2.push(psfF);

# create output
output = clij2.create(inputGPU);

if filterType==0:
	# deconvolve
	DeconvolveFFT.deconvolveFFT(clij2, inputGPU, psfGPU, output);
elif filterType==1:
	# convolve
	DeconvolveFFT.convolveFFT(clij2, inputGPU, psfGPU, output);

# get padded output as an RAI
extendedOutputRAI = clij2.pullRAI(output);

# create unpadding interval
interval = Intervals.createMinMax(-extended.min(0), -extended
	.min(1), -extended.min(2), -extended.min(0) + imgF.dimension(0) -
		1, -extended.min(1) + imgF.dimension(1) - 1, -extended.min(2) +
			imgF.dimension(2) - 1);

# create an RAI for the unpadded output... we could just use a View to unpad, but performance for slicing is slow 
outputRAI = op.create().img(imgF);

# copy the unpadded interval to the output RAI
op.run(Ops.Copy.RAI, outputRAI, Views.zeroMin(Views.interval(extendedOutputRAI, interval)));

if filterType==0:
	title="deconvolved and unpadded"
elif filterType==1:
	title="convolved and unpadded" 

ui.show(title, outputRAI);
