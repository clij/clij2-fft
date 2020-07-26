
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

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_deconvolveFFT")
public class DeconvolveFFT extends AbstractCLIJ2Plugin implements
	CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, HasAuthor
{

	@Override
	public boolean executeCL() {
		Object[] args = openCLBufferArgs();
		boolean result = deconvolveFFT(getCLIJ2(), (ClearCLBuffer) (args[0]),
			(ClearCLBuffer) (args[1]), (ClearCLBuffer) (args[2]));
		releaseBuffers(args);
		return result;
	}

	public static boolean deconvolveFFT(CLIJ2 clij2, ClearCLBuffer input,
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

		ClearCLBuffer extendedKernel_float = clij2.create(input_float);
		pad(clij2, input_float, convolution_kernel_float, extendedKernel_float);
		runDecon(clij2, input_float, extendedKernel_float, destination);
		clij2.release(extendedKernel_float);

		if (input_float != input) {
			clij2.release(input_float);
		}

		if (convolution_kernel_float != convolution_kernel) {
			clij2.release(convolution_kernel_float);
		}

		return true;
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

		ClearCLBuffer extendedKernel_float = clij2.create(input_float);
		pad(clij2, input_float, convolution_kernel_float, extendedKernel_float);

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
	 * run Richardson Lucy deconvolution
	 * 
	 * @param gpuImg - need to prepad to supported FFT size (see
	 *          padInputFFTAndPush)
	 * @param gpuPSF - need to prepad to supported FFT size (see
	 *          padKernelFFTAndPush)
	 * @return
	 */
	public static ClearCLBuffer runDecon(CLIJ2 clij2, ClearCLBuffer gpuImg,
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
		long longPointerEstimate = ((NativePointerObject) (gpuEstimate
			.getPeerPointer().getPointer())).getNativePointer();
		long l_context = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
			.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
			.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();
		long l_device = ((NativePointerObject) clij2.getCLIJ().getClearCLContext()
			.getDevice().getPeerPointer().getPointer()).getNativePointer();

		// call the decon wrapper (100 iterations of RL)
		clij2fftWrapper.deconv3d_32f_lp(100, gpuImg.getDimensions()[0], gpuImg
			.getDimensions()[1], gpuImg.getDimensions()[2], longPointerImg,
			longPointerPSF, longPointerEstimate, longPointerImg, l_context, l_queue,
			l_device);

		long finish = System.currentTimeMillis();

		System.out.println("OpenCL Decon time " + (finish - start));

		return gpuEstimate;
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

		System.out.println("OpenCL Decon time " + (finish - start));

		return gpuEstimate;
	}

	public static void pad(CLIJ2 clij2, ClearCLBuffer input,
		ClearCLBuffer convolution_kernel, ClearCLBuffer extendedKernel)
	{
		long psfHalfWidth = convolution_kernel.getWidth() / 2;
		long psfHalfHeight = convolution_kernel.getHeight() / 2;
		long psfHalfDepth = convolution_kernel.getDepth() / 2;

		clij2.set(extendedKernel, 0);

		ClearCLBuffer temp = clij2.create(psfHalfWidth, psfHalfHeight,
			psfHalfDepth);

		moveCorner(clij2, convolution_kernel, temp, extendedKernel, 0, 0, 0);
		moveCorner(clij2, convolution_kernel, temp, extendedKernel, 0, 0, 1);
		moveCorner(clij2, convolution_kernel, temp, extendedKernel, 0, 1, 0);
		moveCorner(clij2, convolution_kernel, temp, extendedKernel, 0, 1, 1);
		moveCorner(clij2, convolution_kernel, temp, extendedKernel, 1, 0, 0);
		moveCorner(clij2, convolution_kernel, temp, extendedKernel, 1, 0, 1);
		moveCorner(clij2, convolution_kernel, temp, extendedKernel, 1, 1, 0);
		moveCorner(clij2, convolution_kernel, temp, extendedKernel, 1, 1, 1);

		clij2.release(temp);
	}

	private static void moveCorner(CLIJ2 clij2, ClearCLBuffer convolution_kernel,
		ClearCLBuffer temp, ClearCLBuffer extendedKernel, int factorX, int factorY,
		int factorZ)
	{
		clij2.crop(convolution_kernel, temp, temp.getWidth() * factorX, temp
			.getHeight() * factorY, temp.getDepth() * factorZ);
		clij2.paste(temp, extendedKernel, (extendedKernel.getWidth() - temp
			.getWidth()) * (1.0 - factorX), (extendedKernel.getHeight() - temp
				.getHeight()) * (1.0 - factorY), (extendedKernel.getDepth() - temp
					.getDepth()) * (1.0 - factorZ));
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

	public static void main(String[] args) {

		ImagePlus input = IJ.openImage(
			"C:/Users/rober/Downloads/images/Bars-G10-P15-stack-cropped.tif");
		ImagePlus psf = IJ.openImage(
			"C:/Users/rober/Downloads/images/PSF-Bars-stack-cropped-64.tif");

		IJ.run(input, "32-bit", "");
		IJ.run(psf, "32-bit", "");

		CLIJ2 clij2 = CLIJ2.getInstance("RTX");

		ClearCLBuffer inputGPU = clij2.push(input);
		ClearCLBuffer psfGPU = clij2.push(psf);

		ClearCLBuffer output = clij2.create(inputGPU);

		DeconvolveFFT.deconvolveFFT(clij2, inputGPU, psfGPU, output);

		clij2.show(output, "output");

	}
}
