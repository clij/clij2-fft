/*-
 * #%L
 * clij2 fft
 * %%
 * Copyright (C) 2020 - 2025 Robert Haase, MPI CBG
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the MPI CBG nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
#@OpService ops
#@UIService ui
#@File imp_path
#@File psf_path
#@Integer numTilesXY
#@Integer iterations
#@Double regulF

/**
 * Code adapted from 
 * https://forum.image.sc/t/clij-deconvolution-regularization-fiji-crash/55155/8
 * 
 */

IJ.run("Close All", "");

imp = IJ.openImage(imp_path.toString() );
imp = IJ.getImage();// imp.show()

psf = IJ.openImage(psf_path.toString() );
psf = IJ.getImage();// imp.show()

def info = imp.getOriginalFileInfo()
def dir  = info.directory
def fileName = info.fileName
//println info 
//println dir
//println fileName

def output_dir = new File (dir , "deconvolved_it-"+iterations+"_regF-"+regulF+"_output")
output_dir.mkdir()
def output_path = new File (output_dir , FilenameUtils.removeExtension(fileName)+"_deconvolved_it-"+iterations+"_regF"+regulF+".tif")


println 'num tiles are '+numTilesXY

def theImp = null
if (imp.getNChannels() == 1){
	theImp = deconvolutionByTile(imp, psf, numTilesXY, margin)
	
}else { 
	def deconvolved_chs = []
	(1..imp.getNChannels()).each{
	//(1..2).each{
		def ch_imp = new Duplicator().run(imp, it, it, 1, imp.getNSlices(), 1, 1);
		ch_imp.setTitle( "C"+it+"-"+imp.getTitle() )
		def psf_ch_imp = new Duplicator().run(psf, it, it, 1, psf.getNSlices(), 1, 1);
		
		deconvolved_imp = deconvolutionByTile(ch_imp, psf_ch_imp, numTilesXY,iterations,regulF as float)
	
		deconvolved_chs.add(deconvolved_imp.duplicate())
	}
	
	concat_imps =  Concatenator.run( deconvolved_chs as ImagePlus[])
	theImp = HyperStackConverter.toHyperStack(concat_imps, imp.getNChannels(), imp.getNSlices(), 1, "xytzc", "Color");
	theImp.setTitle(imp.getTitle()+"_deconvolved")
}

theImp.show()

resetChs(theImp)

// save file
def fs = new FileSaver(theImp)
fs.saveAsTiff(output_path.toString() )



/**
 * helpers functions
 */

def resetChs(img){
	(1..img.getNChannels()).each{
		img.setC(it); 
		img.setZ( Math.floor(img.getNSlices() /2) as int)
		IJ.resetMinAndMax(img);
	}
}

def deconvolutionByTile(img, psf, numTilesXY, iterations, regulF ){

	tileWidth = Math.floor(img.getWidth() / numTilesXY)
	tileHeight = Math.floor(img.getHeight() / numTilesXY)
	tileDepth = img.getNSlices();
	
	println 'tile size '+tileWidth+' '+tileHeight+' '+tileDepth

	// set margin following the "Rule of Brian"  ;) 
	// https://forum.image.sc/t/clij-deconvolution/35172/110
	println 'psf size '+psf.getWidth()+' '+psf.getHeight()+' '+psf.getNSlices();
	margin = Math.floor(psf.getWidth()/2)
	
	// initialize a device with a given name
	clij2 = CLIJ2.getInstance();
	clijx = CLIJx.getInstance();
	clij2.clear();
	clijx.clear();
	
	gpuPSF = clij2.push(psf);
	
	deconvolved = IJ.createImage(img.getTitle()+"_deconvolved", "32-bit", img.getWidth(), img.getHeight(), img.getNSlices());
	println "Processing : "+deconvolved.getTitle();
	
	for (x = 0; x <numTilesXY; x++) {
		for (y = 0; y <numTilesXY; y++) {
			println(x+' '+y)
			
			gpuImg = clij2.pushTile(img, x, y, 0, tileWidth, tileHeight, tileDepth, margin, margin, 0);
			tempOut = clij2.create(gpuImg.getDimensions(), NativeTypeEnum.Float);
	
			DeconvolveRLTVFFT.deconvolveRichardsonLucyFFT(clij2, gpuImg, gpuPSF, tempOut, iterations, regulF, false);
			//DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpuImg, gpuPSF, tempOut, iterations);
			
			clijx.pullTile(deconvolved, tempOut, x, y, 0, tileWidth, tileHeight, tileDepth, margin, margin, margin);
		}
	}

	return deconvolved
}


import ij.*
import ij.io.FileSaver
import ij.plugin.Concatenator
import ij.plugin.Duplicator
import ij.plugin.HyperStackConverter

import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clijx.plugins.*;

import org.apache.commons.io.FilenameUtils
