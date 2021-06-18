#@OpService ops
#@UIService ui
#@ImagePlus img
#@ImagePlus psf
#@Integer numTilesXY
#@Output ImagePlus deconvolved

import math
from net.haesleinhuepf.clijx import CLIJx;
from net.haesleinhuepf.clij2 import CLIJ2;
from net.haesleinhuepf.clij.coremem.enums import NativeTypeEnum;
from ij import IJ
from net.haesleinhuepf.clijx.plugins import DeconvolveRichardsonLucyFFT;

print 'num tiles are '+str(numTilesXY)

margin = 10;
			
tileWidth = math.floor(img.getWidth() / numTilesXY);
tileHeight = math.floor(img.getHeight() / numTilesXY);
tileDepth = img.getNSlices();

print 'tile size '+str(tileWidth)+' '+str(tileHeight)+' '+str(tileDepth)

# initialize a device with a given name
clij2 = CLIJ2.getInstance("RTX");
clijx=CLIJx.getInstance();
clij2.clear();

gpuPSF = clij2.push(psf);

deconvolved = IJ.createImage("deconvolved", "32-bit", img.getWidth(), img.getHeight(), img.getNSlices());

for x in range(numTilesXY):
	for y in range(numTilesXY):
		print str(x)+' '+str(y)
		
		gpuImg = clij2.pushTile(img, x, y, 0, tileWidth, tileHeight, tileDepth, margin, margin, 0);
		tempOut = clij2.create(gpuImg.getDimensions(), NativeTypeEnum.Float);
		
		DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpuImg, gpuPSF, tempOut, 100, 0.0, False);

		clijx.pullTile(deconvolved, tempOut, x, y, 0, tileWidth, tileHeight, tileDepth, margin, margin, margin);				
