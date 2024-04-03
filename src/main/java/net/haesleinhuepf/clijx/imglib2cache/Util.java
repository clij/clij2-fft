package net.haesleinhuepf.clijx.imglib2cache;


import ij.process.FloatProcessor;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Some useful methods that do not fit elsewhere.
 *
 * @author Stephan Saalfeld
 */
public interface Util {

	/**
	 * Copy the contents of an source {@link RandomAccessible} in an
	 * interval defined by and target {@link RandomAccessibleInterval}
	 * into that target {@link RandomAccessibleInterval}.
	 *
	 * @param <T>
	 * @param source
	 * @param target
	 */
	public static <T extends RealType<T>, S extends RealType<S>> void copyReal(
			final RandomAccessible<? extends T> source,
			final RandomAccessibleInterval<? extends S> target) {

		Views.flatIterable(Views.interval(Views.pair(source, target), target)).forEach(
				pair -> pair.getB().setReal(pair.getA().getRealDouble()));
	}

}
