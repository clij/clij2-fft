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

/**
 * This plugin applies convolution using Fast Fourier Transform (FFT) via the clFFT library.
 * It supports both 2D and 3D images and can perform either convolution or correlation.
 */
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

	/**
	 * Executes the FFT-based convolution or correlation using the provided arguments.
	 *
	 * @return true if the operation was successful
	 */
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
	 * Converts input and kernel to float if necessary, extends them to the next supported FFT size,
	 * and performs FFT-based convolution or correlation.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param input the input image buffer
	 * @param psf the kernel (point spread function) buffer
	 * @param convolved the output buffer for the convolved image
	 * @param correlate if true, performs correlation instead of convolution
	 * @return true if the operation was successful
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

		// Extend input to the next supported FFT size
		ClearCLBuffer inputExtended = padFFTInputZeros(clij2, input, psf, ops);
		
		// create memory for extended psf and convolved
		ClearCLBuffer psf_extended = clij2.create(inputExtended);
		ClearCLBuffer convolvedExtended = clij2.create(inputExtended);

		// Extend and shift the kernel
		padShiftFFTKernel(clij2, psfFloat, psf_extended);

		// Perform convolution or correlation
		runConvolve(clij2, inputExtended, psf_extended, convolvedExtended, correlate);

		// Crop the result to the original size
		OpenCLFFTUtility.cropExtended(clij2, convolvedExtended, convolved);

		// Release temporary buffers
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
	 * Performs FFT-based convolution or correlation.
	 * The input and kernel must already be extended to the next supported FFT size.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param gpuImg the extended input image buffer
	 * @param gpuPSF the extended kernel buffer
	 * @param output the output buffer for the convolved image
	 * @param correlate if true, performs correlation instead of convolution
	 */
	public static void runConvolve(CLIJ2 clij2, ClearCLBuffer gpuImg,
		ClearCLBuffer gpuPSF, ClearCLBuffer output, boolean correlate)
	{

		// run the forward FFT for image and PSF
		ClearCLBuffer gpuFFTImg =ForwardFFT.runFFT(clij2, gpuImg);
		ClearCLBuffer gpuFFTPSF=ForwardFFT.runFFT(clij2, gpuPSF);
		
		// now create a buffer for the complex output
		ClearCLBuffer complexOutput = clij2.create(gpuFFTImg.getDimensions(), NativeTypeEnum.Float);

		// Multiply in frequency domain (convolution theorem)
		MultiplyComplexImages.multiplyComplexImages(clij2, gpuFFTImg, gpuFFTPSF, complexOutput);

		// Perform inverse FFT to get the spatial result
		InverseFFT.runInverseFFT(clij2, complexOutput, output);

		// Release temporary buffers
		complexOutput.close();
		gpuFFTImg.close();
		gpuFFTPSF.close();
	
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
	 * Provides a description of the plugin's parameters.
	 *
	 * @return a string describing the input and output parameters
	 */
	@Override
	public String getParameterHelpText() {
		return "Image input, Image convolution_kernel, ByRef Image destination, Boolean correlate";
	}

	/**
	 * Provides a description of the plugin's functionality.
	 *
	 * @return a string describing what the plugin does
	 */
	@Override
	public String getDescription() {
		return "Applies convolution using a Fast Fourier Transform using the clFFT library.";
	}

	/**
	 * Specifies the dimensions supported by this plugin.
	 *
	 * @return a string indicating the supported dimensions
	 */
	@Override
	public String getAvailableForDimensions() {
		return "2D, 3D";
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
	 * Specifies the category under which this plugin is classified.
	 *
	 * @return a string describing the plugin's category
	 */
	@Override
	public String getCategories() {
		return "Filter";
	}

	/**
	 * Provides default values for the plugin's parameters.
	 *
	 * @return an array of default parameter values
	 */
	@Override
	public Object[] getDefaultValues() {
		return new Object[] {null, null, null, 0};
	}

	/**
	 * Performs convolution or correlation using the native conv3d_32f_lp API.
	 * This method currently only works for 3D images.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param gpuImg the input image buffer
	 * @param gpuPSF the kernel buffer
	 * @param output the output buffer for the convolved image
	 * @param correlate if true, performs correlation instead of convolution
	 * @return true if the operation was successful
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
