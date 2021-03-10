
package net.haesleinhuepf.clijx.plugins;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.imagej.ops.OpService;
import net.imagej.ops.filter.pad.DefaultPadInputFFT;
import net.imglib2.FinalDimensions;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.outofbounds.OutOfBoundsConstantValueFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class OpenCLFFTUtility {

	static Img<ComplexFloatType> copyAsComplex(
		RandomAccessibleInterval<FloatType> in)
	{
		float[] temp = new float[(int) (in.dimension(0) * in.dimension(1))];
		int i = 0;
		for (FloatType f : Views.iterable(in)) {
			temp[i++] = f.getRealFloat();
		}

		return ArrayImgs.complexFloats(temp, new long[] { in.dimension(0) / 2, in
			.dimension(1) });
	}
	
	
	/**
	 * Pad a GPU image to the next supported FFT size using mirror out of bounds strategy.  
	 * Note.  This function is not fully implemented on the GPU, thus the image is pulled 
	 * onto the CPU, extended, then pushed back to GPU
	 *
	 * TODO fully implement this on the GPU
	 * 
	 * @param clij2
	 * @param input
	 * @param psf
	 * @param ops
	 * @return
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
	 * Pad a GPU image to the next supported FFT size using zeros out of bounds strategy.  
	 * Note.  This function is not fully implemented on the GPU, thus the image is pulled 
	 * onto the CPU, extended, then pushed back to GPU
	 *
	 * TODO fully implement this on the GPU
	 * 
	 * @param clij2
	 * @param input
	 * @param psf
	 * @param ops
	 * @return
	 */
	public static ClearCLBuffer padFFTInputZeros(CLIJ2 clij2, ClearCLBuffer input, ClearCLBuffer psf, OpService ops) {
		
		RandomAccessibleInterval img = clij2.pullRAI(input);

		// compute extended dimensions based on image and PSF dimensions
		long[] extendedSize = new long[img.numDimensions()];

		for (int d = 0; d < img.numDimensions(); d++) {
			extendedSize[d] = img.dimension(d) + psf.getDimensions()[d];
		}

		FinalDimensions extendedDimensions = new FinalDimensions(extendedSize);

		// extend image
		RandomAccessibleInterval extended = (RandomAccessibleInterval) ops.run(DefaultPadInputFFT.class, img, extendedDimensions, false,
			new OutOfBoundsConstantValueFactory<>(new FloatType(0)));
	
		// push extended image and psf to GPU
		return  clij2.push(extended);
		
	}

	/**
	 * crop an image that has been extended for FFT back to original size
	 * 
	 * @param clij2
	 * @param extended - the extended image
	 * @param cropped - cropped, should be same size as the original image the extended image will be cropped to this size
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
	 * pad a kernel (PSF) and move origin to center.  This needs to be done as a pre-processing step before 
	 * calling FFT based convolution and/or deconvolution
	 * 
	 * @param clij2
	 * @param convolution_kernel - the psf
	 * @param extendedKernel - the extended and shifted psf will be written here
	 * @return
	 */
	public static ClearCLBuffer padShiftFFTKernel(CLIJ2 clij2, ClearCLBuffer convolution_kernel,
									ClearCLBuffer extendedKernel)
	{
		
		if (convolution_kernel.getDimensions().length==3) {
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
	
			return extendedKernel;
		} else if (convolution_kernel.getDimensions().length==2) {
			long psfHalfWidth = convolution_kernel.getWidth() / 2;
			long psfHalfHeight = convolution_kernel.getHeight() / 2;
			
			clij2.set(extendedKernel, 0);
			ClearCLBuffer temp = clij2.create(psfHalfWidth, psfHalfHeight);
	
			moveCorner2D(clij2, convolution_kernel, temp, extendedKernel, 0, 0);
			moveCorner2D(clij2, convolution_kernel, temp, extendedKernel, 0, 1);
			moveCorner2D(clij2, convolution_kernel, temp, extendedKernel, 1, 0);
			moveCorner2D(clij2, convolution_kernel, temp, extendedKernel, 1, 1);
	
			clij2.release(temp);
	
			return extendedKernel;
		}
		
		return null;
	}

	/**
	 * normalize a PSF so the sum of all voxels is 1.0
	 * 
	 * @param clij2
	 * @param input
	 * @param destination
	 * @return
	 */
	public static boolean normalize(CLIJ2 clij2, ClearCLBuffer input, ClearCLBuffer destination)
	{
		double sum = clij2.getSumOfAllPixels(input);

		clij2.multiplyImageAndScalar(input, destination, 1/sum);

		return true;
	}


	/**
	 * Moves a quadrant of an image stack in a corner by mirroring it
	 */
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

	/**
	 * Moves a quadrant of an image stack in a corner by mirroring it
	 */
	private static void moveCorner2D(CLIJ2 clij2, ClearCLBuffer convolution_kernel,
								   ClearCLBuffer temp, ClearCLBuffer extendedKernel, int factorX, int factorY)
	{
		clij2.crop(convolution_kernel, temp, temp.getWidth() * factorX, temp
				.getHeight() * factorY);
		clij2.paste(temp, extendedKernel, (extendedKernel.getWidth() - temp
				.getWidth()) * (1.0 - factorX), (extendedKernel.getHeight() - temp
				.getHeight()) * (1.0 - factorY));
	}


}
