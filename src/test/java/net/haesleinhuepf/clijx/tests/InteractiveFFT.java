package net.haesleinhuepf.clijx.tests;

import java.io.IOException;

import net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.complex.ComplexFloatType;
import net.imglib2.type.numeric.real.FloatType;

public class InteractiveFFT {
	
	final static ImageJ ij = new ImageJ();
	
	public static <T extends RealType<T> & NativeType<T>> void main(
		final String[] args) throws IOException
	{
		ij.launch(args);
		
		System.out.println(System.getProperty("java.library.path"));

		// load the dataset
		Dataset dataset = (Dataset) ij.io().open("../../images/bridge.tif");

		// convert to 32 bit 
		Img<FloatType> img = ij.op().convert().float32((Img)dataset.getImgPlus().getImg());
		
		// show the image
		ij.ui().show(img);
		
		// run the FFT
		RandomAccessibleInterval<ComplexFloatType> resultComplex = OpenCLFFTUtility.runFFT(img, true, ij.op());
		
		// show the result
		ImageJFunctions.show(resultComplex, "FFT OpenCL");
		
	}
}
