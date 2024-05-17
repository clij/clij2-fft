
package net.haesleinhuepf.clijx.tests;

import java.io.IOException;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.plugins.DeconvolveRichardsonLucyFFT;
import net.haesleinhuepf.clijx.plugins.ForwardFFT;
import net.haesleinhuepf.clijx.plugins.InverseFFT;
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

public class LaunchIJ<T extends RealType<T> & NativeType<T>> {

	final static ImageJ ij = new ImageJ();

	public static <T extends RealType<T> & NativeType<T>> void main(final String[] args) throws IOException {
		// check the library path, can be useful for debugging
		System.out.println(System.getProperty("java.library.path"));

		// launch IJ so we can interact with the inputs and outputs
		ij.launch(args);

		CLIJ2 clij2 = null;
		// get clij
		try {
			clij2 = CLIJ2.getInstance("RTX");
		} catch (Exception e) {
			System.out.println(e);
			return;
		}

		int dataNum = 3;
		
		if (dataNum==0) {
			// do nothing 
		}
		// bridge
		if (dataNum==1) {
			// bridge Brian's windows computer
			Dataset dataset = (Dataset) ij.io().open("D:\\images/images/bridge.tif"); //
			// create a PSF to test convolution
			RandomAccessibleInterval<T> psf=(Img)ij.op().create().kernelGauss(4., dataset.numDimensions(), new FloatType());

			clij2.show(dataset, "data");
			clij2.show(psf, "psf");
		}
		// bars 
		else if (dataNum==2) {
				// bars....
		 Dataset dataset = (Dataset) ij.io().open("D:\\images/images/Bars-G10-P15-stack-cropped.tif");
		 Dataset psf= (Dataset)
				 ij.io().open("D:\\images/images/PSF-Bars-stack-cropped-64.tif");
		
		 clij2.show(dataset, "data");
		 clij2.show(psf, "psf");
		
		}
		
		else if (dataNum==3) {
		 		Dataset dataset = (Dataset) ij.io()
				.open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/im.tif");
		Dataset psf = (Dataset) ij.io()
				.open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/psf.tif");

	
		 //Dataset dataset = (Dataset) ij.io()
		//		.open("D:\\images\\tnia-python-images\\imagesc/2024_02_15_clij_z_tiling/im.tif");
		 //Dataset psf = (Dataset) ij.io()
		//		.open("D:\\images\\tnia-python-images\\imagesc/2024_02_15_clij_z_tiling/psf.tif");
		 clij2.show(dataset, "data");
		 clij2.show(psf, "psf");
		}
				
		//Dataset dataset = (Dataset) ij.io().open("/home/bnorthan/code/support/imagejMacros/DeconvolutionDemos/C1-YeastTNA1_1516_conv_RG_26oC_003_256xcropSub100.tif");
		//Dataset psf = (Dataset) ij.io().open("/home/bnorthan/code/support/imagejMacros/DeconvolutionDemos/gpsf_3D_1514_a3_001_WF-sub105crop64_zcentred.tif");
  
		// bars....
		// Dataset dataset = (Dataset)
		// ij.io().open("/home/bnorthan/code/images/Bars-G10-P15-stack-cropped.tif");
		//Dataset dataset = (Dataset) ij.io().open("D:\\images/images/Bars-G10-P15-stack-cropped.tif");

		// create a PSF to test convolution
		//RandomAccessibleInterval<T> psf = (Img) ij.op().create().kernelGauss(4., dataset.numDimensions(),
		//		new FloatType());

	}
}
