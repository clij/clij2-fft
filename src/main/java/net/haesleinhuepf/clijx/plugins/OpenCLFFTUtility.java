
package net.haesleinhuepf.clijx.plugins;

import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.imagej.ops.OpService;
import net.imagej.ops.filter.pad.DefaultPadInputFFT;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory;
import net.imglib2.outofbounds.OutOfBoundsMirrorFactory.Boundary;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.jocl.NativePointerObject;

public class OpenCLFFTUtility {

	/**
	 * run CLIJ FFT on an RAI and return output as RAI (mostly used for testing
	 * purposes)
	 * 
	 * @param img
	 * @param reflectAndCenter - if true reflect and center for visualization
	 * @param ops
	 * @return
	 */
	public static RandomAccessibleInterval<ComplexFloatType> runFFT(
		RandomAccessibleInterval<FloatType> img, boolean reflectAndCenter,
		OpService ops)
	{
		// extend the image to a smooth number as clFFT does not support
		// all FFT sizes
		img = (RandomAccessibleInterval<FloatType>) ops.run(
			DefaultPadInputFFT.class, img, img, false);

		// get CLIJ and push to GPU
		CLIJ clij = CLIJ.getInstance();
		ClearCLBuffer gpuInput = clij.push(img);

		// run FFT
		ClearCLBuffer fft = runFFT(gpuInput);

		// pull result from GPU
		RandomAccessibleInterval<FloatType> result =
			(RandomAccessibleInterval<FloatType>) clij.pullRAI(fft);

		// convert to Complex
		// TODO: do this without a copy (CLIJ needs complex types?)
		RandomAccessibleInterval<ComplexFloatType> resultComplex = copyAsComplex(
			result);

		if (reflectAndCenter) {
			// compute the interval of a full sized centered FFT
			Interval interval = Intervals.createMinMax(-img.dimension(0) / 2, -img
				.dimension(1) / 2, img.dimension(0) / 2, img.dimension(1) / 2);

			// reflect and center
			resultComplex = (RandomAccessibleInterval<ComplexFloatType>) Views
				.interval(Views.extend(resultComplex,
					new OutOfBoundsMirrorFactory<ComplexFloatType, RandomAccessibleInterval<ComplexFloatType>>(
						Boundary.SINGLE)), interval);
		}

		return resultComplex;
	}

	/**
	 * @param in - RAI to pad
	 * @param paddedDimensions - dimensions to pad to (dimensions will be at least
	 *          this big, but may be bigger if the next supported FFT size is
	 *          bigger)
	 * @param ops
	 * @param clij
	 * @return
	 */
	public static ClearCLBuffer padInputFFTAndPush(
		RandomAccessibleInterval<FloatType> in, Dimensions paddedDimensions,
		OpService ops, CLIJ clij)
	{
		System.out.println(in.dimension(0) + " " + in.dimension(1) + " " + in
			.dimension(2));

		RandomAccessibleInterval<FloatType> extended =
			(RandomAccessibleInterval<FloatType>) ops.run(DefaultPadInputFFT.class,
				in, paddedDimensions, false);

		System.out.println(extended.dimension(0) + " " + extended.dimension(1) +
			" " + extended.dimension(2));

		return clij.push(extended);

	}

	/**
	 * @param psf
	 * @param paddedDimensions - dimensions to pad to (dimensions will be at least
	 *          this big, but may be bigger if the next supported FFT size is
	 *          bigger)
	 * @param ops
	 * @param clij
	 * @return
	 */
	public static ClearCLBuffer padKernelFFTAndPush(
		RandomAccessibleInterval<FloatType> psf, Dimensions paddedDimensions,
		OpService ops, CLIJ clij)
	{

		// extend and shift the PSF
		RandomAccessibleInterval<FloatType> extendedPSF = Views.zeroMin(ops.filter()
			.padShiftFFTKernel(psf, paddedDimensions));

		System.out.println("Extended PSF " + extendedPSF.dimension(0) + " " +
			extendedPSF.dimension(1) + " " + extendedPSF.dimension(2));

		long start = System.currentTimeMillis();

		// transfer PSF to the GPU and return
		return clij.push(extendedPSF);

	}

