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

import ij.CompositeImage;
import ij.ImagePlus;
import ij.plugin.RGBStackMerge;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.imglib2cache.Clij2RichardsonLucyImglib2Cache;
import net.haesleinhuepf.clijx.imglib2cache.Lazy;
import net.haesleinhuepf.clijx.plugins.DeconvolveRichardsonLucyFFT;
import net.imagej.ops.OpService;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
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
import net.imglib2.view.Views;
import net.imglib2.converter.Converter;
import net.imglib2.converter.Converters;
import net.imglib2.type.numeric.RealType;

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
	int xCellSize;
	int yCellSize;
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

	// the OpenCL device: a name from CLIJ's device list, or part of one; CLIJ takes the
	// first device whose name contains it, else the first GPU, else the first device
	String deviceName = "RTX";

	RichardsonLucyModelController(OpService ops, LogService log, StatusService status) {
		this.ops = ops;
		this.log = log;
		this.status = status;
	}
	
	public void setStatusService(StatusService status) {
		this.status = status;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	/**
	 * The CLIJ2 instance on the deconvolution device, or null, after a warning
	 * in the log, when no OpenCL device can be opened.
	 */
	private CLIJ2 getCLIJ2() {
		try {
			return CLIJ2.getInstance(deviceName);
		}
		catch (Exception | LinkageError e) {
			status.warn(describeOpenCLFailure(e));
			return null;
		}
	}

	/**
	 * A log message for when CLIJ cannot open an OpenCL device. The first attempt
	 * fails with OpenCL error -1001 when no driver offers a platform; CLIJ then
	 * leaves its device list null, so later attempts fail with a NullPointerException.
	 */
	static String describeOpenCLFailure(Throwable e) {
		String why = String.valueOf(e.getMessage());
		if (why.contains("-1001") || why.contains("allDevices")) why = "no OpenCL platform found";
		return "No OpenCL device available (" + why + "). Deconvolution needs one, a GPU or a CPU; "
			+ "check that an OpenCL driver works (clinfo should list a device), then restart Fiji.";
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

	public int getXCellSize() {
	    return xCellSize;
	}

	public void setXCellSize(int xCellSize) {
	    this.xCellSize = xCellSize;
	}

	public int getYCellSize() {
	    return yCellSize;
	}

	public void setYCellSize(int yCellSize) {
	    this.yCellSize = yCellSize;
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
			
			CLIJ2 clij2 = getCLIJ2();
			if (clij2 == null) return null;
			
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
		
	
	/**
	 * Deconvolves every channel of {@code imp} (first time point only) as its own 3D
	 * volume, channel c with {@code psfs[c]}, and shows the channels merged into one image.
	 */
	public void runDeconvolution(ImagePlus imp, Img[] psfs) {

		CLIJ2 clij2 = getCLIJ2();
		if (clij2 == null) return;
		this.status.showStatus(0, 2, "Using device "+clij2.getGPUName());

		int nChannels = imp.getNChannels();
		if (imp.getNFrames() > 1) {
			this.status.warn("Only the first of " + imp.getNFrames() + " time points is deconvolved.");
		}
		if (psfs.length < nChannels) {
			this.status.warn("Got " + psfs.length + " PSFs for " + nChannels + " channels; stopping.");
			return;
		}

		// a float view of the image, no copy; ImageJFunctions drops singleton
		// dimensions, so the axes are X Y [C] Z [T]
		RandomAccessibleInterval<FloatType> all = floatView(imp);
		if (imp.getNFrames() > 1) all = Views.hyperSlice(all, all.numDimensions() - 1, 0);

		ImagePlus[] outputs = new ImagePlus[nChannels];
		for (int c = 0; c < nChannels; c++) {
			RandomAccessibleInterval<FloatType> channel = nChannels > 1 ? Views.hyperSlice(all, 2, c) : all;
			if (channel.numDimensions() != 3) {
				this.status.warn("Expected a 3D volume per channel but got " + channel.numDimensions()
					+ " dimensions; stopping.");
				return;
			}
			String label = nChannels > 1 ? "Channel " + (c + 1) + " of " + nChannels + ": " : "";
			outputs[c] = this.useCells ? deconvolveCells(channel, psfs[c], label)
				: deconvolveWhole(clij2, channel, psfs[c], label);
			// ImageJFunctions and CLIJ may label the third axis of a 3D result as channels;
			// it is z, which the channel merge below relies on
			outputs[c].setDimensions(1, outputs[c].getStackSize(), 1);
		}

		ImagePlus out;
		if (nChannels == 1) out = outputs[0];
		else {
			// merge keeps the display settings simple: one composite, one channel per input channel
			out = RGBStackMerge.mergeChannels(outputs, false);
			for (ImagePlus o : outputs) o.close();
		}
		out.setTitle("Deconvolved " + imp.getTitle());
		out.setCalibration(imp.getCalibration().copy());
		// keep the input's colors: its channel LUTs and composite mode, with the
		// contrast reset for the deconvolved values
		if (out instanceof CompositeImage) {
			CompositeImage comp = (CompositeImage) out;
			CompositeImage in = imp instanceof CompositeImage ? (CompositeImage) imp : null;
			for (int c = 1; c <= nChannels; c++) {
				if (in != null) comp.setChannelLut(in.getChannelLut(c), c);
				comp.setC(c);
				comp.resetDisplayRange();
			}
			if (in != null) comp.setMode(in.getMode());
			comp.setC(1);
		}
		else {
			if (imp.isComposite()) out.getProcessor().setLut(((CompositeImage) imp).getChannelLut(1));
			else if (imp.getProcessor() != null && imp.getProcessor().getLut() != null && !imp.getProcessor().isDefaultLut()) {
				out.getProcessor().setLut(imp.getProcessor().getLut());
			}
			out.resetDisplayRange();
		}
		out.show();

		// reset progress
		this.status.showStatus(0, 2, "");
	}

	/** A float view of the image without copying it (ImageJFunctions.wrapFloat only wraps 32-bit). */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static RandomAccessibleInterval<FloatType> floatView(ImagePlus imp) {
		if (imp.getBitDepth() == 32) return ImageJFunctions.wrapFloat(imp);
		RandomAccessibleInterval real = ImageJFunctions.wrapReal(imp);
		return Converters.convert(real, (Converter<RealType, FloatType>) (a, b) -> b.setReal(a.getRealFloat()),
			new FloatType());
	}

	/** Whole-volume mode: the volume, PSF and output all live on the GPU at once. */
	private ImagePlus deconvolveWhole(CLIJ2 clij2, RandomAccessibleInterval<FloatType> volume, Img psf, String label) {
		ClearCLBuffer gpu_psf = clij2.push(psf);
		ClearCLBuffer gpu_image = clij2.push(volume);
		ClearCLBuffer gpu_deconvolved = clij2.create(gpu_image.getDimensions(), NativeTypeEnum.Float);

		this.status.showStatus(1, 2, label + "deconvolving the volume in one piece, " + iterations + " iterations");
		
		DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpu_image, gpu_psf, gpu_deconvolved, iterations, regularizaitonFactor, true);

		ImagePlus out = clij2.pull(gpu_deconvolved);
		
		gpu_psf.close();
		gpu_image.close();
		gpu_deconvolved.close();
		return out;
	}

	/** Cell mode: the volume is deconvolved cell by cell, each cell padded by the overlap. */
	private ImagePlus deconvolveCells(RandomAccessibleInterval<FloatType> volume, Img psf, String label) {
		log.info(label + "cell deconvolution, cell size " + this.xCellSize + " x " + this.yCellSize + " x " + this.zCellSize
			+ ", volume " + volume.dimension(0) + " x " + volume.dimension(1) + " x " + volume.dimension(2));
		
		int numDivisionsX = (int)(Math.ceil((float)volume.dimension(0)/(float)this.xCellSize));
		int numDivisionsY = (int)(Math.ceil((float)volume.dimension(1)/(float)this.yCellSize));
		int numDivisionsZ = (int)(Math.ceil((float)volume.dimension(2)/(float)this.zCellSize));
		int numCells = numDivisionsX*numDivisionsY*numDivisionsZ;
		this.status.showStatus(0, numCells, label + "deconvolving " + numCells + " cells, " + iterations + " iterations");
		
		// create the version of clij2 RL that works on cells
		Clij2RichardsonLucyImglib2Cache<FloatType, ?, ?> op =
				Clij2RichardsonLucyImglib2Cache.builder().rai(volume).psf(psf).overlap(10,10,10).numberOfIterations(iterations)
				.regularizationFactor(regularizaitonFactor).build();
		
		op.setUpStatus(status, numCells);

		// the imglib2 cache 'generate' utility is used here as a convenient way to split the
		// volume into cells; every cell is computed right away (getData) instead of on view,
		// so the result is complete and responsive when shown
		CachedCellImg<FloatType, ArrayDataAccess<FloatType>> decon = (CachedCellImg) Lazy.generate(volume,
				new int[] { this.xCellSize, this.yCellSize, this.zCellSize },
				new FloatType(), AccessFlags.setOf(AccessFlags.VOLATILE), op);
		decon.getCells().forEach(Cell::getData);
		
		// copy into a regular ImageJ image so the channels can be merged and saved
		return ImageJFunctions.wrapFloat(decon, "Deconvolution").duplicate();
	}

}
