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
package net.haesleinhuepf.clijx.tests;

import java.io.IOException;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility;
import net.haesleinhuepf.clijx.plugins.clij2fftWrapper;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;

import org.jocl.NativePointerObject;

public class InteractiveFFTFilter {
	
	final static ImageJ ij = new ImageJ();
	
	public static <T extends RealType<T> & NativeType<T>> void main(
		final String[] args) throws IOException
	{
		ij.launch(args);
		
		System.out.println(System.getProperty("java.library.path"));

		// launch IJ so we can interact with the inputs and outputs
		ij.launch(args);

		CLIJ2 clij2=null;
		
		// get clij
		try {
			clij2 = CLIJ2.getInstance("RTX");
		}
		catch(Exception e) {
			System.out.println(e);
			return;
		}

		// load the dataset
		Dataset dataset = (Dataset) ij.io().open("../../images/bridge.tif");

		Img<FloatType> img = ij.op().convert().float32((Img)dataset);
		clij2.show(img, "img");

		// push to GPU
		ClearCLBuffer gpu_img = clij2.push(img);
		
		// input size
		int N0=(int)gpu_img.getDimensions()[0];
		int N1=(int)gpu_img.getDimensions()[1];
		
		// Complex FFT size of real input signal
		int M0=N0/2+1;
		int M1=N1;
		
		// buffer to put FFT in
		ClearCLBuffer FFT = clij2.create(2*M0, M1);
		// FFT after filter is applied
		ClearCLBuffer FFT2 = clij2.create(2*M0, M1);
		// filter
		ClearCLBuffer filter = clij2.create(2*M0, M1);
		// output
		ClearCLBuffer out = clij2.create(new long[] {N0,N1}, NativeTypeEnum.Float);
		
		// Get the CL Buffers, context, queue and device as long native pointers
		long longPointerImg = ((NativePointerObject) (gpu_img.getPeerPointer()
				.getPointer())).getNativePointer();
		long longPointerFFT = ((NativePointerObject) (FFT.getPeerPointer()
				.getPointer())).getNativePointer();
		long longPointerFFT2 = ((NativePointerObject) (FFT2.getPeerPointer()
				.getPointer())).getNativePointer();
		long longPointerOut = ((NativePointerObject) (out.getPeerPointer()
				.getPointer())).getNativePointer();
		long l_context = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
				.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
				.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();

		clij2fftWrapper.fft2d_32f_lp(N0, N1, longPointerImg, longPointerFFT, l_context, l_queue);
		
		clij2.show(FFT, "FFT");

		// apply a naive box filter to the image
		// consider FFT is complex with N0/2+1 coefficients in X, and N1 in Y but stored as floats
		// the complex numbers are stored in the loat array in interleaving format 
		// that is (real1, imag1, real2, imag2,... realn, imagn)
		// TODO in the future it would be nice if CLIJ supported a complex format
	  clij2.drawBox(filter, 0, 0, N0/8, N1/16);	
	  clij2.drawBox(filter, 0, 15*N1/16, N0/8, N1/16);	
	  
	  clij2.show(filter, "filter");
		clij2.multiplyImages(FFT, filter, FFT2);
		
		clij2fftWrapper.fft2dinv_32f_lp(N0, N1, longPointerFFT2, longPointerOut, l_context, l_queue);
		
		clij2.show(out, "inverse");
	
	}
}
