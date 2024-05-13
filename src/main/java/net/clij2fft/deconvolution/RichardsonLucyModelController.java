package net.clij2fft.deconvolution;

import org.scijava.app.StatusService;
import org.scijava.log.LogService;
import org.scijava.ui.UIService;

import ij.ImagePlus;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.imglib2cache.Clij2RichardsonLucyImglib2Cache;
import net.haesleinhuepf.clijx.imglib2cache.Lazy;
import net.haesleinhuepf.clijx.plugins.DeconvolveRichardsonLucyFFT;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.real.FloatType;

public class RichardsonLucyModelController {
	
	OpService ops;
	LogService log;
	StatusService status;
	
	int psfType;
	int iterations;
	float regularizaitonFactor;
	boolean useCells;
	int xyCellSize;
	int zCellSize;
	float xySpacing;
	float zSpacing;
	float wavelength;
	float NA;
	float riImmersion;
	float riSample;
	float PSFDepth;
	int PsfXYSize;
	int PsfZSize;
	
	RichardsonLucyModelController(OpService ops, LogService log, StatusService status) {
		this.ops = ops;
		this.log = log;
		this.status = status;
	}
	
	public void setStatusService(StatusService status) {
		this.status = status;
	}
	
	public float getXYSpacing() {
		return xySpacing;
	}

	public void setXYSpacing(float xSpacing) {
		this.xySpacing = xSpacing;
	}
	
	public int getPsfType() {
	    return psfType;
	}

	public void setPsfType(int psfType) {
	    this.psfType = psfType;
	}

	public int getIterations() {
	    return iterations;
	}

	public void setIterations(int iterations) {
	    this.iterations = iterations;
	}

	public float getRegularizaitonFactor() {
	    return regularizaitonFactor;
	}

	public void setRegularizaitonFactor(float regularizaitonFactor) {
	    this.regularizaitonFactor = regularizaitonFactor;
	}

	public boolean isUseCells() {
	    return useCells;
	}

	public void setUseCells(boolean useCells) {
	    this.useCells = useCells;
	}

	public int getXyCellSize() {
	    return xyCellSize;
	}

	public void setXyCellSize(int xyCellSize) {
	    this.xyCellSize = xyCellSize;
	}

	public int getzCellSize() {
	    return zCellSize;
	}

	public void setzCellSize(int zCellSize) {
	    this.zCellSize = zCellSize;
	}

	public float getZSpacing() {
	    return zSpacing;
	}

	public void setZSpacing(float zSpacing) {
	    this.zSpacing = zSpacing;
	}

	public float getWavelength() {
	    return wavelength;
	}

	public void setWavelength(float wavelength) {
	    this.wavelength = wavelength;
	}

	public float getNA() {
	    return NA;
	}

	public void setNA(float NA) {
	    this.NA = NA;
	}

	public float getRiImmersion() {
	    return riImmersion;
	}

	public void setRiImmersion(float riImmersion) {
	    this.riImmersion = riImmersion;
	}

	public float getRiSample() {
	    return riSample;
	}

	public void setRiSample(float riSample) {
	    this.riSample = riSample;
	}

	public float getPSFDepth() {
	    return PSFDepth;
	}

	public void setPSFDepth(float PSFDepth) {
	    this.PSFDepth = PSFDepth;
	}

	public int getPsfXYSize() {
	    return PsfXYSize;
	}

	public void setPsfXYSize(int XYSize) {
	    this.PsfXYSize = XYSize;
	}

	public int getPsfZSize() {
	    return PsfZSize;
	}

	public void setPsfZSize(int ZSize) {
	    this.PsfZSize = ZSize;
	}
		
	
	public void runDeconvolution(ImagePlus imp, ImagePlus psf, int numIterations) {
		

		CLIJ2 clij2=null;
		
		// get clij
		try {
			clij2 = CLIJ2.getInstance("RTX");
		}
		catch(Exception e) {
			System.out.println(e);
			return;
		}

		ClearCLBuffer gpu_psf = clij2.push(psf);
		ClearCLBuffer gpu_image = clij2.push(imp);
		
		ClearCLBuffer gpu_deconvolved = clij2.create(gpu_image.getDimensions(), NativeTypeEnum.Float);
		
		
				if (!this.useCells) {
			DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpu_image, gpu_psf, gpu_deconvolved, 100, 0.002f, true);

			ImagePlus out = clij2.pull(gpu_deconvolved);
			out.show();
		}
		else {
			
			log.error("show me");
			log.info("Performing Cell deconvolution "+this.useCells);
			
			log.info("cell xy "+this.xyCellSize);
			log.info("cell z "+this.zCellSize);
			
			status.showStatus(50, 100, "Deconvolving");
		

			Img<FloatType> img = ImageJFunctions.convertFloat(imp);
			
			// create the version of clij2 RL that works on cells
			Clij2RichardsonLucyImglib2Cache<FloatType, FloatType> op = new Clij2RichardsonLucyImglib2Cache<FloatType, FloatType>(
				img, gpu_psf, 10, 10, 10);
			
			op.setUpStatus(status, 25);

			// here we use the imglib2cache lazy 'generate' utility
			// first parameter is the image to process
			// second parameter is the cell size (which we set to half the original dimension in each direction)
			CachedCellImg<FloatType, RandomAccessibleInterval<FloatType>> decon = (CachedCellImg) Lazy.generate(img,
					new int[] { (int) img.dimension(0) / 2, (int) img.dimension(1) / 2, (int) img.dimension(2) / 2 },
					new FloatType(), AccessFlags.setOf(AccessFlags.VOLATILE), op);
	
			// trigger processing of the entire volume
			// (otherwise processing will be triggerred as we view different parts of the volume,
			// which is sometimes the behavior we want, but sometiems we'd rather process everything before hand,
			// so viewing is responsive)
			// so in this case the main reason for using imglib2 cache is as a convenient mechanism for chunking the image
			// and we don't fully take advantage of the 'just in time' aspect. 
			decon.getCells().forEach(Cell::getData);
			
			ImagePlus out = ImageJFunctions.show(decon);
			
		}
	}

}
