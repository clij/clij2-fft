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

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.imagej.ops.OpService;
import net.imagej.ops.filter.pad.DefaultPadInputFFT;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Utility class for FFT-related operations on GPU images using CLij2.
 * Provides methods for padding, cropping, and normalizing images and kernels for FFT-based convolution and deconvolution.
 */
public class OpenCLFFTUtility {

	/**
	 * Copies a real-valued image into a complex-valued image.
	 * The output image has half the width of the input, as required for real-to-complex FFTs.
	 *
	 * @param in the input real-valued image
	 * @return a complex-valued image with half the width of the input
	 */
	static Img<ComplexFloatType> copyAsComplex(RandomAccessibleInterval<FloatType> in) {
		float[] temp = new float[(int) (in.dimension(0) * in.dimension(1))];
		int i = 0;
		for (FloatType f : Views.iterable(in)) {
			temp[i++] = f.getRealFloat();
		}

		return ArrayImgs.complexFloats(temp, new long[] { in.dimension(0) / 2, in
			.dimension(1) });
	}
	
	
	/**
	 * Pads a GPU image to the next supported FFT size using a mirror out-of-bounds strategy.
	 * The image is pulled onto the CPU, extended, and then pushed back to the GPU.
	 *
	 * TODO fully implement this on the GPU
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param input the input image to pad
	 * @param psf the point spread function (PSF) used to determine padding size
	 * @param ops the ImageJ OpService for image operations
	 * @return the padded image as a ClearCLBuffer
	 */
	public static ClearCLBuffer padFFTInputMirror(CLIJ2 clij2, ClearCLBuffer input, ClearCLBuffer psf, OpService ops) {
		
		RandomAccessibleInterval img = clij2.pullRAI(input);

		// compute extended dimensions based on image and PSF dimensions
		long[] extendedSize = new long[img.numDimensions()];

		for (int d = 0; d < img.numDimensions(); d++) {
			extendedSize[d] = img.dimension(d) + psf.getDimensions()[d];
		}

		FinalDimensions extendedDimensions = new FinalDimensions(extendedSize);

		// extend image
		RandomAccessibleInterval extended = (RandomAccessibleInterval) ops.run(DefaultPadInputFFT.class, img, extendedDimensions, false,
						new OutOfBoundsMirrorFactory(OutOfBoundsMirrorFactory.Boundary.SINGLE));
	
		// push extended image and psf to GPU
		return  clij2.push(extended);
		
	}
	
	/**
	 * Pads a GPU image to the next supported FFT size using a zero out-of-bounds strategy.
	 * The image is pulled onto the CPU, extended, and then pushed back to the GPU.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param input the input image to pad
	 * @param psf the point spread function (PSF) used to determine padding size
	 * @param ops the ImageJ OpService for image operations
	 * @return the padded image as a ClearCLBuffer
	 */
	public static ClearCLBuffer padFFTInputZeros(CLIJ2 clij2, ClearCLBuffer input, ClearCLBuffer psf, OpService ops) {
		ClearCLBuffer extended = null;
	
		// the below code isn't very DRY, we implemeent the same logic (compute extended size, compute
		// next fast FFT size) for both 2D and 3D case)...  TODO: Refactor
		if (input.getDimensions().length==3) {
			Dimensions dimsExtended = new FinalDimensions(input.getDimensions()[0]+psf.getDimensions()[0],
					input.getDimensions()[1]+psf.getDimensions()[1],
					input.getDimensions()[2]+psf.getDimensions()[2]);
				
			// we can find the supported FFT size using ops.  So could re-use or re-implement this in CLIJ
			long[][] nextFastFFTDimensions=ops.filter().fftSize(dimsExtended, false);
		
			long[] extendedSize = new long[input.getDimensions().length];
			long[] start= new long[input.getDimensions().length];
			
			extendedSize[0]=nextFastFFTDimensions[0][0];
			extendedSize[1]=nextFastFFTDimensions[0][1];
			extendedSize[2]=nextFastFFTDimensions[0][2];
			
			// define the starting coordinates that will be used to paste the input within
			// the larger extended space. 
			// Note: if (extendedSize[n]-input.getDimension(n)) is odd the border size on each end will 
			// be different.  Need to make sure we are consistent with respect to which side gets the smaller
			// padding
			start[0]=(long)(Math.floor((extendedSize[0]-input.getDimensions()[0])/2));
			start[1]=(long)(Math.floor((extendedSize[1]-input.getDimensions()[1])/2));
			start[2]=(long)(Math.floor((extendedSize[2]-input.getDimensions()[2])/2));
			
			extended = clij2.create(extendedSize, NativeTypeEnum.Float);
			// create() does not initialize the memory, so set the padding to zero explicitly
			clij2.set(extended, 0);
			clij2.paste(input, extended, start[0], start[1], start[2]);
		}
		else {
			Dimensions dimsExtended = new FinalDimensions(input.getDimensions()[0]+psf.getDimensions()[0],
						input.getDimensions()[1]+psf.getDimensions()[1]);
		
			// we can find the supported FFT size using ops.  So could re-use or re-implement this in CLIJ
			long[][] nextFastFFTDimensions=ops.filter().fftSize(dimsExtended, false);
			
			long[] extendedSize = new long[input.getDimensions().length];
			
			extendedSize[0]=nextFastFFTDimensions[0][0];
			extendedSize[1]=nextFastFFTDimensions[0][1];

			long[] start= new long[input.getDimensions().length];

			start[0]=(long)(Math.floor((extendedSize[0]-input.getDimensions()[0])/2));
			start[1]=(long)(Math.floor((extendedSize[1]-input.getDimensions()[1])/2));
			
			extended = clij2.create(extendedSize, NativeTypeEnum.Float);
			// create() does not initialize the memory, so set the padding to zero explicitly
			clij2.set(extended, 0);
			clij2.paste(input, extended, start[0], start[1]);
		}
		
		return extended;
		
	}

