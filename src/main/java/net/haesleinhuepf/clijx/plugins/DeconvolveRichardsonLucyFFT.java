
package net.haesleinhuepf.clijx.plugins;

import ij.IJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.utilities.HasAuthor;
import net.imagej.ops.OpService;
import net.imagej.ops.filter.pad.DefaultPadInputFFT;
import net.imagej.ops.filter.pad.DefaultPadShiftKernelFFT;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.jocl.NativePointerObject;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;

import static net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility.pad;

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_deconvolveRichardsonLucyFFT")
public class DeconvolveRichardsonLucyFFT extends AbstractCLIJ2Plugin implements
	CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, HasAuthor
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
		boolean result = deconvolveRichardsonLucyFFT(getCLIJ2(), (ClearCLBuffer) (args[0]),
			(ClearCLBuffer) (args[1]), (ClearCLBuffer) (args[2]), asInteger(args[3]));
		return result;
	}

	public static boolean deconvolveRichardsonLucyFFT(CLIJ2 clij2, ClearCLBuffer input,
													  ClearCLBuffer convolution_kernel, ClearCLBuffer destination, int num_iterations)
	{

		ClearCLBuffer input_float = input;
		if (input_float.getNativeType() != NativeTypeEnum.Float) {
			input_float = clij2.create(input.getDimensions(), NativeTypeEnum.Float);
			clij2.copy(input, input_float);
		}

		ClearCLBuffer convolution_kernel_float = convolution_kernel;
		if (convolution_kernel.getNativeType() != NativeTypeEnum.Float) {
			convolution_kernel_float = clij2.create(convolution_kernel
					.getDimensions(), NativeTypeEnum.Float);
			clij2.copy(convolution_kernel, convolution_kernel_float);
		}

		long start = System.currentTimeMillis();
		
		RandomAccessibleInterval imgF = clij2.pullRAI(input_float);
		RandomAccessibleInterval psfF = clij2.pullRAI(convolution_kernel_float);

		convolution_kernel_float.close();

		// compute extended dimensions based on image and PSF dimensions
		long[] extendedSize = new long[imgF.numDimensions()];

		for (int d = 0; d < imgF.numDimensions(); d++) {
			extendedSize[d] = imgF.dimension(d) + psfF.dimension(d);
		}

		FinalDimensions extendedDimensions = new FinalDimensions(extendedSize);

		// extend image
		RandomAccessibleInterval<FloatType> extended = (RandomAccessibleInterval) ops.run(DefaultPadInputFFT.class, imgF, extendedDimensions, false,
						new OutOfBoundsMirrorFactory(OutOfBoundsMirrorFactory.Boundary.SINGLE));

		// extend psf
		RandomAccessibleInterval<FloatType> psfExtended = (RandomAccessibleInterval) ops.run(DefaultPadShiftKernelFFT.class, psfF, extendedDimensions, false);

		// show extended image and PSF
		//clij2.show(Views.zeroMin(extended), "img ext");
		//clij2.show(Views.zeroMin(psfExtended), "psf ext");

		// show image and PSF
		//clij2.show(imgF, "img ");
		//clij2.show(psfF, "psf ");

		// push extended image and psf to GPU
		ClearCLBuffer inputGPU = clij2.push(extended);
		ClearCLBuffer psfGPU = clij2.push(psfF);
		
		long end = System.currentTimeMillis();
		
		System.out.println("Extension time "+(end-start));

		// create output
		ClearCLBuffer output = clij2.create(inputGPU);

		start= System.currentTimeMillis();
		
		// deconvolve
		deconvolveFFT(clij2, inputGPU, psfGPU, output, num_iterations);

		end = System.currentTimeMillis();
		
		System.out.println("Deconvolve time "+(end-start));

		inputGPU.close();
		psfGPU.close();

		// crop the result from the extended result
		clij2.crop(output, destination, -extended.min(0), -extended.min(1), -extended.min(2));

		output.close();

		return true;
	}



	private static boolean deconvolveFFT(CLIJ2 clij2, ClearCLBuffer input,
										ClearCLBuffer convolution_kernel, ClearCLBuffer destination, int num_iterations)
	{

		ClearCLBuffer input_float = input;
		ClearCLBuffer convolution_kernel_float = convolution_kernel;

		ClearCLBuffer extendedKernel_float = clij2.create(input_float);
		pad(clij2, convolution_kernel_float, extendedKernel_float);

		runDecon(clij2, input_float, extendedKernel_float, destination, num_iterations);
		//System.out.println(clij2.reportMemory());
		//clij2.show(destination, "Destination");

		clij2.release(extendedKernel_float);

		return true;
	}


	/**
	 * run Richardson Lucy deconvolution
	 *
	 * @param gpuImg - need to prepad to supported FFT size (see
	 *          padInputFFTAndPush)
	 * @param gpuPSF - need to prepad to supported FFT size (see
	 *          padKernelFFTAndPush)
	 * @return
	 */
	public static ClearCLBuffer runDecon(CLIJ2 clij2, ClearCLBuffer gpuImg,
										 ClearCLBuffer gpuPSF, ClearCLBuffer output, int num_iterations)
	{
		long start = System.currentTimeMillis();

		// copy the image to use as the initial value
		ClearCLBuffer gpuEstimate = output;
		clij2.copy(gpuImg, gpuEstimate);

		// Get the CL Buffers, context, queue and device as long native pointers
		long longPointerImg = ((NativePointerObject) (gpuImg.getPeerPointer()
				.getPointer())).getNativePointer();
		long longPointerPSF = ((NativePointerObject) (gpuPSF.getPeerPointer()
				.getPointer())).getNativePointer();
		long longPointerEstimate = ((NativePointerObject) (gpuEstimate
				.getPeerPointer().getPointer())).getNativePointer();
		long l_context = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
				.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
				.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();
		long l_device = ((NativePointerObject) clij2.getCLIJ().getClearCLContext()
				.getDevice().getPeerPointer().getPointer()).getNativePointer();

		// call the decon wrapper (100 iterations of RL)
		clij2fftWrapper.deconv3d_32f_lp(num_iterations, gpuImg.getDimensions()[0], gpuImg
						.getDimensions()[1], gpuImg.getDimensions()[2], longPointerImg,
				longPointerPSF, longPointerEstimate, longPointerImg, l_context, l_queue,
				l_device);

		long finish = System.currentTimeMillis();

		System.out.println("OpenCL Decon time " + (finish - start));

		return gpuEstimate;
	}

	@Override
	public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
		ClearCLBuffer in = (ClearCLBuffer) args[0];
		return getCLIJ2().create(in.getDimensions(), NativeTypeEnum.Float);
	}

	@Override
	public String getParameterHelpText() {
		return "Image input, Image convolution_kernel, ByRef Image destination, Number num_iterations";
	}

	@Override
	public String getDescription() {
		return "Applies Richardson-Lucy deconvolution using a Fast Fourier Transform using the clFFT library.";
	}

	@Override
	public String getAvailableForDimensions() {
		return "3D";
	}

	@Override
	public String getAuthorName() {
		return "Brian Northon, Robert Haase";
	}

}
