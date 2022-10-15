
package net.haesleinhuepf.clijx.tests;

import java.io.IOException;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.plugins.ConvolveFFT;
import net.haesleinhuepf.clijx.plugins.DeconvolveRichardsonLucyFFT;
import net.haesleinhuepf.clijx.plugins.Normalize;
import net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility;
import net.haesleinhuepf.clijx.plugins.clij2fftWrapper;
import net.haesleinhuepf.clijx.CLIJx;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.converter.ChannelARGBConverter.Channel;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class InteractiveConvolve<T extends RealType<T> & NativeType<T>> {

	final static ImageJ ij = new ImageJ();

	public static <T extends RealType<T> & NativeType<T>> void main(
		final String[] args) throws IOException
	{
		// check the library path, can be useful for debugging
		System.out.println(System.getProperty("java.library.path"));
		
		clij2fftWrapper.diagnostic();
		
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
		
		Dataset img = (Dataset) ij.io().open("D:\\images/images/Bars-stack.tif");
		
		Dataset psf= (Dataset) ij.io().open("D:\\images/images/PSF-Bars-stack-cropped-64.tif");
	
		clij2.show(img, "img ");
		clij2.show(psf, "psf ");
	
		ClearCLBuffer gpu_psf = clij2.push(psf);
		ClearCLBuffer gpu_image = clij2.push(img);
		
		ClearCLBuffer gpu_convolved = clij2.create(gpu_image.getDimensions(), NativeTypeEnum.Float);

		
		// convolve the image
		ConvolveFFT.convolveFFT(clij2, gpu_image, gpu_psf, gpu_convolved, false);
		
		RandomAccessibleInterval convolvedRAI = clij2.pullRAI(gpu_convolved);

		clij2.show(convolvedRAI, "convolved");

	}
}
