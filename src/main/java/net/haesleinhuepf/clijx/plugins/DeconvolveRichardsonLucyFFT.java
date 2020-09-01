
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
import net.imagej.Dataset;
import net.imagej.ops.OpService;
import net.imagej.ops.Ops;
import net.imagej.ops.filter.pad.DefaultPadInputFFT;
import net.imagej.ops.filter.pad.DefaultPadShiftKernelFFT;
import net.imglib2.FinalDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;

import static net.haesleinhuepf.clijx.plugins.DeconvolveFFT.pad;
import static net.haesleinhuepf.clijx.plugins.DeconvolveFFT.runDecon;

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
		clij2.show(convolution_kernel, "convolution_kernel");

		RandomAccessibleInterval imgF = clij2.pullRAI(input);
		RandomAccessibleInterval psfF = clij2.pullRAI(convolution_kernel_float);

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
		clij2.show(Views.zeroMin(extended), "img ext");
		clij2.show(Views.zeroMin(psfExtended), "psf ext");

		// show image and PSF
		clij2.show(imgF, "img ");
		clij2.show(psfF, "psf ");

		// push extended image and psf to GPU
		ClearCLBuffer inputGPU = clij2.push(extended);
		ClearCLBuffer psfGPU = clij2.push(psfF);

		// create output
		ClearCLBuffer output = clij2.create(inputGPU);

		boolean deconvolve = true;

		// deconvolve
		deconvolveFFT(clij2, inputGPU, psfGPU, output,100);

		// crop the result from the extended result
		clij2.crop(output, destination, -extended.min(0), -extended.min(1), -extended.min(2));

		/*
		// get deconvolved as an RAI
		RandomAccessibleInterval deconv=clij2.pullRAI(output);

		// create unpadding interval
		Interval interval = Intervals.createMinMax(-extended.min(0), -extended
				.min(1), -extended.min(2), -extended.min(0) + imgF.dimension(0) -
				1, -extended.min(1) + imgF.dimension(1) - 1, -extended.min(2) +
				imgF.dimension(2) - 1);

		// create an RAI for the output... we could just use a View to unpad, but performance for slicing is slow
		RandomAccessibleInterval outputRAI = ops.create().img(imgF);

		// copy the unpadded interval back to original size
		ops.run(Ops.Copy.RAI.class, outputRAI, Views.zeroMin(Views.interval(deconv,
				interval)));
*/

		return true;
	}

	private static boolean deconvolveFFT(CLIJ2 clij2, ClearCLBuffer input,
										ClearCLBuffer convolution_kernel, ClearCLBuffer destination, int num_iterations)
	{

		ClearCLBuffer input_float = input;
		ClearCLBuffer convolution_kernel_float = convolution_kernel;

		ClearCLBuffer extendedKernel_float = pad(clij2, convolution_kernel_float);
		clij2.show(extendedKernel_float, "Extd kernel");

		System.out.println("RUN DECON");
		System.out.println(input_float);
		System.out.println(extendedKernel_float);
		System.out.println(destination);
		System.out.println(10);


		runDecon(clij2, input_float, extendedKernel_float, destination, 10);
		//System.out.println(clij2.reportMemory());
		clij2.show(destination, "Destination");

		clij2.release(extendedKernel_float);

		return true;
	}


	@Override
	public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
		ClearCLBuffer in = (ClearCLBuffer) args[0];
		return getCLIJ2().create(in.getDimensions(), NativeTypeEnum.Float);
	}

	@Override
	public String getParameterHelpText() {
		return "Image input, Image convolution_kernel, Image destination, Integer num_iterations";
	}

	@Override
	public String getDescription() {
		return "Applies Richardson-Lucy deconvolution using a Fast Fourier Transform using the clFFT library.";
	}

	@Override
	public String getAvailableForDimensions() {
		return "2D, 3D";
	}

	@Override
	public String getAuthorName() {
		return "Brian Northon, Robert Haase";
	}

}
