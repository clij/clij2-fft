
package net.haesleinhuepf.clijx.tests;

import java.io.IOException;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.plugins.DeconvolveRichardsonLucyFFT;
import net.haesleinhuepf.clijx.plugins.Normalize;
import net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

public class InteractiveDeconvolve<T extends RealType<T> & NativeType<T>> {

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

		// load data
		//Dataset testData = (Dataset) ij.io().open("/home/bnorthan/code/images/Bars-G10-P15-stack-cropped.tif");
		//Dataset psf = (Dataset) ij.io().open("/home/bnorthan/code/images/PSF-Bars-stack-cropped-64.tif");
		Dataset testData = (Dataset) ij.io().open("C:/structure/data/Deconvolution_Brian/Bars-G10-P15-stack-cropped.tif");
		Dataset psf = (Dataset) ij.io().open("C:/structure/data/Deconvolution_Brian/PSF-Bars-stack-cropped-64.tif");

		//Dataset testData = (Dataset) ij.io().open("C:\\Users\\bnort\\ImageJ2018\\ops-experiments\\images/Bars-G10-P15-stack-cropped.tif");
		//Dataset psf = (Dataset) ij.io().open("C:\\Users\\bnort\\ImageJ2018\\ops-experiments\\images/PSF-Bars-stack-cropped-64.tif");
		
		//Dataset testData = (Dataset) ij.io().open("/home/bnorthan/Images/Deconvolution/CElegans_April_2020/CElegans-CY3.tif");
		//Dataset psf = (Dataset) ij.io().open("/home/bnorthan/Images/Deconvolution/CElegans_April_2020/PSF-CElegans-CY3-cropped.tif");
		
		// convert input data to float
		RandomAccessibleInterval<FloatType> imgF = (RandomAccessibleInterval) (ij
			.op().convert().float32((Img) testData.getImgPlus().getImg()));
		
		RandomAccessibleInterval<FloatType> psfF = (RandomAccessibleInterval) (ij
			.op().convert().float32((Img) psf.getImgPlus()));

		// show image and PSF
		clij2.show(imgF, "img ");
		clij2.show(psfF, "psf ");

		// crop PSF - the image will be extended using PSF size
		// if the PSF size is too large it will explode image size, memory needed and processing speed
		// so crop just small enough to capture significant signal of PSF 
		//	psfF = ImageUtility.cropSymmetric(psfF,
		//			new long[] { 64, 64, 41 }, ij.op());
		// ij.ui().show(Views.zeroMin(psfF));
		
		ClearCLBuffer gpu_psf = clij2.push(psfF);
		ClearCLBuffer gpu_image = clij2.push(imgF);
		ClearCLBuffer gpu_deconvolved = clij2.create(gpu_image);

		// deconvolve the image
		DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpu_image, gpu_psf, gpu_deconvolved, 100);

		RandomAccessibleInterval deconvolvedRAI = clij2.pullRAI(gpu_deconvolved);

		clij2.show(deconvolvedRAI, "deconvolved");
		//ij.ui().show("deconvolved", deconvolvedRAI);

	}
}
