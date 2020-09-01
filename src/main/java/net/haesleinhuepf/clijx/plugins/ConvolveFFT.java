
package net.haesleinhuepf.clijx.plugins;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.utilities.HasAuthor;

import org.jocl.NativePointerObject;
import org.scijava.plugin.Plugin;

import ij.IJ;
import ij.ImagePlus;

import static net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility.pad;

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_convolveFFT")
public class ConvolveFFT extends AbstractCLIJ2Plugin implements
	CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, HasAuthor
{

	@Override
	public boolean executeCL() {
		boolean result = convolveFFT(getCLIJ2(), (ClearCLBuffer) (args[0]),
			(ClearCLBuffer) (args[1]), (ClearCLBuffer) (args[2]));
		return result;
	}

	public static boolean convolveFFT(CLIJ2 clij2, ClearCLBuffer input,
		ClearCLBuffer convolution_kernel, ClearCLBuffer destination)
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

		ClearCLBuffer extendedKernel_float = pad(clij2, convolution_kernel_float);

		runConvolve(clij2, input_float, extendedKernel_float, destination);

		clij2.release(extendedKernel_float);

		if (input_float != input) {
			clij2.release(input_float);
		}

		if (convolution_kernel_float != convolution_kernel) {
			clij2.release(convolution_kernel_float);
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
	public static ClearCLBuffer runConvolve(CLIJ2 clij2, ClearCLBuffer gpuImg,
		ClearCLBuffer gpuPSF, ClearCLBuffer output)
	{

		long start = System.currentTimeMillis();

		// create another copy of the image to use as the initial value
		ClearCLBuffer gpuEstimate = output;
		clij2.copy(gpuImg, gpuEstimate);

		// Get the CL Buffers, context, queue and device as long native pointers
		long longPointerImg = ((NativePointerObject) (gpuImg.getPeerPointer()
			.getPointer())).getNativePointer();
		long longPointerPSF = ((NativePointerObject) (gpuPSF.getPeerPointer()
			.getPointer())).getNativePointer();
		long longPointerOutput = ((NativePointerObject) (gpuEstimate
			.getPeerPointer().getPointer())).getNativePointer();
		long l_context = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
			.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
			.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();
		long l_device = ((NativePointerObject) clij2.getCLIJ().getClearCLContext()
			.getDevice().getPeerPointer().getPointer()).getNativePointer();

		// call the decon wrapper (100 iterations of RL)
		clij2fftWrapper.conv3d_32f_lp(gpuImg.getDimensions()[0], gpuImg
			.getDimensions()[1], gpuImg.getDimensions()[2], longPointerImg,
			longPointerPSF, longPointerOutput, true, l_context, l_queue, l_device);

		long finish = System.currentTimeMillis();

		System.out.println("OpenCL Convolution time " + (finish - start));

		return gpuEstimate;
	}


	@Override
	public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
		ClearCLBuffer in = (ClearCLBuffer) args[0];
		return getCLIJ2().create(in.getDimensions(), NativeTypeEnum.Float);
	}

	@Override
	public String getParameterHelpText() {
		return "Image input, Image convolution_kernel, Image destination";
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
}
