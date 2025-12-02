
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

/**
 * This plugin applies Richardson-Lucy deconvolution using Fast Fourier Transform (FFT)
 * via the clFFT library. It supports 3D images and includes options for regularization
 * and non-circulant boundary conditions.
 */
@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_deconvolveRichardsonLucyFFT")
public class DeconvolveRichardsonLucyFFT extends AbstractCLIJ2Plugin implements
		CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, HasAuthor, HasClassifiedInputOutput, IsCategorized {

	private static OpService ops;
	private static Context ctx;
	static {
		// this initializes the SciJava platform.
		// See https://forum.image.sc/t/compatibility-of-imagej-tensorflow-with-imagej1/41295/2
		ctx = (Context) IJ.runPlugIn("org.scijava.Context", "");
		if (ctx == null) ctx = new Context(CommandService.class, OpService.class);
		ops = ctx.getService(OpService.class);
	}

	/**
	 * Executes the Richardson-Lucy deconvolution using the provided arguments.
	 *
	 * @return true if the operation was successful
	 */
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

	/**
	 * Runs Richardson-Lucy deconvolution with default parameters.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param input the input image buffer
	 * @param psf the point spread function buffer
	 * @param deconvolved the output buffer for the deconvolved image
	 * @param num_iterations the number of iterations to perform
	 * @return true if the operation was successful
	 */
	public static boolean deconvolveRichardsonLucyFFT(CLIJ2 clij2, ClearCLBuffer input,
							ClearCLBuffer psf, ClearCLBuffer deconvolved, int num_iterations) 
	{
		return deconvolveRichardsonLucyFFT(clij2, input, psf, deconvolved, num_iterations, 0.0f, false); 
	}

	/**
	 * Runs Richardson-Lucy deconvolution with a specified regularization factor.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param input the input image buffer
	 * @param psf the point spread function buffer
	 * @param deconvolved the output buffer for the deconvolved image
	 * @param num_iterations the number of iterations to perform
	 * @param regularizationFactor the regularization factor to dampen noise
	 * @return true if the operation was successful
	 */
	public static boolean deconvolveRichardsonLucyFFT(CLIJ2 clij2, ClearCLBuffer input,
		ClearCLBuffer psf, ClearCLBuffer deconvolved, int num_iterations, float regularizationFactor) 
	{
			return deconvolveRichardsonLucyFFT(clij2, input, psf, deconvolved, num_iterations, regularizationFactor, false); 
	}
	
	/**
	 * Runs Richardson-Lucy deconvolution with full parameter control.
	 * Converts input and PSF to float if necessary, normalizes the PSF, and performs the deconvolution.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param input the input image buffer
	 * @param psf the point spread function buffer
	 * @param deconvolved the output buffer for the deconvolved image
	 * @param num_iterations the number of iterations to perform
	 * @param regularizationFactor the regularization factor to dampen noise
	 * @param nonCirculant if true, uses non-circulant boundary conditions
	 * @return true if the operation was successful
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
	 * Extends the input image and PSF to the next supported FFT size and runs Richardson-Lucy deconvolution.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param input the input image buffer
	 * @param psf the point spread function buffer
	 * @param output the output buffer for the deconvolved image
	 * @param num_iterations the number of iterations to perform
	 * @param regularizationFactor the regularization factor to dampen noise
	 * @param nonCirculant if true, uses non-circulant boundary conditions
	 * @return true if the operation was successful
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
		
		if (nonCirculant) {
			clij2.release(normalization_factor);
		}

		return true;
	}


	/**
	 * Runs the Richardson-Lucy deconvolution on the GPU.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param gpuImg the input image buffer
	 * @param gpuPSF the point spread function buffer
	 * @param output the output buffer for the deconvolved image
	 * @param gpuNormal the normalization factor buffer (can be null)
	 * @param num_iterations the number of iterations to perform
	 * @param regularizationFactor the regularization factor to dampen noise
	 * @return true if the operation was successful
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
		
		//clij2fftWrapper.cleanup(); // CAN'T USE MULTIPLE GPU IF UNCOMMENTED, see https://github.com/clij/clij2-fft/issues/37 TODO fix!
		
		return true;
	}
	
	/**
	 * Creates a normalization factor for non-circulant boundary conditions.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param paddedDimensions the dimensions of the padded image
	 * @param originalDimensions the dimensions of the original image
	 * @param psf the point spread function buffer
	 * @return the normalization factor buffer
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

		gpuvalidregion.close();
		
		return gpunormal;
	}

	/**
	 * Creates an output buffer with the same dimensions as the input.
	 *
	 * @param input the input buffer
	 * @return the output buffer
	 */
	@Override
	public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
		ClearCLBuffer in = (ClearCLBuffer) args[0];
		return getCLIJ2().create(in.getDimensions(), NativeTypeEnum.Float);
	}

	/**
	 * Provides default values for the plugin's parameters.
	 *
	 * @return an array of default parameter values
	 */
	@Override
	public Object[] getDefaultValues() {
		return new Object[] {null, null, null, 100, 0, 0};
	}

	/**
	 * Provides a description of the plugin's parameters.
	 *
	 * @return a string describing the input and output parameters
	 */
	@Override
	public String getParameterHelpText() {
		return "Image input, Image convolution_kernel, ByRef Image destination, Number num_iterations";
	}

	/**
	 * Provides a description of the plugin's functionality.
	 *
	 * @return a string describing what the plugin does
	 */
	@Override
	public String getDescription() {
		return "Applies Richardson-Lucy deconvolution using a Fast Fourier Transform using the clFFT library.  Currently 3D images only";
	}

	/**
	 * Specifies the dimensions supported by this plugin.
	 *
	 * @return a string indicating the supported dimensions
	 */
	@Override
	public String getAvailableForDimensions() {
		return "3D";
	}

	/**
	 * Provides the names of the plugin's authors.
	 *
	 * @return the authors' names
	 */
	@Override
	public String getAuthorName() {
		return "Brian Northan, Robert Haase";
	}

	/**
	 * Specifies the type of input expected by this plugin.
	 *
	 * @return a string describing the input type
	 */
	@Override
	public String getInputType() {
		return "Image";
	}

	/**
	 * Specifies the type of output produced by this plugin.
	 *
	 * @return a string describing the output type
	 */
	@Override
	public String getOutputType() {
		return "Image";
	}

	/**
	 * Specifies the categories under which this plugin is classified.
	 *
	 * @return a string describing the plugin's categories
	 */
	@Override
	public String getCategories() {
		return "Filter,Deconvolve";
	}
}
