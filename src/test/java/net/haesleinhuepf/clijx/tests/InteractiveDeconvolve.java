
package net.haesleinhuepf.clijx.tests;

import java.io.IOException;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.plugins.DeconvolveRichardsonLucyFFT;
import net.haesleinhuepf.clijx.plugins.clij2fftWrapper;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class InteractiveDeconvolve<T extends RealType<T> & NativeType<T>> {
	
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
		
		// load data
<<<<<<< HEAD
	//	Dataset img = (Dataset) ij.io().open("/home/bnorthan/code/images/Bars-G10-P15-stack-cropped.tif");
	//	Dataset psf = (Dataset) ij.io().open("/home/bnorthan/code/images/PSF-Bars-stack-cropped-64.tif");
		
	//	Dataset img = (Dataset) ij.io().open("D:\\images/images/Bars-G10-P15-stack-cropped.tif");
	//	Dataset psf = (Dataset) ij.io().open("D:\\images/images/PSF-Bars-stack-cropped-64.tif");
		
	//	Dataset img = (Dataset) ij.io().open("D:\\images\\From Roman Guiet July 13th 2021\\clij_deconv_data\\D_X555_Ph488_Mit647_4-c3_crop1.tif");
	//	Dataset psf = (Dataset) ij.io().open("D:\\\\images\\\\From Roman Guiet July 13th 2021\\\\clij_deconv_data\\\\PSF_w500_256x256x41_squared.tif");
		
		Dataset img = (Dataset) ij.io().open("D:\\images\\tnia-python-images\\imagesc\\2024_02_15_clij_z_tiling\\im.tif");
		Dataset psf = (Dataset) ij.io().open("D:\\images\\tnia-python-images\\imagesc\\2024_02_15_clij_z_tiling\\psf_1.tif");
		
		
		//	Dataset img = (Dataset) ij.io().open("D:\\images\\decon-phantoms\\spheres_cropped.tiff");
	//	Dataset psf = (Dataset) ij.io().open("D:\\images\\decon-phantoms\\psf.tiff");
=======
		//	Dataset img = (Dataset) ij.io().open("/home/bnorthan/code/images/Bars-G10-P15-stack-cropped.tif");
		//	Dataset psf = (Dataset) ij.io().open("/home/bnorthan/code/images/PSF-Bars-stack-cropped-64.tif");
			
		//	Dataset img = (Dataset) ij.io().open("D:\\images/images/Bars-G10-P15-stack-cropped.tif");
		//	Dataset psf = (Dataset) ij.io().open("D:\\images/images/PSF-Bars-stack-cropped-64.tif");
			
		//	Dataset img = (Dataset) ij.io().open("D:\\images\\From Roman Guiet July 13th 2021\\clij_deconv_data\\D_X555_Ph488_Mit647_4-c3_crop1.tif");
		//	Dataset psf = (Dataset) ij.io().open("D:\\\\images\\\\From Roman Guiet July 13th 2021\\\\clij_deconv_data\\\\PSF_w500_256x256x41_squared.tif");
			
		//	Dataset img = (Dataset) ij.io().open("D:\\images\\decon-phantoms\\spheres_cropped.tiff");
		//	Dataset psf = (Dataset) ij.io().open("D:\\images\\decon-phantoms\\psf.tiff");
>>>>>>> fbe9af910534f683fbc778c9da75b2dd724f60ac
	
		//Dataset img = (Dataset) ij.io().open("C:/structure/data/Deconvolution_Brian/Bars-G10-P15-stack-cropped.tif");
		//Dataset psf = (Dataset) ij.io().open("C:/structure/data/Deconvolution_Brian/PSF-Bars-stack-cropped-64.tif");

		//Dataset img = (Dataset) ij.io().open("C:\\Users\\bnort\\ImageJ2018\\ops-experiments\\images/Bars-G10-P15-stack-cropped.tif");
		//Dataset psf = (Dataset) ij.io().open("C:\\Users\\bnort\\ImageJ2018\\ops-experiments\\images/PSF-Bars-stack-cropped-64.tif");
		
		//Dataset img = (Dataset) ij.io().open("/home/bnorthan/Images/Deconvolution/CElegans_April_2020/CElegans-CY3.tif");
		//Dataset psf = (Dataset) ij.io().open("/home/bnorthan/Images/Deconvolution/CElegans_April_2020/PSF-CElegans-CY3-cropped.tif");
		
		Dataset img = (Dataset) ij.io().open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/im.tif");
		//Dataset img = (Dataset) ij.io().open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/half_bead.tif");
		//Dataset img = (Dataset) ij.io().open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/half_bead_266_266_512.tif");
		//Dataset img = (Dataset) ij.io().open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/ones_266_266_512.tif");
		Dataset psf = (Dataset) ij.io().open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/psf.tif");
		
		// show image and PSF
		
		clij2.show(img, "img ");
		clij2.show(psf, "psf ");
		
		// crop PSF - the image will be extended using PSF size
		// if the PSF size is too large it will explode image size, memory needed and processing speed
		// so crop just small enough to capture significant signal of PSF 
		//	psfF = ImageUtility.cropSymmetric(psfF,
		//			new long[] { 64, 64, 41 }, ij.op());
		// ij.ui().show(Views.zeroMin(psfF));
		
		ClearCLBuffer gpu_psf = clij2.push(psf);
		ClearCLBuffer gpu_image = clij2.push(img);
		
		ClearCLBuffer gpu_deconvolved = clij2.create(gpu_image.getDimensions(), NativeTypeEnum.Float);
		ClearCLBuffer gpu_deconvolved_tv = clij2.create(gpu_image.getDimensions(), NativeTypeEnum.Float);

		boolean tile = true;
		
		if (!tile) {
			// deconvolve the image
			//DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpu_image, gpu_psf, gpu_deconvolved, 100, 0.0f, false);
			DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpu_image, gpu_psf, gpu_deconvolved_tv, 100, 0.002f, true   );
			//DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpu_image, gpu_psf, gpu_deconvolved, 100, 0.0f, true);
			//DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpu_image, gpu_psf, gpu_deconvolved_tv, 100, 0.02f, false);
		}
		else {
			int numTilesX = 2;
			int numTilesY = 2;
			
			int margin = 8;
			
			int tileWidth = (int)Math.floor(gpu_image.getWidth() / numTilesX);
			int tileHeight = (int)Math.floor(gpu_image.getHeight() / numTilesY);
			int tileDepth = (int)gpu_image.getDepth();
			
			CLIJx clijx=CLIJx.getInstance();
			
			for (int x = 0; x < numTilesX; x++) {
				for (int y = 0; y < numTilesY; y++) {
					System.out.println(x+" "+y);
					ClearCLBuffer tempIn = clijx.pushTile(gpu_image, x, y, 0, tileWidth, tileHeight, tileDepth, margin, margin, 0);
					ClearCLBuffer tempOut = clij2.create(tempIn.getDimensions(), NativeTypeEnum.Float);
					DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, tempIn, gpu_psf, tempOut, 100, 0.0f, true);
					
					clijx.show(tempOut, "tile");
					clijx.pullTile(tempOut, gpu_deconvolved, x, y, 0, tileWidth, tileHeight, tileDepth, margin, margin, 0);
				
					tempOut.close();
				}
			}
		}
		RandomAccessibleInterval deconvolvedRAI = clij2.pullRAI(gpu_deconvolved);
		//RandomAccessibleInterval deconvolvedRAI_tv = clij2.pullRAI(gpu_deconvolved_tv);

		//clij2.show(deconvolvedRAI, "deconvolved");
		//clij2.show(deconvolvedRAI_tv, "deconvolved tv");
		ij.ui().show("deconvolved", deconvolvedRAI);

	}
}
