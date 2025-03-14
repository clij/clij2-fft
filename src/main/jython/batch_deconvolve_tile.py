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
	
	