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

import org.jocl.NativePointerObject;
import org.scijava.plugin.Plugin;

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

/**
 * This plugin performs a forward Fast Fourier Transform (FFT) on a GPU buffer.
 * It supports both 2D and 3D images and outputs the FFT in a packed complex format.
 * The input image must be pre-extended to an FFT-friendly size.
 */
@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_forwardFFT")
public class ForwardFFT extends AbstractCLIJ2Plugin implements
		CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, HasAuthor, HasClassifiedInputOutput, IsCategorized {

	/**
	 * Executes the forward FFT using the provided arguments.
	 *
	 * @return true if the operation was successful
	 */
	@Override
	public boolean executeCL() {
		boolean result = runFFT(getCLIJ2(), (ClearCLBuffer) (args[0]), (ClearCLBuffer) (args[1]));
		return result;
	}

	/**
	 * Runs the forward FFT on the input buffer and returns the result in a new buffer.
	 * The output buffer will have dimensions suitable for storing the complex FFT result.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param gpuImg the input image buffer
	 * @return the output buffer containing the FFT result
	 */
	public static ClearCLBuffer runFFT(CLIJ2 clij2, ClearCLBuffer gpuImg) {
		
		long[] dimensions = getFFTDimensions(gpuImg);

		// create GPU memory for FFT
		ClearCLBuffer gpuFFT = clij2.create(dimensions, NativeTypeEnum.Float);

		boolean result = runFFT(clij2, gpuImg, gpuFFT);
		
		return gpuFFT;
	}

	
	/**
	 * Runs the forward FFT on the input buffer and writes the result to the output buffer.
	 * The input buffer must be pre-extended to an FFT-friendly size.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param gpuImg the input image buffer (must be pre-extended to an FFT-friendly size)
	 * @param gpuFFT the output buffer for the FFT result
	 * @return true if the operation was successful
	 */
	public static boolean runFFT(CLIJ2 clij2, ClearCLBuffer gpuImg, ClearCLBuffer gpuFFT) {
		
		ClearCLBuffer input_float = gpuImg;
		
		boolean input_converted=false;
		
		// currently we only support float type
		if (input_float.getNativeType() != NativeTypeEnum.Float) {
			input_float = clij2.create(gpuImg.getDimensions(), NativeTypeEnum.Float);
			clij2.copy(gpuImg, input_float);
			input_converted=true;
		}

		// get the long pointers to in, out, context and queue.
		long l_in = ((NativePointerObject) (input_float.getPeerPointer()
			.getPointer())).getNativePointer();
		long l_out = ((NativePointerObject) (gpuFFT.getPeerPointer()
			.getPointer())).getNativePointer();
		long l_context = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
			.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
			.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();

		if (gpuImg.getDimensions().length==2) {
			// call the 2D native code that runs the FFT
			clij2fftWrapper.fft2d_32f_lp((long) (gpuImg.getWidth()), gpuImg.getHeight(),
					l_in, l_out, l_context, l_queue);
		}
		else if (gpuImg.getDimensions().length==3) {
			// call the 3D native code that runs the FFT
			clij2fftWrapper.fft3d_32f_lp((long) (gpuImg.getWidth()), gpuImg.getHeight(), gpuImg.getDepth(),
					l_in, l_out, l_context, l_queue);
		}
		
		// if we had to convert the input deallocate the converted
		if (input_converted) {
			input_float.close();
		}
		
		return true;
	}

	/**
	 * Calculates the appropriate dimensions for the FFT output buffer.
	 * For real-to-complex FFTs, the width is adjusted to 2*(N/2+1) to store the complex result.
	 *
	 * @param in the input buffer
	 * @return the dimensions for the FFT output buffer
	 */
	private static long[] getFFTDimensions(ClearCLBuffer in) {
		long[] dimensions=new long[in.getDimensions().length];
		
		dimensions[0]=2*(in.getDimensions()[0]/2+1);
		
		for (int d=1;d<in.getDimensions().length;d++) {
			dimensions[d]=in.getDimensions()[d];
		}
		
		return dimensions;
	}

	/**
	 * Creates an output buffer with dimensions suitable for storing the FFT result.
	 *
	 * @param input the input buffer
	 * @return the output buffer for the FFT result
	 */
	@Override
	public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
		ClearCLBuffer in = (ClearCLBuffer) args[0];
		
		long[] dimensions = getFFTDimensions(in);
	
		return getCLIJ2().create(dimensions, NativeTypeEnum.Float);
	}

	/**
	 * Provides a description of the plugin's parameters.
	 *
	 * @return a string describing the input and output parameters
	 */
	@Override
	public String getParameterHelpText() {
		return "Image input, ByRef Image destination";
	}

	/**
	 * Provides a description of the plugin's functionality.
	 *
	 * @return a string describing what the plugin does
	 */
	@Override
	public String getDescription() {
		return "Performs forward FFT, currently only works on power of 2 or prime factorable numbers";
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
	 * Provides the name of the plugin's author.
	 *
	 * @return the author's name
	 */
	@Override
	public String getAuthorName() {
		return "Brian Northan";
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

}
