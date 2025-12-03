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
package net.clij2fft.deconvolution;

import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import ij.ImagePlus;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.imglib2cache.Clij2RichardsonLucyImglib2Cache;
import net.haesleinhuepf.clijx.imglib2cache.Lazy;
import net.haesleinhuepf.clijx.plugins.DeconvolveRichardsonLucyFFT;
import net.imagej.ops.OpService;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RealLocalizable;
import net.imglib2.algorithm.labeling.ConnectedComponents.StructuringElement;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.Img;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.roi.labeling.ImgLabeling;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.type.numeric.real.FloatType;

public class RichardsonLucyModelController {
	
	public enum PSFTypeEnum {
	    MEASURED_SINGLE_BEAD,
	    EXTRACTED_FROM_MULTIPLE_BEADS,
	    GIBSON_LANNI,
	    GAUSSIAN
	}
	
	OpService ops;
	LogService log;
	StatusService status;
	
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
	float confocalFactor=1;
	
	float sigmaXY=2.f;
	float sigmaZ=3.f;
	
	PSFTypeEnum psfType;
	
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
	
	public PSFTypeEnum getPSFType() {
	    return psfType;
	}

	public void setPSFType(PSFTypeEnum psfType) {
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
	
	public void setConfocalFactor(float confocalFactor) {
		this.confocalFactor = confocalFactor;
	}
	
	public void setSigmaXY(float sigmaXY) {
		this.sigmaXY = sigmaXY;
	}
	
	public void setSigmaZ(float sigmaZ) {
		this.sigmaZ = sigmaZ;
	}
	
	public Img computePSF(PSFTypeEnum psfType, Img beads) {
		
		//this.status.showStatus(0, 2, "");
		
		if (psfType == PSFTypeEnum.GIBSON_LANNI) {
			this.status.showStatus(1, 2, "Computing Gibson Lanni PSF");
			
			FinalDimensions psfSize=new FinalDimensions(PsfXYSize, PsfXYSize, PsfZSize);

			
			Img<FloatType> psf = ops.create().kernelDiffraction(psfSize, NA, wavelength,
					riSample, riImmersion, xySpacing, zSpacing, PSFDepth, new FloatType());
			
			// raise PSF to power of confocalFactor
			// If confocalFactor = 2 this is equivalent to squaring PSF to approximate confocal
			// If confocalFactor > 1 and < 2 this is a estimate of partial confocal (spinning disc) PSF
			// Note that this method is a somewhat ad-hoc approximation of confocal PSF that experimentally is useful
			// to increase contrast in confocal/spinning disc images
			if (confocalFactor > 1.0 & confocalFactor < 2) {
				for (FloatType p:psf) {
					float val = p.getRealFloat()*p.getRealFloat();
					
					p.setReal(val);
					
				}
			}
			
			ImageJFunctions.show(psf);
			
			return psf;
	
		}
		else if (psfType == PSFTypeEnum.EXTRACTED_FROM_MULTIPLE_BEADS) {
			this.status.showStatus(1, 2, "Extracting PSF from beads");
			
			// get dimensions of bead image
			long xSize=beads.dimension(0);
			long ySize=beads.dimension(1);
			long zSize=beads.dimension(2);

			// create an empty image to store the points
			Img points=ops.create().img(new FinalDimensions(new long[] {xSize, ySize, zSize}), new FloatType());
			
			// otsu threshold to find the beads
			Img thresholded = (Img)ops.threshold().otsu(beads);
			
			// call connected components to label each connected region
			ImgLabeling labeling=ops.labeling().cca(thresholded, StructuringElement.FOUR_CONNECTED);
			
			// get the index image (each object will have a unique gray level)
			Img labelingIndex=(Img)labeling.getIndexImg();
			
			// get the collection of regions and loop through them
			LabelRegions<Integer> regions=new LabelRegions(labeling);
			
			for (LabelRegion<Integer> region:regions) {

				// get the center of mass of the region
				RealLocalizable center=region.getCenterOfMass();

				// place a point at the bead centroid
				RandomAccess<FloatType> randomAccess= points.randomAccess();
				randomAccess.setPosition(new long[]{(long)(center.getFloatPosition(0)), (long)(center.getFloatPosition(1)), (long)(center.getFloatPosition(2))});
				randomAccess.get().setReal(255.0);
			}
			
			CLIJ2 clij2=null;
			
			// get clij
			try {
				clij2 = CLIJ2.getInstance("RTX");
			}
			catch(Exception e) {
				System.out.println(e);
				return null;
			}
			
			ImageJFunctions.show(thresholded, "Thresholded Beads");
			
			ClearCLBuffer gpuBeadCentroids = clij2.push(points);
			ClearCLBuffer gpuImage = clij2.push(beads);
			ClearCLBuffer gpuPSF= clij2.create(gpuImage.getDimensions(), NativeTypeEnum.Float);
			
			DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpuImage, gpuBeadCentroids, gpuPSF, 100, 0, true);

			Img psf = (Img)clij2.pullRAI(gpuPSF);
			ImageJFunctions.show(psf, "Extracted PSF");
			
			gpuBeadCentroids.close();
			gpuImage.close();
			gpuPSF.close();
			this.status.showStatus(0, 2, "Finished extracting PSF from beads");
			
			return psf;

		}
		else if (psfType == PSFTypeEnum.GAUSSIAN) {
			this.status.showStatus(1, 2, "Computing Gaussian PSF");
		
			Img psf=(Img)ops.create().kernelGauss(new double[] {sigmaXY, sigmaXY, sigmaZ}, new FloatType());
			
			ImageJFunctions.show(psf, "Gaussian PSF");
			
			return psf;
		}
		
		return null; 
	}
		
	
	public void runDeconvolution(ImagePlus imp, Img psf, int numIterations) {
		

		CLIJ2 clij2=null;
		
		// get clij
		try {
			clij2 = CLIJ2.getInstance("RTX");
			this.status.showStatus(0, 2, "Using device "+clij2.getGPUName());
		}
		catch(Exception e) {
			System.out.println(e);
			return;
		}

		ClearCLBuffer gpu_psf = clij2.push(psf);
		ClearCLBuffer gpu_image = clij2.push(imp);
		
		ClearCLBuffer gpu_deconvolved = clij2.create(gpu_image.getDimensions(), NativeTypeEnum.Float);
		
		
		if (!this.useCells) {
			this.status.showStatus(1, 2, "cell mode is off");
			this.status.showStatus(1, 2, "deconvolving volume using " + iterations + " iterations" );
			
			DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpu_image, gpu_psf, gpu_deconvolved, iterations, regularizaitonFactor, true);

			ImagePlus out = clij2.pull(gpu_deconvolved);
			out.show();
			out.setTitle("Deconvolved");
			
			gpu_psf.close();
			gpu_image.close();
			gpu_deconvolved.close();
		}
		else {
			
			log.info("Performing Cell deconvolution "+this.useCells);
		
			
			log.info("cell xy "+this.xyCellSize);
			log.info("cell z "+this.zCellSize);
			
			
			
			
			Img<FloatType> img = ImageJFunctions.convertFloat(imp);

			log.info("image size x "+img.dimension(0));
			log.info("image size y "+img.dimension(1));
			log.info("image size z "+img.dimension(2));
			
			int numDivisionsX = (int)(Math.ceil((float)img.dimension(0)/(float)this.xyCellSize));
			int numDivisionsY = (int)(Math.ceil((float)img.dimension(1)/(float)this.xyCellSize));
			int numDivisionsZ = (int)(Math.ceil((float)img.dimension(2)/(float)this.zCellSize));
			
			int numCells = numDivisionsX*numDivisionsY*numDivisionsZ;
			
			// create the version of clij2 RL that works on cells
			Clij2RichardsonLucyImglib2Cache<FloatType, ?, ?> op =
					Clij2RichardsonLucyImglib2Cache.builder().rai(img).psf(psf).overlap(10,10,10).numberOfIterations(iterations).build();
			
			op.setUpStatus(status, numCells);

			// here we use the imglib2cache lazy 'generate' utility
			// first parameter is the image to process
			// second parameter is the cell size (which we set to half the original dimension in each direction)
			
			CachedCellImg<FloatType, ArrayDataAccess<FloatType>> decon = (CachedCellImg) Lazy.generate(img,
					new int[] { (int) this.xyCellSize, (int) this.xyCellSize, (int) this.zCellSize },
					new FloatType(), AccessFlags.setOf(AccessFlags.VOLATILE), op);
			
			// trigger processing of the entire volume
			// (otherwise processing will be triggerred as we view different parts of the volume,
			// which is sometimes the behavior we want, but sometiems we'd rather process everything before hand,
			// so viewing is responsive)
			// so in this case the main reason for using imglib2 cache is as a convenient mechanism for chunking the image
			// and we don't fully take advantage of the 'just in time' aspect. 
			decon.getCells().forEach(Cell::getData);
			
			ImagePlus out = ImageJFunctions.show(decon, "Deconvolution");
			
		}
		// reset progress
		this.status.showStatus(0, 2, "");
	}

}