	/**
	 * Crops an extended image back to its original size.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param extended the extended image to crop
	 * @param cropped the target buffer for the cropped image (must be the same size as the original image)
	 */
	public static void cropExtended(CLIJ2 clij2, ClearCLBuffer extended, ClearCLBuffer cropped)
	{
		
		if (extended.getDimensions().length==3) {
		
			long startX = (extended.getDimensions()[0]-(int)cropped.getDimensions()[0])/2;
			long startY = (extended.getDimensions()[1]-(int)cropped.getDimensions()[1])/2;
			long startZ = (extended.getDimensions()[2]-(int)cropped.getDimensions()[2])/2;
			
			clij2.crop(extended, cropped, startX, startY, startZ);	
		} else if (extended.getDimensions().length==2) {
		
				long startX = (extended.getDimensions()[0]-(int)cropped.getDimensions()[0])/2;
				long startY = (extended.getDimensions()[1]-(int)cropped.getDimensions()[1])/2;
		
				clij2.crop(extended, cropped, startX, startY);	
		}
	}
	
	/**
	 * Pads a kernel (PSF) and shifts its origin to the center.
	 * This is a preprocessing step for FFT-based convolution and deconvolution.
	 *
	 * The center of the kernel is the pixel at index size/2 in each dimension. It is moved
	 * to index 0 of the extended kernel: the part of the kernel from the center on is
	 * pasted at the start of the extended kernel, the part before the center is wrapped
	 * around to its end. For an odd size the part from the center on is one pixel larger
	 * than the part before it, so each corner is cropped with its own size.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param convolution_kernel the input kernel (PSF)
	 * @param extendedKernel the output buffer for the extended and shifted kernel
	 * @return the extended and shifted kernel as a ClearCLBuffer
	 */
	public static ClearCLBuffer padShiftFFTKernel(CLIJ2 clij2, ClearCLBuffer convolution_kernel,
									ClearCLBuffer extendedKernel)
	{
		final int numDimensions = convolution_kernel.getDimensions().length;

		if (numDimensions!=2 && numDimensions!=3) {
			return null;
		}

		final long[] kernelSize = convolution_kernel.getDimensions();
		final long[] extendedSize = extendedKernel.getDimensions();

		clij2.set(extendedKernel, 0);

		// each bit of 'corner' tells, for one dimension, whether this corner is the part
		// of the kernel from the center on (1) or the part before the center (0)
		for (int corner = 0; corner < (1 << numDimensions); corner++) {
			long[] cornerSize = new long[numDimensions];
			long[] cropStart = new long[numDimensions];
			long[] pasteStart = new long[numDimensions];
			boolean empty = false;

			for (int d = 0; d < numDimensions; d++) {
				long half = kernelSize[d] / 2;
				if (((corner >> d) & 1) == 1) {
					cornerSize[d] = kernelSize[d] - half;
					cropStart[d] = half;
					pasteStart[d] = 0;
				} else {
					cornerSize[d] = half;
					cropStart[d] = 0;
					pasteStart[d] = extendedSize[d] - half;
				}
				// a size 1 dimension has nothing before its center
				if (cornerSize[d] == 0) {
					empty = true;
				}
			}

			if (empty) {
				continue;
			}

			ClearCLBuffer temp = clij2.create(cornerSize, convolution_kernel.getNativeType());

			if (numDimensions==3) {
				clij2.crop(convolution_kernel, temp, cropStart[0], cropStart[1], cropStart[2]);
				clij2.paste(temp, extendedKernel, pasteStart[0], pasteStart[1], pasteStart[2]);
			} else {
				clij2.crop(convolution_kernel, temp, cropStart[0], cropStart[1]);
				clij2.paste(temp, extendedKernel, pasteStart[0], pasteStart[1]);
			}

			clij2.release(temp);
		}

		return extendedKernel;
	}

	/**
	 * Normalizes a PSF so that the sum of all voxels is 1.0.
	 *
	 * @param clij2 the CLij2 instance for GPU operations
	 * @param input the input PSF
	 * @param destination the output buffer for the normalized PSF
	 * @return true if normalization was successful
	 */
	public static boolean normalize(CLIJ2 clij2, ClearCLBuffer input, ClearCLBuffer destination)
	{
		double sum = clij2.getSumOfAllPixels(input);

		clij2.multiplyImageAndScalar(input, destination, 1/sum);

		return true;
	}



}
