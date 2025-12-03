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
# @File(label="source directory", style="directory") input_dir
# @File(label="choose output directory", style="directory") output_dir
# @File(label="PSF File", style="file") psf_file
# @IOService io
# @UIService ui
# @OpService ops
# @Boolean(value=False) use_tiles
# @Boolean(value=False) show_decon
# @DatasetService ds

import os
from net.haesleinhuepf.clijx.plugins import clij2fftWrapper;
from net.haesleinhuepf.clijx.imglib2cache import Clij2RichardsonLucyImglib2Cache;
from net.haesleinhuepf.clijx.parallel import CLIJxPool
from net.haesleinhuepf.clijx.imglib2cache import Lazy;
from net.imglib2.type.numeric.real import FloatType;
from net.imglib2.img.basictypeaccess import AccessFlags
from net.haesleinhuepf.clij2 import CLIJ2;
from net.imglib2.img.display.imagej import ImageJFunctions;
from net.haesleinhuepf.clijx.plugins import DeconvolveRichardsonLucyFFT;
from net.haesleinhuepf.clij.coremem.enums import NativeTypeEnum;

clij2 = CLIJ2.getInstance()

clij2fftWrapper.diagnostic()

files = os.listdir(input_dir.getPath())
psf = io.open(psf_file.getPath()).getImgPlus()
#ui.show(psf)

for file in files:
	
	full_name = os.path.join(input_dir.getPath(),file)
	
	base_name, ext = os.path.splitext(file)  # Splitting filename and extension
	new_name = "{}_deconvolved.tif".format(base_name)  # Appending new suffix
	deconvolved_name = os.path.join(output_dir.getPath(), new_name)
	
	img = io.open(full_name).getImgPlus()
	img = ops.convert().float32(img)
	#ui.show(img)
	
	if use_tiles:
		print('deconvolving', base_name,'using tiles')
		op = Clij2RichardsonLucyImglib2Cache.builder().rai(img).psf(psf).overlap(10,10,10).regularizationFactor(0.0002).numberOfIterations(100).useGPUPool(CLIJxPool.getInstance()).build();	
		decon = Lazy.generate(img, [img.dimension(0)/2, img.dimension(1)/2, img.dimension(2)], FloatType(), AccessFlags.setOf(AccessFlags.VOLATILE), op);
		decon.getCells().forEach(lambda cell: cell.getData())
	else:
		print('deconvolving', base_name)
		gpu_psf = clij2.push(psf);
		gpu_image = clij2.push(img);
		gpu_decon = clij2.create(gpu_image.getDimensions(), NativeTypeEnum.Float)
		
		DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpu_image, gpu_psf, gpu_decon, 100, 0.0002, True);
		
		decon = clij2.pullRAI(gpu_decon)
		
		gpu_psf.close()
		gpu_image.close()
		gpu_decon.close()
		
	if show_decon:
		ui.show(new_name, decon)
	
	io.save(ds.create(decon), deconvolved_name)
	
	
