package net.haesleinhuepf.clijx.imglib2cache;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import net.haesleinhuepf.clijx.CLIJ2Pool;
import net.haesleinhuepf.clijx.ResourcePool;
import org.scijava.app.StatusService;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.converters.implementations.ClearCLBufferToRandomAccessibleIntervalConverter;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.plugins.DeconvolveRichardsonLucyFFT;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * 
 * @author bnorthan
 * 
 * This class is used to deconvolve a cell of a larger image using clij2-fft Richardson Lucy
 * <p>
 * It is designed to be used as an input to the {@link Lazy} class
 * 
 * @param <T>
 * @param <S>
 */
public class Clij2RichardsonLucyImglib2Cache<T extends RealType<T> & NativeType<T>, S extends RealType<S>> implements Consumer<RandomAccessibleInterval<T>>{

	private final RandomAccessibleInterval<S> source;
	private final long[] overlap;
	protected StatusService status = null;
	protected int total = -1;
	protected int current = 0;
	final int numberOfIterations;
	final float regularizationFactor;
	final boolean nonCirculant;
	final Map<CLIJ2, ClearCLBuffer> psfPushed = new HashMap<>(); // Keeps track of PSF pushed per CLIJ2 instance
	final ResourcePool<CLIJ2> clij2Pool;
	final RandomAccessibleInterval<S> psf;

