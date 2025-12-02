package net.haesleinhuepf.clijx.imglib2cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.haesleinhuepf.clijx.CLIJx;
import net.haesleinhuepf.clijx.parallel.CLIJxPool;
import net.imglib2.type.numeric.real.FloatType;
import org.scijava.app.StatusService;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.converters.implementations.ClearCLBufferToRandomAccessibleIntervalConverter;
import net.haesleinhuepf.clijx.plugins.DeconvolveRichardsonLucyFFT;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * This class is used to deconvolve a cell of a larger image using CLij2-FFT Richardson-Lucy deconvolution.
 * It is designed to be used as an input to the {@link Lazy} class for lazy evaluation and caching.
 *
 * @param <T> pixel type of the deconvolved image - typically {@link FloatType}
 * @param <S> pixel type of the source image to deconvolve
 * @param <P> pixel type of the point spread function (PSF)
 *
 * @author bnorthan
 */
public class Clij2RichardsonLucyImglib2Cache<T extends RealType<T> & NativeType<T>, S extends RealType<S>, P extends RealType<P>>
		implements Consumer<RandomAccessibleInterval<T>> {

	private final RandomAccessibleInterval<S> source;
	private final long[] overlap;
	protected StatusService status = null;
	protected int total = -1;
	protected int current = 0;
	final int numberOfIterations;
	final float regularizationFactor;
	final boolean nonCirculant;
	final Map<CLIJx, ClearCLBuffer> psfPushed = new HashMap<>(); // Keeps track of PSF pushed per CLIJ2 instance
	final CLIJxPool clijxPool;
	final RandomAccessibleInterval<P> psf;

	/**
	 * Constructor for Clij2RichardsonLucyImglib2Cache.
	 * Should not be called directly; use the {@link Builder} instead.
	 *
	 * @param source the source image to deconvolve
	 * @param pool the pool of CLIJ2 instances for GPU processing
	 * @param psf the point spread function (PSF) used for deconvolution
	 * @param overlap the overlap in pixels between deconvolution tiles, for each dimension
	 * @param numberOfIterations the number of Richardson-Lucy iterations to perform
	 * @param regularizationFactor the regularization factor to dampen noise amplification
	 * @param nonCirculant if true, uses non-circulant boundary conditions; if false, uses circulant
	 */
	protected Clij2RichardsonLucyImglib2Cache(
			final RandomAccessibleInterval<S> source,
			final CLIJxPool pool,
			final RandomAccessibleInterval<P> psf,
			final long[] overlap,
			final int numberOfIterations,
			final float regularizationFactor,
			final boolean nonCirculant) {

		this.source = source;
		this.clijxPool = pool;
		final int n = source.numDimensions();
		if (n == overlap.length) {
			this.overlap = overlap;
		} else {
			this.overlap = Arrays.copyOf(overlap, n);
		}

		this.nonCirculant = nonCirculant;
		this.numberOfIterations = numberOfIterations;
		this.regularizationFactor = regularizationFactor;
		this.psf = psf;
	}

	/**
	 * Sets up the status service for progress reporting.
	 *
	 * @param status the status service to report progress
	 * @param total the total number of cells to deconvolve
	 */
	public void setUpStatus(StatusService status, int total) {
		this.status = status;
		this.total = total;
		this.current = 0;
	}

	/**
	 * Deconvolves a single cell of the source image.
	 * <p>
	 * This method:
	 * <ul>
	 *   <li>Extracts the interval defined by 'cell' and overlap from the source</li>
	 *   <li>Applies the CLij Richardson-Lucy deconvolution filter</li>
	 *   <li>Writes the result to the interval defined by 'cell' (excluding the padded area)</li>
	 * </ul>
	 *
	 * @param cell the cell to deconvolve, as a {@link RandomAccessibleInterval}
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void accept(final RandomAccessibleInterval<T> cell) {
		try {
			if (this.status != null) {
				this.status.showStatus(current, total, "deconvolving cell " + current + " of " + total );
			}
			
			current = current + 1;
			
			System.out.println(" ");
			System.out.println("========================================================");
			System.out.println("deconvolving cell " + current + " of " + total);

			// min and max of the cell we are computing values for
			final long[] min = new long[]{cell.min(0), cell.min(1), cell.min(2)};
			final long[] max = new long[]{cell.max(0), cell.max(1), cell.max(2)};

			// extended min and max when considering overlap
			final long[] mine = new long[cell.numDimensions()];
			final long[] maxe = new long[cell.numDimensions()];

			// overlap size
			long[] overlapmin = new long[cell.numDimensions()];
			long[] overlapmax = new long[cell.numDimensions()];

			// if at start or end of the image set overlap size to 0 otherwise use the
			// input overlap
			for (int d = 0; d < cell.numDimensions(); d++) {
				System.out.println("min/max cell " + min[d] + " " + max[d]);
				overlapmin[d] = overlap[d];
				overlapmax[d] = overlap[d];

				if (min[d] == 0) {
					overlapmin[d] = 0;
				}
				if (max[d] == source.dimension(d) - 1) {
					overlapmax[d] = 0;
				}
			}

			// calculated extended min and max to be the cell min and max +- the overlap
			mine[0] = min[0] - overlapmin[0];
			mine[1] = min[1] - overlapmin[1];
			mine[2] = min[2] - overlapmin[2];

			maxe[0] = max[0] + overlapmax[0];
			maxe[1] = max[1] + overlapmax[1];
			maxe[2] = max[2] + overlapmax[2];

			for (int d = 0; d < cell.numDimensions(); d++) {
				if (maxe[d] > source.max(d)) {
                    maxe[d] = source.max(d);
                }
			}
			
			System.out.println("mine " + Arrays.toString(mine)+")");
			System.out.println("maxe " + Arrays.toString(maxe)+")");
			
			RandomAccessibleInterval<S> inputRAI = null;

			try {
				// get the input RAI (using the min and max interval computed above)
				inputRAI = Views.interval(source, mine, maxe);
			} catch (Exception e) {
				System.out.println("Error getting Cell");
				e.printStackTrace();
				return;
			}
				// Getting one CLIJ2 instance
			// - if one is available, gets one instantly, and lock it while it is not recycled
			// - if none is available:
			//   - or waits for one to be available (Thread will park, it's not busy waiting)
			CLIJx clijx = clijxPool.getIdleCLIJx();
			// Using GPU named clij2.getGPUName()

			// convert input RAI to ClearCLBuffer
			final ClearCLBuffer input = clijx.push(Views.zeroMin(inputRAI));

			// create temporary buffer for output
			final ClearCLBuffer output = clijx.create(input);

			if (!psfPushed.containsKey(clijx)) {
				psfPushed.put(clijx, clijx.push(psf)); // Push one psf buffer per CLIJ2 only
			}

			DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clijx, input, psfPushed.get(clijx), output, numberOfIterations, regularizationFactor, nonCirculant);

			// convert CLBuffer result to RAI
			// at this point the result contains the padded area
			final ClearCLBufferToRandomAccessibleIntervalConverter cl2rai = new ClearCLBufferToRandomAccessibleIntervalConverter();
			cl2rai.setCLIJ(clijx.getCLIJ());
			final RandomAccessibleInterval<T> result = cl2rai.convert(output);

			input.close();
			output.close();

			// Set clij2 instance as available again
			clijxPool.setCLIJxIdle(clijx);

			// get the valid part of the extended deconvolution (ie exclude the padded area)
			RandomAccessibleInterval<T> valid = Views.zeroMin(Views.interval(
					result, new FinalInterval(new long[]{overlapmin[0], overlapmin[1], overlapmin[2]},
							new long[]{result.dimension(0) - overlapmax[0] - 1, result.dimension(
									1) - overlapmax[1] - 1, result.dimension(2) - overlapmax[2] - 1})));

			// copy the extended result to the original cell
			Util.copyReal(valid, Views.zeroMin(cell));

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builder for constructing a deconvolver.
	 *
	 * @return a new {@link Builder} instance
	 */
	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		// Compulsory
		private RandomAccessibleInterval source; // source the 3D (xyz) RAI to deconvolve
		private RandomAccessibleInterval psf; // psf the 3D (xyz) RAI of the point spread function
		// Optional
		private CLIJxPool pool = null;
		private long[] overlap = new long[]{10, 10, 10};
		private int numberOfIterations = 100;
		private float regularizationFactor = 0.0f;
		private boolean nonCirculant = true;

		/**
		 * Sets the source image to deconvolve.
		 *
		 * @param rai the source image
		 * @return the builder
		 */
		public Builder rai(RandomAccessibleInterval<? extends RealType<?>> rai) {
			this.source = rai;
			return this;
		}

		/**
		 * Sets the point spread function (PSF) for deconvolution.
		 *
		 * @param rai the PSF image
		 * @return the builder
		 */
		public Builder psf(RandomAccessibleInterval<? extends RealType<?>> rai) {
			this.psf = rai;
			return this;
		}

		/**
		 * Specifies the GPU pool to be used for deconvolution.
		 * Useful when sharing the same pool between different deconvolution tasks.
		 *
		 * @param pool the pool of CLIJ2 instances
		 * @return the builder
		 */
		public Builder useGPUPool(CLIJxPool pool) {
			if (this.pool != null) {
				System.err.println("The GPU pool was already defined and will be overridden");
			}
			this.pool = pool;
			return this;
		}

		/**
		 * Sets the overlap between deconvolution tiles, for each dimension.
		 *
		 * @param overlap the overlap in pixels, ordered as x, y, z
		 * @return the builder
		 */
		public Builder overlap(long... overlap) {
			this.overlap = overlap;
			return this;
		}

		/**
		 * Sets the overlap between deconvolution tiles, using the same value for all dimensions.
		 *
		 * @param overlap the overlap in pixels
		 * @return the builder
		 */
		public Builder overlap(long overlap) {
			this.overlap = new long[]{overlap, overlap, overlap};
			return this;
		}

		/**
		 * Sets the number of Richardson-Lucy iterations to perform.
		 *
		 * @param numberOfIterations the number of iterations (typically 100)
		 * @return the builder
		 */
		public Builder numberOfIterations(int numberOfIterations) {
			this.numberOfIterations = numberOfIterations;
			return this;
		}

		/**
		 * Sets the regularization factor to dampen noise amplification during deconvolution.
		 * A higher value results in stronger regularization.
		 *
		 * @param regularizationFactor the regularization factor
		 * @return the builder
		 */
		public Builder regularizationFactor(float regularizationFactor) {
			this.regularizationFactor = regularizationFactor;
			return this;
		}

		/**
		 * Sets whether to use non-circulant boundary conditions.
		 * If true, the deconvolution will not assume periodic boundaries.
		 *
		 * @param nonCirculant if true, uses non-circulant boundary conditions
		 * @return the builder
		 */
		public Builder nonCirculant(boolean nonCirculant) {
			this.nonCirculant = nonCirculant;
			return this;
		}

		/**
		 * Builds the deconvolver.
		 *
		 * @return a new {@link Clij2RichardsonLucyImglib2Cache} instance
		 * @throws IllegalArgumentException if source or PSF is not specified
		 */
		public Clij2RichardsonLucyImglib2Cache<FloatType, ? extends RealType<?>, ? extends RealType<?>> build() {

			if (source == null) throw new IllegalArgumentException("A source to deconvolve has to be specified");
			if (psf == null) throw new IllegalArgumentException("A point spread function has to be specified");
			if (pool == null) pool = CLIJxPool.getInstance();

			return new Clij2RichardsonLucyImglib2Cache<>(
					source,
					pool,
					psf,
					overlap,
					numberOfIterations,
					regularizationFactor,
					nonCirculant
			);
		}
	}

}
