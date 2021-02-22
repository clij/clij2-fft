
package net.haesleinhuepf.clijx.tests;

import java.io.IOException;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.plugins.DeconvolveRichardsonLucyFFT;
import net.haesleinhuepf.clijx.plugins.MultiplyComplexImages;
import net.haesleinhuepf.clijx.plugins.Normalize;
import net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility;
import net.haesleinhuepf.clijx.plugins.clij2fftWrapper;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.Dimensions;
import net.imglib2.FinalDimensions;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.ChannelARGBConverter.Channel;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;
import net.imglib2.view.Views;

import org.apache.http.HeaderElementIterator;
import org.jocl.NativePointerObject;

public class TextFFTConvolution<T extends RealType<T> & NativeType<T>> {

	final static ImageJ ij = new ImageJ();

	public static <T extends RealType<T> & NativeType<T>> void main(
		final String[] args) throws IOException
	{
		// check the library path, can be useful for debugging
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
		
		// TODO 1: Find supported FFT size FFT has optimal speed at power of 2.  Thus some implementations pad to power of 2
		// sometimes it is not desirable to pad so much (what if your size is 129?).  FFT is also efficient at "smooth' numbers
		// To further complicate things some FFT implementations only work for power of 2 or a smooth number.  CLFFT only works 
		// for smooth numbers. See https://ltfat.github.io/notes/ltfatnote017.pdf and 
		// https://github.com/imagej/imagej-ops/blob/f4bb1c15ad5591874a823e710ddf9c7c0512afa3/src/main/java/net/imagej/ops/filter/fftSize/NextSmoothNumber.java
		// so we will eventually need a way to pad arbitrary input size to a smooth number.  Currently in the decon this is done
		// by pulling the image from GPU and using ops to pad, then pushing back. 
		// See: https://github.com/clij/clij2-fft/blob/master/src/main/java/net/haesleinhuepf/clijx/plugins/OpenCLFFTUtility.java#L137
		//
		// In the example try changing 'widthToTry' to 511... you should an empty result because this example does not pad to a smooth size
		// and we don't yet handle the error properly.
		int widthToTry = 512;
		int heightToTry = 512;
	
		// we can find the supported FFT size using ops.  So could re-use or re-implement this in CLIJ
		long[][] nextFastFFTDimensions=ij.op().filter().fftSize(new FinalDimensions(widthToTry,heightToTry), false);
		System.out.println("next fast image size size "+nextFastFFTDimensions[0][0]+" "+nextFastFFTDimensions[0][1]);
		System.out.println("size of resulting complex FFT "+nextFastFFTDimensions[1][0]+" "+nextFastFFTDimensions[1][1]);
		System.out.println("note because of symmetrry ");
		
		// now load data
		Dataset dataset = (Dataset) ij.io().open("/home/bnorthan/code/images/bridge.tif");
			
		// TODO 2 we need to convert to 32 because so far only 32 float FFT has been wrapped from clfft, 
		// the dimensionality, dimensions and precision is defined by a 'plan' 
		// see here https://github.com/clij/clij2-fft/blob/master/native/clij2fft/clij2fft.cpp#L402
		RandomAccessibleInterval<FloatType> img = ij.op().convert().float32((Img)dataset);
		img = (RandomAccessibleInterval)ij.op().transform().crop(img, Intervals.createMinMax(0,0,widthToTry-1, heightToTry-1));
			
		// create a PSF to test convolution
		RandomAccessibleInterval<T> psf=(Img)ij.op().create().kernelGauss(4., 2, new FloatType());
    
		// TODO 3 - for FFT convolution the PSF has to be padded to the same size and input and shifted
		// there is currently a function in OpenCLFFTUTility that does this on the GPU but it only works for 3D
		// see OpenCLFFTUtility.padShiftFFTKernel(clij2, gpu_psf_original, gpu_psf);
		// so for our 2D example we'll use ops
		psf = (RandomAccessibleInterval)ij.op().filter().padShiftFFTKernel(psf, img);	
		
		ClearCLBuffer gpuImage= clij2.push(img);
		ClearCLBuffer gpuPSF= clij2.push(psf);
		ClearCLBuffer gpuConvolved= clij2.create(new long[] {gpuImage.getWidth(), gpuImage.getHeight()}, NativeTypeEnum.Float);
		
		clij2.show(psf, "psf");
		clij2.show(gpuPSF, "gpu_psf");
		clij2.show(gpuImage, "gpu_img ");
		
		// compute FFT dimensions, FFTs of a real signal are conjugate symmetric so half of the data is discarded
		long fftWidth = (gpuImage.getWidth() / 2 + 1);
		long fftHeight = gpuImage.getHeight();
		
		long[] fftDim = new long[] { fftWidth*2, fftHeight};

		// create GPU memory for FFT
		ClearCLBuffer gpuFFTImg= clij2.create(fftDim, NativeTypeEnum.Float);
		ClearCLBuffer gpuFFTPSF = clij2.create(fftDim, NativeTypeEnum.Float);
		
		// TODO 4 now we want to run the FFT for the image and psf.  We could do it by calling
		// https://github.com/clij/clij2-fft/blob/master/src/main/java/net/haesleinhuepf/clijx/plugins/OpenCLFFTUtility.java#L85
		// this wraps the code that gets the pointers to the context, queue and GPU memory however right now only 2d float forward FFT 
		// is wrapped in such a way.  We would need to at the very least also wrap 2d float inverse, and 3d float forward and inverse

		// get the long pointers to in, out, context and queue.
		long lContext = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
			.getPeerPointer().getPointer())).getNativePointer();
		long lQueue = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext()
			.getDefaultQueue().getPeerPointer().getPointer())).getNativePointer();

		
		long lIn = ((NativePointerObject) (gpuImage.getPeerPointer()
			.getPointer())).getNativePointer();
		long lOut = ((NativePointerObject) (gpuFFTImg.getPeerPointer()
			.getPointer())).getNativePointer();
		// call the native code that runs the FFT
		clij2fftWrapper.fft2d_32f_lp((long) (gpuImage.getWidth()), gpuImage.getHeight(),
			lIn, lOut, lContext, lQueue);

			// use a hack to get the long pointers to in, out, context and queue.
		long lInPSF = ((NativePointerObject) (gpuPSF.getPeerPointer()
			.getPointer())).getNativePointer();
		long lOutPSF = ((NativePointerObject) (gpuFFTPSF.getPeerPointer()
			.getPointer())).getNativePointer();
	
		// now FFT for the PSF
		clij2fftWrapper.fft2d_32f_lp((long) (gpuPSF.getWidth()), gpuPSF.getHeight(),
			lInPSF, lOutPSF, lContext, lQueue);

		// TODO 5:  Now we show the FFT, however we don't have a nice way to show complex conjugate interleaved.  
		// Ideally we need utilities that can be used to show real, imaginary, phas and power (like in imglib2)
		// Also we may want to center the power spectrum FFT and reflect it, as this is the format that ImageJ1 uses to show FFT
		clij2.show(gpuFFTImg, "FFT image");
		clij2.show(gpuFFTPSF, "FFT psf");
		
		// OK new we create a buffer for the FFT output
		ClearCLBuffer complexOutput = clij2.create(fftDim, NativeTypeEnum.Float);

		// and mulitply in the frequency domain (see https://en.wikipedia.org/wiki/Convolution_theorem)
    MultiplyComplexImages.multiplyComplexImages(clij2, gpuFFTImg, gpuFFTPSF, complexOutput);
	
    clij2.show(complexOutput, "FFTs multiplied");
    
    // use a hack to get the long pointers to in, out, context and queue.
		long lComplexOutput = ((NativePointerObject) (complexOutput.getPeerPointer()
			.getPointer())).getNativePointer();
		long lConvolved = ((NativePointerObject) (gpuConvolved.getPeerPointer()
			.getPointer())).getNativePointer();
	
		// now the inverse of FFT
		clij2fftWrapper.fft2dinv_32f_lp(gpuConvolved.getWidth(), gpuConvolved.getHeight(), lComplexOutput, lConvolved, lContext, lQueue);
		
		// and finally we can show our convolution
		clij2.show(gpuConvolved, "gpu_convolved");
	
	}
}
