package net.haesleinhuepf.clijx.imglib2cache;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.scijava.app.StatusService;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.converters.implementations.ClearCLBufferToRandomAccessibleIntervalConverter;
import net.haesleinhuepf.clij.converters.implementations.RandomAccessibleIntervalToClearCLBufferConverter;
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
 * 
 * It is designed to be used as an input to the {@link Lazy} class
 * 
 * @param <T>
 * @param <S>
 */
public class Clij2RichardsonLucyImglib2Cache<T extends RealType<T> & NativeType<T>, S extends RealType<S>> implements Consumer<RandomAccessibleInterval<T>>{

	protected final RandomAccessibleInterval<S> source;
	protected final long[] overlap;
	protected final CLIJ2 clij2;
	protected final ClearCLBuffer psf;
	protected BiConsumer<ClearCLBuffer, ClearCLBuffer> filter = (a, b) -> {};

	public CLIJ2 getClij2() { return clij2; }
	
	protected StatusService status = null;
	protected int total = -1;
	protected int current = 0;

	/**
	 * Creates a new clij2OverlapOp instance with the specified parameters.
	 *
	 * @param source The source RandomAccessibleInterval to process.
	 * @param gpuId The ID of the GPU to use for processing.
	 * @param psf The ClearCLBuffer representing the Point Spread Function (PSF).
	 * @param overlap The overlap values for processing, specified as an array of long values.
	 */
	public Clij2RichardsonLucyImglib2Cache(
			final RandomAccessibleInterval<S> source,
			final String gpuId,
			final ClearCLBuffer psf,
			final long... overlap) {

		this.source = source;
		final int n = source.numDimensions();
		if (n == overlap.length)
			this.overlap = overlap;
		else
			this.overlap = Arrays.copyOf(overlap, n);
		clij2 = CLIJ2.getInstance(gpuId);
		
		this.psf = psf;
		this.filter = (a,b) -> DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, a, psf, b, 100, 0.0f, true);
	
	}
	
	public void setUpStatus(StatusService status, int total) {
		this.status = status;
		this.total = total;
		this.current = 0;
	}
	
	public Clij2RichardsonLucyImglib2Cache(
			final RandomAccessibleInterval<S> source,
			final ClearCLBuffer psf,
			final long... overlap) {

		this(source, null, psf, overlap);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	/**
	 * Implement an accept function that 
	 * 
	 * a) extracts the Interval defined by 'cell' and overlap from the source
	 *  Note: this function only adds overlap between cells, but does not pad out of bounds (that is left to filter implementation)
	 *  
	 * b) applies the clij filter
	 * 
	 * c) writes the result to the Interval defined by 'cell' (does not write the padded area)
	 *
	 * @param cell The RandomAccessibleInterval representing the cell.
	 */
	public void accept(final RandomAccessibleInterval<T> cell) {
		
		if (this.status != null) {
			this.status.showStatus(current, total, "deconvolving volume "+current + " of "+total);
			current = current + 1;
		}
		
		final RandomAccessibleIntervalToClearCLBufferConverter rai2cl = new RandomAccessibleIntervalToClearCLBufferConverter();
		rai2cl.setCLIJ(clij2.getCLIJ());
	
		// min and max of the cell cell we are computing values for
		final long[] min = new long[] { cell.min(0), cell.min(1), cell.min(
			2) };
		final long[] max = new long[] { cell.max(0), cell.max(1), cell.max(
			2) };

		// extended min and max when considering overlap
		final long[] mine = new long[cell.numDimensions()];
		final long[] maxe = new long[cell.numDimensions()];

		// overlap size
		long[] overlapmin = new long[cell.numDimensions()];
		long[] overlapmax = new long[cell.numDimensions()];
		
		// if at start or end of the image set overlap size to 0 otherwise use the
		// input overlap
		for (int d = 0; d < cell.numDimensions(); d++) {
			System.out.println("min/max source "+source.realMin(d)+" "+ source.realMax(d));
			System.out.println("min/max cell "+min[d]+" "+max[d]);
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
	
		// convert input RAI to ClearCLBuffer
		final ClearCLBuffer input = rai2cl.convert(Views.zeroMin(inputRAI));
		
		// create temporary buffer for output
		final ClearCLBuffer output = clij2.create(input);

		// call the CLIJ filter
		filter.accept(input, output);
		
		// convert CLBuffer result to RAI
		// at this point the result contains the padded area
		final ClearCLBufferToRandomAccessibleIntervalConverter cl2rai = new ClearCLBufferToRandomAccessibleIntervalConverter();
		cl2rai.setCLIJ(clij2.getCLIJ());
		final RandomAccessibleInterval<T> result = cl2rai.convert(output);
		
		// get the valid part of the extended deconvolution (ie exclude the padded area)
		RandomAccessibleInterval<T> valid = Views.zeroMin(Views.interval(
			result, new FinalInterval(new long[] { overlapmin[0], overlapmin[1], overlapmin[2] },
				new long[] { result.dimension(0) - overlapmax[0] - 1, result.dimension(
					1) - overlapmax[1] - 1, result.dimension(2) -overlapmax[2] - 1 })));

		// copy the extended result to the original cell
		Util.copyReal(valid, Views.zeroMin(cell));

		input.close();
		output.close();
	}

}