	/**
	 * Run FFT on a CLBuffer
	 * 
	 * @param gpuImg input CLBuffer (needs to be pre-extended to an FFT friendly
	 *          size this can be done by using the padInputAndPush function)
	 * @return - output FFT as CLBuffer
	 */
	public static ClearCLBuffer runFFT(ClearCLBuffer gpuImg) {
		CLIJ clij = CLIJ.getInstance();

		// compute complex FFT dimension assuming Hermitian interleaved
		long[] fftDim = new long[] { (gpuImg.getWidth() / 2 + 1) * 2, gpuImg
			.getHeight() };

		// create GPU memory for FFT
		ClearCLBuffer gpuFFT = clij.create(fftDim, NativeTypeEnum.Float);

		// use a hack to get the long pointers to in, out, context and queue.
		long l_in = ((NativePointerObject) (gpuImg.getPeerPointer()
			.getPointer())).getNativePointer();
		long l_out = ((NativePointerObject) (gpuFFT.getPeerPointer()
			.getPointer())).getNativePointer();
		long l_context = ((NativePointerObject) (clij.getClearCLContext()
			.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij.getClearCLContext()
			.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();

		// call the native code that runs the FFT
		clij2fftWrapper.fft2d_long((long) (gpuImg.getWidth()), gpuImg.getHeight(),
			l_in, l_out, l_context, l_queue);

		return gpuFFT;
	}

	/**
	 * run Richardson Lucy deconvolution 
	 * 
	 * @param gpuImg - need to prepad to supported FFT size (see padInputFFTAndPush)
	 * @param gpuPSF - need to prepad to supported FFT size (see padKernelFFTAndPush)
	 * @return
	 */
	public static ClearCLBuffer runDecon(CLIJ clij, ClearCLBuffer gpuImg,
		ClearCLBuffer gpuPSF, ClearCLBuffer output)
	{

		long start = System.currentTimeMillis();

		// create another copy of the image to use as the initial value
		ClearCLBuffer gpuEstimate = output;
		clij.op().copy(gpuImg, gpuEstimate);

		// Get the CL Buffers, context, queue and device as long native pointers
		long longPointerImg = ((NativePointerObject) (gpuImg
			.getPeerPointer().getPointer())).getNativePointer();
		long longPointerPSF = ((NativePointerObject) (gpuPSF
			.getPeerPointer().getPointer())).getNativePointer();
		long longPointerEstimate = ((NativePointerObject) (gpuEstimate
			.getPeerPointer().getPointer())).getNativePointer();
		long l_context = ((NativePointerObject) (clij.getClearCLContext()
			.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij.getClearCLContext()
			.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();
		long l_device = ((NativePointerObject) clij.getClearCLContext()
			.getDevice().getPeerPointer().getPointer()).getNativePointer();

		// call the decon wrapper (100 iterations of RL)
		clij2fftWrapper.deconv_long(100, gpuImg.getDimensions()[0], gpuImg
			.getDimensions()[1], gpuImg.getDimensions()[2], longPointerImg,
			longPointerPSF, longPointerEstimate, longPointerImg, l_context, l_queue,
			l_device);

		long finish = System.currentTimeMillis();

		System.out.println("OpenCL Decon time " + (finish - start));

		return gpuEstimate;
	}

	/**
	 * Run Richardson Lucy Deconvolution
	 * @param gpuImg
	 * @param psf
	 * @param ops
	 * @return
	 */
	public static ClearCLBuffer runDecon(CLIJ clij, ClearCLBuffer gpuImg,
		RandomAccessibleInterval<FloatType> psf, OpService ops)
	{

		// extend and shift the PSF
		RandomAccessibleInterval<FloatType> extendedPSF = Views.zeroMin(ops.filter()
			.padShiftFFTKernel(psf, new FinalDimensions(gpuImg.getDimensions())));

		System.out.println("Extended PSF " + extendedPSF.dimension(0) + " " +
			extendedPSF.dimension(1) + " " + extendedPSF.dimension(2));

		long start = System.currentTimeMillis();

		// transfer PSF to the GPU
		ClearCLBuffer gpuPSF = clij.push(extendedPSF);

		// create another copy of the image to use as the initial value
		ClearCLBuffer gpuEstimate = clij.create(gpuImg);
		clij.op().copy(gpuImg, gpuEstimate);

		// Use a hack to get long pointers to the CL Buffers, context, queue and
		// device
		// (TODO: Use a more sensible approach once Robert H's pull request is
		// released)
		long longPointerImg = ((NativePointerObject) (gpuImg
			.getPeerPointer().getPointer())).getNativePointer();
		long longPointerPSF = ((NativePointerObject) (gpuPSF
			.getPeerPointer().getPointer())).getNativePointer();
		long longPointerEstimate = ((NativePointerObject) (gpuEstimate
			.getPeerPointer().getPointer())).getNativePointer();
		long l_context = ((NativePointerObject) (clij.getClearCLContext()
			.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij.getClearCLContext()
			.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();
		long l_device = ((NativePointerObject) clij.getClearCLContext()
			.getDevice().getPeerPointer().getPointer()).getNativePointer();

		// call the decon wrapper (100 iterations of RL)
		clij2fftWrapper.deconv_long(100, gpuImg.getDimensions()[0], gpuImg
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
	 * @param gpuImg - need to prepad to supported FFT size (see padInputFFTAndPush)
	 * @param gpuPSF - need to prepad to supported FFT size (see padKernelFFTAndPush)
	 * @return
	 */
	public static ClearCLBuffer runConvolve(CLIJ clij, ClearCLBuffer gpuImg,
		ClearCLBuffer gpuPSF, ClearCLBuffer output)
	{

		long start = System.currentTimeMillis();

		// create another copy of the image to use as the initial value
		ClearCLBuffer gpuEstimate = output;
		clij.op().copy(gpuImg, gpuEstimate);

		// Get the CL Buffers, context, queue and device as long native pointers
		long longPointerImg = ((NativePointerObject) (gpuImg
			.getPeerPointer().getPointer())).getNativePointer();
		long longPointerPSF = ((NativePointerObject) (gpuPSF
			.getPeerPointer().getPointer())).getNativePointer();
		long longPointerOutput = ((NativePointerObject) (gpuEstimate
			.getPeerPointer().getPointer())).getNativePointer();
		long l_context = ((NativePointerObject) (clij.getClearCLContext()
			.getPeerPointer().getPointer())).getNativePointer();
		long l_queue = ((NativePointerObject) (clij.getClearCLContext()
			.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();
		long l_device = ((NativePointerObject) clij.getClearCLContext()
			.getDevice().getPeerPointer().getPointer()).getNativePointer();

		// call the decon wrapper (100 iterations of RL)
		clij2fftWrapper.conv_long(gpuImg.getDimensions()[0], gpuImg
			.getDimensions()[1], gpuImg.getDimensions()[2], longPointerImg,
			longPointerPSF, longPointerOutput, true, l_context, l_queue,
			l_device);

		long finish = System.currentTimeMillis();

		System.out.println("OpenCL Decon time " + (finish - start));

		return gpuEstimate;
	}

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
}
