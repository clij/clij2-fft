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
package net.haesleinhuepf.clijx.tests;

import bdv.cache.SharedQueue;
import bdv.util.BdvFunctions;
import bdv.util.BdvOptions;
import bdv.util.BdvStackSource;
import bdv.util.volatiles.VolatileViews;
import ij.IJ;
import net.haesleinhuepf.clij.CLIJ;
import net.haesleinhuepf.clijx.parallel.CLIJPoolOptions;
import net.haesleinhuepf.clijx.parallel.CLIJxPool;
import net.haesleinhuepf.clijx.imglib2cache.Clij2RichardsonLucyImglib2Cache;
import net.haesleinhuepf.clijx.imglib2cache.Lazy;
import net.haesleinhuepf.clijx.plugins.clij2fftWrapper;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.Volatile;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.array.ArrayDataAccess;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.ARGBType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/**
 * Demo of GPU deconvolution with multiple GPUs.
 * Tiled, interactive or non-interactive
 * This demo is self-contained - the first execution will download the data on user home folder
 * <p>
 * Nico's test:
 * Deconv with RTX A500 only: 26s
 * Deconv with Intel Iris only: 48s
 * Deconv with both RTX + Iris: 19s (theory: 1/(1/26+1/48) = 18s)
 */
public class InteractiveSelfContainedImgLib2CacheDeconvolve {

	final static ImageJ ij = new ImageJ();

	public static void main(final String[] args) throws Exception {
		// check the library path, can be useful for debugging
		System.out.println(System.getProperty("java.library.path"));

		clij2fftWrapper.diagnostic();

		ij.ui().showUI();

		List<String> deviceNames = CLIJ.getAvailableDeviceNames();

		if (deviceNames.isEmpty()) {
			System.err.println("This demo requires at least one CLIJ compatible device.");
			return;
		}

		// Gets PSF
		File psfFile = DatasetHelper.getDataset("https://zenodo.org/records/5101351/files/PSFHuygens_confocal_Theopsf.tif");
		Dataset psfD = (Dataset) ij.io().open(psfFile.getAbsolutePath());
		RandomAccessibleInterval<FloatType> psf = (RandomAccessibleInterval<FloatType>) psfD.getImgPlus();

		// Crops PSF because it's way too large
		int radius = 32;
		psf = Views.interval(psf, new long[]{512-radius, 512-radius, 0}, new long[]{512+radius, 512+radius, 40});

		// Gets image
		File imgFile = DatasetHelper.getDataset("https://zenodo.org/records/5101351/files/Raw_large.tif");
		Dataset imgD = (Dataset) ij.io().open(imgFile.getAbsolutePath());
		RandomAccessibleInterval<FloatType> img = (RandomAccessibleInterval<FloatType>) imgD.getImgPlus();

		IJ.run("CLIJ Pool Options"); // Prompts user to specify pool
		// Alternatively you can specify programmatically the pool with
		// CLIJPoolOptions.set("0:2, 1:4"); // This means the device 0 has 2 threads and the device 1 has 4 threads
		// CLIJPoolOptions.set sets a persistent IJ preference, it needs to be set before the first CLIJxPool.getInstance() call to have any effect

		// show image and PSF
		ImageJFunctions.show(img, "img");
		ImageJFunctions.show(psf, "psf");

		// Create the version of clij2 RL that works on cells
		Clij2RichardsonLucyImglib2Cache op =
				Clij2RichardsonLucyImglib2Cache.builder()
						.rai(img).psf(psf)
						.overlap(10)
						.regularizationFactor(0.001f)
						.numberOfIterations(50)
						.nonCirculant(false)
						.useGPUPool(CLIJxPool.getInstance()) // in fact this is the default behaviour, but one can specify a different pool if necessary here
						.build();

		// here we use the imglib2cache lazy 'generate' utility
		// first parameter is the image to process
		// second parameter is the cell size (which we set to a quarter of the original dimension in each direction)
		CachedCellImg<FloatType, ArrayDataAccess<FloatType>> decon = (CachedCellImg) Lazy.generate(img,
				new int[] { (int) img.dimension(0) / 4, (int) img.dimension(1) / 4, (int) img.dimension(2) / 1 },
				new FloatType(), AccessFlags.setOf(AccessFlags.VOLATILE), op);


		// Change this flag to switch between:
		// - a parallel compute all strategy
		// - vs on demand computation depending on what is displayed
		boolean interactive = true;

		if (interactive) {
			// Interactive display
			// Number of fetcher threads should be higher than the number of GPUs - otherwise they are not used
			SharedQueue queue = new SharedQueue(10,1);

			BdvStackSource<Volatile<FloatType>> deconStackSource = BdvFunctions.show(VolatileViews.wrapAsVolatile(decon, queue), "decon");
			deconStackSource.getConverterSetups().get(0).setDisplayRange(0,200);
			deconStackSource.getConverterSetups().get(0).setColor(new ARGBType(ARGBType.rgba(0,255,0,0)));

			BdvStackSource<?> imgStackSource = BdvFunctions.show(img, "img", BdvOptions.options().addTo(deconStackSource.getBdvHandle()));
			imgStackSource.getConverterSetups().get(0).setColor(new ARGBType(ARGBType.rgba(255,0,255,0)));
			imgStackSource.getConverterSetups().get(0).setDisplayRange(0,200);

			deconStackSource.getBdvHandle().getSplitPanel().setCollapsed(false); // Opens split panel for bdv newbies
		} else {

			Instant start = Instant.now();

			// process entire volume in parallel
			decon.getCells()
					.parallelStream() // Parallel magic, comment to go serial
					.forEach(Cell::getData);

			Duration processingDuration = Duration.between(start, Instant.now());
			System.out.println("Deconvolution duration = "+processingDuration.toMillis()+" ms");
			ImageJFunctions.show(decon, "decon");
		}

	}


	/**
	 * A utility class that helps loading and caching file from the internet
	 */
	public static class DatasetHelper {

		public static final File cachedSampleDir = new File(System.getProperty("user.home"),"CachedSamples");

		public static File urlToFile(URL url, Function<String, String> decoder) {
			try {
				File file_out = new File(cachedSampleDir,decoder.apply(url.getFile()));
				if (file_out.exists()) {
					return file_out;
				} else {
					System.out.println("Downloading and caching: "+url+" size = "+ (getFileSize(url)/1024) +" kb");
					FileUtils.copyURLToFile(url, file_out, 10000, 10000);
					System.out.println("Downloading and caching of "+url+" completed successfully ");
					return file_out;
				}
			} catch (IOException e) {
				e.printStackTrace();
				return null;
			}
		}

		static Function<String, String> decoder = (str) -> {
			try {
				return URLDecoder.decode(str, "UTF-8");
			} catch(Exception e){
				e.printStackTrace();
				return str;
			}
		};

		public static File getDataset(String urlString) {
			return getDataset(urlString, decoder);
		}

		public static File getDataset(String urlString, Function<String, String> decoder) {
			URL url = null;
			try {
				url = new URL(urlString);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			return urlToFile(url, decoder);
		}

		// https://stackoverflow.com/questions/12800588/how-to-calculate-a-file-size-from-url-in-java
		private static int getFileSize(URL url) {
			URLConnection conn = null;
			try {
				conn = url.openConnection();
				if(conn instanceof HttpURLConnection) {
					((HttpURLConnection)conn).setRequestMethod("HEAD");
				}
				conn.getInputStream();
				return conn.getContentLength();
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				if(conn instanceof HttpURLConnection) {
					((HttpURLConnection)conn).disconnect();
				}
			}
		}

	}

}
