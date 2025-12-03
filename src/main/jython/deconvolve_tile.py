###
# #%L
# clij2 fft
# %%
# Copyright (C) 2020 - 2025 Robert Haase, MPI CBG
# %%
# Redistribution and use in source and binary forms, with or without modification,
# are permitted provided that the following conditions are met:
# 
# 1. Redistributions of source code must retain the above copyright notice, this
#    list of conditions and the following disclaimer.
# 
# 2. Redistributions in binary form must reproduce the above copyright notice,
#    this list of conditions and the following disclaimer in the documentation
#    and/or other materials provided with the distribution.
# 
# 3. Neither the name of the MPI CBG nor the names of its contributors
#    may be used to endorse or promote products derived from this software without
#    specific prior written permission.
# 
# THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
# ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
# WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
# IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
# INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
# BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
# DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
# LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
# OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
# OF THE POSSIBILITY OF SUCH DAMAGE.
# #L%
###
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
