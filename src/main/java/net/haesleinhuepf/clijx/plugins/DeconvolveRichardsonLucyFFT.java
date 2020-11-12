
package net.haesleinhuepf.clijx.plugins;

import static net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility.cropExtended;
import static net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility.padFFTInputMirror;
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

import org.jocl.NativePointerObject;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;

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
		boolean result = deconvolveRichardsonLucyFFT(getCLIJ2(), (ClearCLBuffer) (args[0]),
			(ClearCLBuffer) (args[1]), (ClearCLBuffer) (args[2]), asInteger(args[3]));
		return result;
	}

	public static boolean deconvolveRichardsonLucyFFT(CLIJ2 clij2, ClearCLBuffer input,
													  ClearCLBuffer psf, ClearCLBuffer deconvolved, int num_iterations)
	{
		ClearCLBuffer input_float = input;
		
		boolean input_converted=false;
		
		if (input_float.getNativeType() != NativeTypeEnum.Float) {
			input_float = clij2.create(input.getDimensions(), NativeTypeEnum.Float);
			clij2.copy(input, input_float);
			input_converted=true;
		}

		boolean psf_converted=false;
		ClearCLBuffer psf_float = psf;
		if (psf.getNativeType() != NativeTypeEnum.Float) {
			psf_float = clij2.create(psf
					.getDimensions(), NativeTypeEnum.Float);
			clij2.copy(psf, psf_float);
			psf_converted=true;
		}

		// normalize PSF so that it's sum is one 
		ClearCLBuffer psf_normalized = clij2.create(psf_float);
		
		OpenCLFFTUtility.normalize(clij2, psf_float, psf_normalized);
		
		long start = System.currentTimeMillis();
		
		// deconvolve
		deconvolveFFT(clij2, input, psf_normalized, deconvolved, num_iterations);

		long end = System.currentTimeMillis();
		
		System.out.println("Deconvolve time "+(end-start));

		if (input_converted) {
			input_float.close();
		}
		
		if (psf_converted) {
			psf_float.close();
		}
		
		psf_normalized.close();

		return true;
	}


	private static boolean deconvolveFFT(CLIJ2 clij2, ClearCLBuffer input,
										ClearCLBuffer psf, ClearCLBuffer output, int num_iterations)
	{

		//ClearCLBuffer input_extended = padFFT(clij2, input, psf);
		ClearCLBuffer input_extended = padFFTInputMirror(clij2, input, psf, ops);
		ClearCLBuffer deconvolved_extended = clij2.create(input_extended);
		ClearCLBuffer psf_extended = clij2.create(input_extended);
		
		clij2.copy(input_extended, deconvolved_extended);
		
		padShiftFFTKernel(clij2, psf, psf_extended);
		
		runDecon(clij2, input_extended, psf_extended, deconvolved_extended, 100);

		cropExtended(clij2, deconvolved_extended, output);
		
		clij2.release(psf_extended);
		clij2.release(input_extended);
		clij2.release(deconvolved_extended);

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

		// copy the image to use as the initial value
		ClearCLBuffer gpuEstimate = output;

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