	/*
	 * The constructor should not be called directly. This object should be built
	 *  instead built with the builder
	 */
	protected Clij2RichardsonLucyImglib2Cache(
			final RandomAccessibleInterval<S> source,
			final CLIJ2Pool pool,
			final RandomAccessibleInterval<S> psf,
			final long[] overlap,
			final int numberOfIterations,
			final float regularizationFactor,
			final boolean nonCirculant) {

		this.source = source;
		this.clij2Pool = pool;
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
	
	public void setUpStatus(StatusService status, int total) {
		this.status = status;
		this.total = total;
		this.current = 0;
	}

	/**
	 * Implement an accept function that
	 * <p>
	 * a) extracts the Interval defined by 'cell' and overlap from the source
	 *  Note: this function only adds overlap between cells, but does not pad out of bounds (that is left to filter implementation)
	 * <p>
	 * b) applies the clij filter
	 * <p>
	 * c) writes the result to the Interval defined by 'cell' (does not write the padded area)
	 *
	 * @param cell The RandomAccessibleInterval representing the cell.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void accept(final RandomAccessibleInterval<T> cell) {
		try {
			if (this.status != null) {
				this.status.showStatus(current, total, "deconvolving cell " + current + " of " + total );
				current = current + 1;
			}

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
				System.out.println("min/max source " + source.realMin(d) + " " + source.realMax(d));
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

			// get the input RAI (using the min and max interval computed above)
			RandomAccessibleInterval<S> inputRAI = Views.interval(source, mine, maxe);

			// Getting one CLIJ2 instance
			// - if one is available, gets one instantly, and lock it while it is not recycled
			// - if none is available:
			//   - or waits for one to be available (Thread will park, it's not busy waiting)
			CLIJ2 clij2 = clij2Pool.acquire();
			// Using GPU named clij2.getGPUName()

			// convert input RAI to ClearCLBuffer
			final ClearCLBuffer input = clij2.push(Views.zeroMin(inputRAI));

			// create temporary buffer for output
			final ClearCLBuffer output = clij2.create(input);

			if (!psfPushed.containsKey(clij2)) {
				psfPushed.put(clij2, clij2.push(psf)); // Push one psf buffer per CLIJ2 only
			}

			DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, input, psfPushed.get(clij2), output, numberOfIterations, regularizationFactor, nonCirculant);

			// convert CLBuffer result to RAI
			// at this point the result contains the padded area
			final ClearCLBufferToRandomAccessibleIntervalConverter cl2rai = new ClearCLBufferToRandomAccessibleIntervalConverter();
			cl2rai.setCLIJ(clij2.getCLIJ());
			final RandomAccessibleInterval<T> result = cl2rai.convert(output);

			input.close();
			output.close();

			// Set clij2 instance as available again
			clij2Pool.recycle(clij2);

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
	 * Builder for constructing a deconvolver. The arguments are the compulsory ones.
	 * @param source the 3D (xyz) RAI to deconvolve
	 * @param psf the 3D (xyz) RAI of the point spread function
	 * @return builder for constructing the deconvolver
	 * @param <T>
	 * @param <S>
	 */
	public static <T extends RealType<T> & NativeType<T>, S extends RealType<S>> Builder<T,S> builder(
			RandomAccessibleInterval<S> source,
			RandomAccessibleInterval<S> psf
	) {
		return new Builder<>(source, psf);
	}

	public static class Builder<T extends RealType<T> & NativeType<T>, S extends RealType<S>> {
		// Compulsory
		final private RandomAccessibleInterval<S> source;
		final private RandomAccessibleInterval<S> psf;

		// Optional
		private CLIJ2Pool pool = null;
		private long[] overlap = new long[]{10,10,10};
		private int numberOfIterations = 100;
		private float regularizationFactor = 0.0f;
		private boolean nonCirculant = true;

		protected Builder(RandomAccessibleInterval<S> source, RandomAccessibleInterval<S> psf) {
			this.source = source;
			this.psf = psf;
		}

		/**
		 * Specify the set of gpus that can be used for deconvolving tiles.
		 * The same gpu name can be used to add multiple CLIJ2 instance per GPU
		 * for instance .useGPUs("RTX","RTX","Intel")
		 * <p>
		 * The method useGPUPool can be used instead to specify directly the pool
		 * @param gpuId the list of gpu (can be a single one) to be used for deconvolution task
		 * @return builder
		 */
		public Builder<T,S> useGPU(String... gpuId) {
			if (this.pool!=null) {
				System.err.println("The gpu pool was already defined and will be overridden");
			}
			pool = new CLIJ2Pool(gpuId);
			return this;
		}

		/**
		 * Specify the GPU pool to be used for deconvolution.
		 * This is handy when the same pool has to be shared
		 * between different deconvolution tasks
		 * <p>
		 * The method useGPU is an alternative is you don't want the builder to create the
		 * @param pool the pool of CLIJ2 instances that can be used for deconvolution tasks
		 * @return builder
		 */
		public Builder<T,S> useGPUPool(CLIJ2Pool pool) {
			if (this.pool!=null) {
				System.err.println("The gpu pool was already defined and will be overridden");
			}
			this.pool = pool;
			return this;
		}

		/**
		 * overlap between deconvolution tiles, ordered x, y, z
		 * @param overlap in pixel unit
		 * @return builder
		 */
		public Builder<T,S> overlap(long... overlap) {
			this.overlap = overlap;
			return this;
		}

		/**
		 * overlap between deconvolution tiles, same value for all dimensions
		 * @param overlap in pixel unit
		 * @return builder
		 */
		public Builder<T,S> overlap(long overlap) {
			this.overlap = new long[]{overlap};
			return this;
		}

		/**
		 * @param numberOfIterations for deconvolution, typically 100
		 * @return builder
		 */
		public Builder<T,S> numberOfIterations(int numberOfIterations) {
			this.numberOfIterations = numberOfIterations;
			return this;
		}

		/**
		 * TODO explain
		 * @param regularizationFactor
		 * @return builder
		 */
		public Builder<T,S> regularizationFactor(float regularizationFactor) {
			this.regularizationFactor = regularizationFactor;
			return this;
		}

		/**
		 * TODO explain
		 * @param nonCirculant
		 * @return builder
		 */
		public Builder<T,S> nonCirculant(boolean nonCirculant) {
			this.nonCirculant = nonCirculant;
			return this;
		}

		/**
		 * builds the deconvovolver
		 * @return Clij2RichardsonLucyImglib2Cache instance
		 */
		public Clij2RichardsonLucyImglib2Cache<T,S> build() {
			if (pool == null) pool = new CLIJ2Pool();

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
