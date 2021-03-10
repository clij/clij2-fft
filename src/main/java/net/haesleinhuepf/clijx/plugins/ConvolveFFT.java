
package net.haesleinhuepf.clijx.plugins;

import static net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility.padShiftFFTKernel;
import static net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility.padFFTInputZeros;

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

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_convolveFFT")
public class ConvolveFFT extends AbstractCLIJ2Plugin implements
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
		
		boolean result = convolveFFT(getCLIJ2(), (ClearCLBuffer) (args[0]),
			(ClearCLBuffer) (args[1]), (ClearCLBuffer) (args[2]));
		return result;
		
	}

	/**
	 * Convert image to float if not already float, 
	 * 
	 * @param clij2
	 * @param input
	 * @param convolution_kernel
	 * @param destination
	 * @return
	 */
	public static boolean convolveFFT(CLIJ2 clij2, ClearCLBuffer input,
		ClearCLBuffer convolution_kernel, ClearCLBuffer destination)
	{

		ClearCLBuffer input_float = input;
		
		boolean input_converted = false;
		
		if (input_float.getNativeType() != NativeTypeEnum.Float) {
			input_float = clij2.create(input.getDimensions(), NativeTypeEnum.Float);
			clij2.copy(input, input_float);
			input_converted=true;
		}

		boolean psf_converted=false;
		ClearCLBuffer convolution_kernel_float = convolution_kernel;
		if (convolution_kernel.getNativeType() != NativeTypeEnum.Float) {
			convolution_kernel_float = clij2.create(convolution_kernel
				.getDimensions(), NativeTypeEnum.Float);
			clij2.copy(convolution_kernel, convolution_kernel_float);
			psf_converted=true;
		}

		// extended input
		ClearCLBuffer input_extended = padFFTInputZeros(clij2, input, convolution_kernel, ops);
		
		// create memory for extended psf and convolved
		ClearCLBuffer psf_extended = clij2.create(input_extended);
		ClearCLBuffer convolved_extended = clij2.create(input_extended);
		
		// extend kernel
		padShiftFFTKernel(clij2, convolution_kernel_float, psf_extended);
		
		runConvolve(clij2, input_extended, psf_extended, convolved_extended, false);
		
		clij2.release(psf_extended);

		if (input_float != input) {
			clij2.release(input_float);
		}

		if (convolution_kernel_float != convolution_kernel) {
			clij2.release(convolution_kernel_float);
		}
		
		OpenCLFFTUtility.cropExtended(clij2, convolved_extended, destination);
	
		clij2.release(psf_extended);
		clij2.release(input_extended);
		clij2.release(convolved_extended);
		
		if (input_converted) {
			input_float.close();
		}
		
		if (psf_converted) {
			convolution_kernel_float.close();
		}
		
		
		return true;
	}

	/**
	 * run convolution
	 * 
	 * @param gpuImg - need to prepad to supported FFT size (see
	 *          padInputFFTAndPush)
	 * @param gpuPSF - need to prepad to supported FFT size (see
	 *          padKernelFFTAndPush)
	 * @return
	 */
	public static void runConvolve(CLIJ2 clij2, ClearCLBuffer gpuImg,
		ClearCLBuffer gpuPSF, ClearCLBuffer output, boolean correlate)
	{

		// run the forward FFT for image and PSF
		ClearCLBuffer gpuFFTImg =ForwardFFT.runFFT(clij2, gpuImg);
		ClearCLBuffer gpuFFTPSF=ForwardFFT.runFFT(clij2, gpuPSF);
		
		// now create a buffer for the complex output
		ClearCLBuffer complexOutput = clij2.create(gpuFFTImg.getDimensions(), NativeTypeEnum.Float);

		// Perform convolution by mulitplying in the frequency domain (see https://en.wikipedia.org/wiki/Convolution_theorem)
		MultiplyComplexImages.multiplyComplexImages(clij2, gpuFFTImg, gpuFFTPSF, complexOutput);

		// now get convolved spatian signal by performing inverse 
		InverseFFT.runInverseFFT(clij2, complexOutput, output);
		
		complexOutput.close();
	
	}


	@Override
	public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
		ClearCLBuffer in = (ClearCLBuffer) args[0];
		return getCLIJ2().create(in.getDimensions(), NativeTypeEnum.Float);
	}

	@Override
	public String getParameterHelpText() {
		return "Image input, Image convolution_kernel, ByRef Image destination";
	}

	@Override
	public String getDescription() {
		return "Applies convolution using a Fast Fourier Transform using the clFFT library.";
	}

	@Override
	public String getAvailableForDimensions() {
		return "2D, 3D";
	}

	@Override
	public String getAuthorName() {
		return "Brian Northon, Robert Haase";
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
		return "Filter";
	}

}
