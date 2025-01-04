package net.haesleinhuepf.clijx.tests;

import java.io.IOException;

import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.imglib2cache.Lazy;
import net.haesleinhuepf.clijx.imglib2cache.Clij2RichardsonLucyImglib2Cache;
import net.haesleinhuepf.clijx.plugins.clij2fftWrapper;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;

public class InteractiveImgLib2CacheDeconvolve<T extends RealType<T> & NativeType<T>> {

	final static ImageJ ij = new ImageJ();

	public static <T extends RealType<T> & NativeType<T>> void main(final String[] args) throws IOException {
		// check the library path, can be useful for debugging
		System.out.println(System.getProperty("java.library.path"));

		clij2fftWrapper.diagnostic();

		// launch IJ so we can interact with the inputs and outputs
		ij.launch(args);
		String gpuId = "RTX";

		final CLIJ2 clij2 = CLIJ2.getInstance(gpuId);

		// load data
		// (keep commented out list of datasets we've tested with previously and on
		// different machines)

		// Dataset img = (Dataset)
		// ij.io().open("C:/structure/data/Deconvolution_Brian/Bars-G10-P15-stack-cropped.tif");
		// Dataset psf = (Dataset)
		// ij.io().open("C:/structure/data/Deconvolution_Brian/PSF-Bars-stack-cropped-64.tif");

		// Dataset img = (Dataset)
		// ij.io().open("C:\\Users\\bnort\\ImageJ2018\\ops-experiments\\images/Bars-G10-P15-stack-cropped.tif");
		// Dataset psf = (Dataset)
		// ij.io().open("C:\\Users\\bnort\\ImageJ2018\\ops-experiments\\images/PSF-Bars-stack-cropped-64.tif");

		// Dataset img = (Dataset)
		// ij.io().open("/home/bnorthan/Images/Deconvolution/CElegans_April_2020/CElegans-CY3.tif");
		// Dataset psf = (Dataset)
		// ij.io().open("/home/bnorthan/Images/Deconvolution/CElegans_April_2020/PSF-CElegans-CY3-cropped.tif");

		 //Dataset img = (Dataset)
		 //ij.io().open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/half_bead.tif");
		 //Dataset img = (Dataset)
		 //ij.io().open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/half_bead_266_266_512.tif");
		 //Dataset img = (Dataset)
		 //ij.io().open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/ones_266_266_512.tif");

		//Dataset imgD = (Dataset) ij.io()
		//		.open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/im.tif");
		//Dataset psfD = (Dataset) ij.io()
		//		.open("/home/bnorthan/images/tnia-python-images/imagesc/2024_02_15_clij_z_tiling/psf.tif");

		Dataset imgD = (Dataset) ij.io()
				.open("D:\\images\\tnia-python-images\\imagesc\\2024_06_03_clij_z_error\\im.tif");
		Dataset psfD = (Dataset) ij.io()
				.open("D:\\images\\tnia-python-images\\imagesc\\2024_06_03_clij_z_error\\psf.tif");
		
		RandomAccessibleInterval<FloatType> img = (RandomAccessibleInterval<FloatType>) imgD.getImgPlus();
		RandomAccessibleInterval<FloatType> psf = (RandomAccessibleInterval<FloatType>) psfD.getImgPlus();

		// show image and PSF
		clij2.show(img, "img ");
		clij2.show(psf, "psf ");

		// create the version of clij2 RL that works on cells
		Clij2RichardsonLucyImglib2Cache<FloatType, FloatType> op =
				Clij2RichardsonLucyImglib2Cache.<FloatType, FloatType>builder(img, psf)
						.overlap(10).useGPU(gpuId).build();
	
		// here we use the imglib2cache lazy 'generate' utility
		// first parameter is the image to process
		// second parameter is the cell size (which we set to half the original dimension in each direction)
		CachedCellImg<FloatType, ArrayDataAccess<FloatType>> decon = (CachedCellImg) Lazy.generate(img,
				new int[] { (int) img.dimension(0) / 2, (int) img.dimension(1) / 2, (int) img.dimension(2) / 2 },
				new FloatType(), AccessFlags.setOf(AccessFlags.VOLATILE), op);

		// trigger processing of the entire volume
		// (otherwise processing will be triggerred as we view different parts of the volume,
		// which is sometimes the behavior we want, but sometiems we'd rather process everything before hand,
		// so viewing is responsive)
		// so in this case the main reason for using imglib2 cache is as a convenient mechanism for chunking the image
		// and we don't fully take advantage of the 'just in time' aspect. 
		decon.getCells().forEach(Cell::getData);

		clij2.show(decon, "decon");
		
	}
}
