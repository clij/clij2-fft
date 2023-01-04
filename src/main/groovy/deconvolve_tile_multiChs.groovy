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