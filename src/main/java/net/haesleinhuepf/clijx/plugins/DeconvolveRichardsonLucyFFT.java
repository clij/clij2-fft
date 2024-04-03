
package net.haesleinhuepf.clijx.plugins;

import static net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility.cropExtended;
import static net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility.padFFTInputMirror;
import static net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility.padFFTInputZeros;
import static net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility.padShiftFFTKernel;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.utilities.HasAuthor;
import net.haesleinhuepf.clij2.utilities.HasClassifiedInputOutput;
import net.haesleinhuepf.clij2.utilities.IsCategorized;
import net.imagej.ops.OpService;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.loops.LoopBuilder;

import org.jocl.NativePointerObject;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;

import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

import ij.IJ;

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_deconvolveRichardsonLucyFFT")
public class DeconvolveRichardsonLucyFFT extends AbstractCLIJ2Plugin implements
	CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, HasAuthor, HasClassifiedInputOutput, IsCategorized
{

	private static OpService ops;
	private static Context ctx;
	static {
		// this initializes the SciJava platform.
		// See https://forum.image.sc/t/compatibility-of-imagej-tensorflow-with-imagej1/41295/2
		ctx = (Context) IJ.runPlugIn("org.scijava.Context", "");
		if (ctx == null) ctx = new Context(CommandService.class, OpService.class);
		ops = ctx.getService(OpService.class);
	}

	@Override
	public boolean executeCL() {
		
		float regularizationFactor = 0.0f;
		
		if (args.length>=5) {
			regularizationFactor = ((Double)(args[4])).floatValue();
		}
		
		boolean nonCirculant = false;
		
		if (args.length>=6) {
			if ((Double)(args[5])>0) {
			nonCirculant = true;
			}
		}
		
		boolean result = deconvolveRichardsonLucyFFT(getCLIJ2(), (ClearCLBuffer) (args[0]),
			(ClearCLBuffer) (args[1]), (ClearCLBuffer) (args[2]), asInteger(args[3]), regularizationFactor, nonCirculant);
		return result;
	}
 	
	public static boolean deconvolveRichardsonLucyFFT(CLIJ2 clij2, ClearCLBuffer input,
							ClearCLBuffer psf, ClearCLBuffer deconvolved, int num_iterations) 
	{
		return deconvolveRichardsonLucyFFT(clij2, input, psf, deconvolved, num_iterations, 0.0f, false); 
	}
	
	public static boolean deconvolveRichardsonLucyFFT(CLIJ2 clij2, ClearCLBuffer input,
		ClearCLBuffer psf, ClearCLBuffer deconvolved, int num_iterations, float regularizationFactor) 
	{
			return deconvolveRichardsonLucyFFT(clij2, input, psf, deconvolved, num_iterations, regularizationFactor, false); 
	}
	
	/**
	 * Convert images to float (if not already float), normalize PSF and call Richardson Lucy 
	 * 
	 * @param clij2
	 * @param input
	 * @param psf
	 * @param deconvolved
	 * @param num_iterations
	 * 
	 * @return true if successful
	 */
	public static boolean deconvolveRichardsonLucyFFT(CLIJ2 clij2, ClearCLBuffer input,
													  ClearCLBuffer psf, ClearCLBuffer deconvolved, int num_iterations, 
													  float regularizationFactor, boolean nonCirculant)
	{
		ClearCLBuffer inputFloat = input;
		
		boolean inputConverted=false;
		
		if (inputFloat.getNativeType() != NativeTypeEnum.Float) {
			inputFloat = clij2.create(input.getDimensions(), NativeTypeEnum.Float);
			clij2.copy(input, inputFloat);
			inputConverted=true;
		}

		boolean psfConverted=false;
		ClearCLBuffer psfFloat = psf;
		if (psf.getNativeType() != NativeTypeEnum.Float) {
			psfFloat = clij2.create(psf
					.getDimensions(), NativeTypeEnum.Float);
			clij2.copy(psf, psfFloat);
			psfConverted=true;
		}

		// normalize PSF so that it's sum is one 
		ClearCLBuffer psf_normalized = clij2.create(psfFloat);
		
		OpenCLFFTUtility.normalize(clij2, psfFloat, psf_normalized);
		
		long start = System.currentTimeMillis();
		
		// deconvolve
		extendAndDeconvolveRichardsonLucyFFT(clij2, inputFloat, 
			psf_normalized, deconvolved, num_iterations, regularizationFactor, nonCirculant);

		long end = System.currentTimeMillis();
		
		System.out.println("Deconvolve time "+(end-start));

		if (inputConverted) {
			inputFloat.close();
		}
		
		if (psfConverted) {
			psfFloat.close();
		}
		
		psf_normalized.close();

		return true;
	}

  /**
   * Extend image and PSF to next supported FFT size and call Richardson Lucy
   * 
   * @param clij2
   * @param input
   * @param psf
   * @param output
   * @param num_iterations
   * 
   * @return true if successful
   * 
   * TODO error handling
   */
	private static boolean extendAndDeconvolveRichardsonLucyFFT(CLIJ2 clij2, ClearCLBuffer input,
										ClearCLBuffer psf, ClearCLBuffer output, int num_iterations, float regularizationFactor, boolean nonCirculant)
	{

		ClearCLBuffer inputExtended;
		
		// if NOT non-circulant mode pad and mirror
		if (!nonCirculant) {
			inputExtended = padFFTInputMirror(clij2, input, psf, ops);
		}
		// if in non-circulant mode pad with zeros
		else {
			inputExtended = padFFTInputZeros(clij2, input, psf, ops);
		}
		
		ClearCLBuffer deconvolvedExtended = clij2.create(inputExtended);
		ClearCLBuffer psfExtended = clij2.create(inputExtended);
		
		clij2.copy(inputExtended, deconvolvedExtended);
		
		padShiftFFTKernel(clij2, psf, psfExtended);
		
		long[] extendedDims = inputExtended.getDimensions();
		long[] originalDims = input.getDimensions();
	
		ClearCLBuffer normalization_factor=null;
		
		if (nonCirculant) {
			normalization_factor = createNormalizationFactor(clij2, new FinalDimensions(extendedDims[0],extendedDims[1],extendedDims[2]),  
				new FinalDimensions(originalDims[0],originalDims[1],originalDims[2]), psfExtended);
		
			// for the non-circulant case the first guess needs to be a flat sheet
			double mean=clij2.meanOfAllPixels(inputExtended);
			clij2.set(deconvolvedExtended, mean);
		}
		
		runRichardsonLucyGPU(clij2, inputExtended, psfExtended, deconvolvedExtended, normalization_factor, num_iterations, regularizationFactor);

		cropExtended(clij2, deconvolvedExtended, output);
		
		clij2.release(psfExtended);
		clij2.release(inputExtended);
		clij2.release(deconvolvedExtended);

		return true;
	}


	/**
	 * run Richardson Lucy deconvolution
	 * 
	 * @param clij2
	 * @param gpuImg
	 * @param gpuPSF
	 * @param output
	 * @param num_iterations
	 * 
	 * @return Deconvolved CLBuffer
	 * TODO proper error handling
	 */
	public static boolean runRichardsonLucyGPU(CLIJ2 clij2, ClearCLBuffer gpuImg,
										 ClearCLBuffer gpuPSF, ClearCLBuffer output, ClearCLBuffer gpuNormal, 
										 int num_iterations, float regularizationFactor)
	{


		// Get the CL Buffers, context, queue and device as long native pointers
		long longPointerImg = ((NativePointerObject) (gpuImg.getPeerPointer()
				.getPointer())).getNativePointer();
		long longPointerPSF = ((NativePointerObject) (gpuPSF.getPeerPointer()
				.getPointer())).getNativePointer();
		long longPointerOutput = ((NativePointerObject) (output
				.getPeerPointer().getPointer())).getNativePointer();
		long longPointerNormal=0;
		
		if (gpuNormal!=null) {
		 longPointerNormal = ((NativePointerObject) (gpuNormal
				.getPeerPointer().getPointer())).getNativePointer();
		}
		
		long l_context = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
				.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
				.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();
		long l_device = ((NativePointerObject) clij2.getCLIJ().getClearCLContext()
				.getDevice().getPeerPointer().getPointer()).getNativePointer();

		// call the decon wrapper (n iterations of RL)
		clij2fftWrapper.deconv3d_32f_lp_tv(num_iterations, regularizationFactor, gpuImg.getDimensions()[0], gpuImg
						.getDimensions()[1], gpuImg.getDimensions()[2], longPointerImg,
				longPointerPSF, longPointerOutput, longPointerNormal, l_context, l_queue,
				l_device);

		return true;
	}
	
	/**
	 * Calculate non-circulant normalization factor. This is used as part of the
	 * Boundary condition handling scheme described here
	 * http://bigwww.epfl.ch/deconvolution/challenge2013/index.html?p=doc_math_rl)
	 *
	 */
	private static ClearCLBuffer createNormalizationFactor(CLIJ2 clij2, final Dimensions paddedDimensions,
		final Dimensions originalDimensions, ClearCLBuffer psf) {
		
		final long[] start = new long[paddedDimensions.numDimensions()];
		final long[] end = new long[paddedDimensions.numDimensions()];

		// calculate the start and end of the original image within the extended image 
		for (int d = 0; d < originalDimensions.numDimensions(); d++) {
			start[d] = (paddedDimensions.dimension(d) - originalDimensions
				.dimension(d)) / 2;
			end[d] = start[d] + originalDimensions.dimension(d) - 1;
		}
		
		// use the start and end to for the convolution interval
		final Interval convolutionInterval = new FinalInterval(start, end);
	
		// create an image buffer to contain the valid region (all pixels 
		// in the original region are 1, extended pixels are 0)
		final Img<FloatType> validRegion = ops.create().img(paddedDimensions,
			new FloatType());

		// set all voxels in the valid region to 1
		final RandomAccessibleInterval<FloatType> temp = Views.interval(Views
			.zeroMin(validRegion), convolutionInterval);
		LoopBuilder.setImages(temp).multiThreaded().forEachPixel(a -> a.setOne());

		// convert above to ClearBufferCl
		ClearCLBuffer gpuvalidregion = clij2.push(validRegion);
		ClearCLBuffer gpunormal = clij2.create(gpuvalidregion);	
		
		// the normalization factor is the correlation between valid region and psf 
		ConvolveFFT.runConvolve2(clij2, gpuvalidregion, psf, gpunormal, true);
		
		return gpunormal;
	}
	

	@Override
	public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
		ClearCLBuffer in = (ClearCLBuffer) args[0];
		return getCLIJ2().create(in.getDimensions(), NativeTypeEnum.Float);
	}

	@Override
	public Object[] getDefaultValues() {
		return new Object[] {null, null, null, 100, 0, 0};
	}

	@Override
	public String getParameterHelpText() {
		return "Image input, Image convolution_kernel, ByRef Image destination, Number num_iterations";
	}

	@Override
	public String getDescription() {
		return "Applies Richardson-Lucy deconvolution using a Fast Fourier Transform using the clFFT library.  Currently 3D images only";
	}

	@Override
	public String getAvailableForDimensions() {
		return "3D";
	}

	@Override
	public String getAuthorName() {
		return "Brian Northan, Robert Haase";
	}

	@Override
	public String getInputType() {
		return "Image";
	}

	@Override
	public String getOutputType() {
		return "Image";
	}

	@Override
	public String getCategories() {
		return "Filter,Deconvolve";
	}
}
