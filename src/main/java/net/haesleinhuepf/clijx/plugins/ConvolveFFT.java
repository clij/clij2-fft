
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
		
		boolean correlate=false;
		
		if ((Double)args[3]>0) {
			correlate=true;
		}
		
		boolean result = convolveFFT(getCLIJ2(), (ClearCLBuffer) (args[0]),
			(ClearCLBuffer) (args[1]), (ClearCLBuffer) (args[2]), correlate);
		
		return result;
		
	}

	/**
	 * Convert image to float if not already float, extend, then convolve 
	 * 
	 * @param clij2
	 * @param input
	 * @param psf
	 * @param convolved
	 * @return
	 */
	public static boolean convolveFFT(CLIJ2 clij2, ClearCLBuffer input,
		ClearCLBuffer psf, ClearCLBuffer convolved, boolean correlate)
	{

		ClearCLBuffer inputFloat = input;
		
		boolean inputConverted = false;
		
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

		// extended input
		ClearCLBuffer inputExtended = padFFTInputZeros(clij2, input, psf, ops);
		
		// create memory for extended psf and convolved
		ClearCLBuffer psf_extended = clij2.create(inputExtended);
		ClearCLBuffer convolvedExtended = clij2.create(inputExtended);
		
		// extend kernel
		padShiftFFTKernel(clij2, psfFloat, psf_extended);
		
		runConvolve(clij2, inputExtended, psf_extended, convolvedExtended, correlate);
	
		OpenCLFFTUtility.cropExtended(clij2, convolvedExtended, convolved);
	
		clij2.release(psf_extended);
		clij2.release(inputExtended);
		clij2.release(convolvedExtended);
		
		if (inputConverted) {
			inputFloat.close();
		}
		
		if (psfConverted) {
			psfFloat.close();
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
		gpuFFTImg.close();
		gpuFFTPSF.close();
	
	}


	@Override
	public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
		ClearCLBuffer in = (ClearCLBuffer) args[0];
		return getCLIJ2().create(in.getDimensions(), NativeTypeEnum.Float);
	}

	@Override
	public String getParameterHelpText() {
		return "Image input, Image convolution_kernel, ByRef Image destination, Boolean correlate";
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
	
	@Override
	public Object[] getDefaultValues() {
		return new Object[] {null, null, null, 0};
	}

	/**
	 * (WIP)
	 * run convolution using the conv3d_32f_lp API.  This only works for 3D because we don't have a conv2d_32f_lp.
	 * 
	 * @param clij2
	 * @param gpuImg
	 * @param gpuPSF
	 * @param output
	 * @param correlate
	 * @return
	 */
	public static boolean runConvolve2(CLIJ2 clij2, ClearCLBuffer gpuImg,
										 ClearCLBuffer gpuPSF, ClearCLBuffer output, boolean correlate)
	{

		// Get the CL Buffers, context, queue and device as long native pointers
		long longPointerImg = ((NativePointerObject) (gpuImg.getPeerPointer()
				.getPointer())).getNativePointer();
		long longPointerPSF = ((NativePointerObject) (gpuPSF.getPeerPointer()
				.getPointer())).getNativePointer();
		long longPointerOutput = ((NativePointerObject) (output
				.getPeerPointer().getPointer())).getNativePointer();
	
		long l_context = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
				.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
				.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();
		long l_device = ((NativePointerObject) clij2.getCLIJ().getClearCLContext()
				.getDevice().getPeerPointer().getPointer()).getNativePointer();

		// call the decon wrapper (n iterations of RL)
		clij2fftWrapper.conv3d_32f_lp(gpuImg.getDimensions()[0], gpuImg
						.getDimensions()[1], gpuImg.getDimensions()[2], longPointerImg,
				longPointerPSF, longPointerOutput, correlate, l_context, l_queue,
				l_device);
		
		// clij2fftWrapper.cleanup();

		return true;
	}
	

}
