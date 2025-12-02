package net.haesleinhuepf.clijx.imglib2cache;

import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.type.numeric.RealType;
import net.imglib2.view.Views;

/**
 * Utility interface providing helper methods for common operations on
 * {@link RandomAccessible} and {@link RandomAccessibleInterval} objects.
 */
public interface Util {

	/**
	 * Copies the real-valued contents of a source {@link RandomAccessible} within the interval
	 * defined by a target {@link RandomAccessibleInterval} into the target.
	 * The source and target must have the same dimensions and compatible pixel types.
	 *
	 * @param <T> the pixel type of the source
	 * @param <S> the pixel type of the target
	 * @param source the source image or data structure
	 * @param target the target image or data structure, defining the interval to copy
	 */
	static <T extends RealType<T>, S extends RealType<S>> void copyReal(
			final RandomAccessible<? extends T> source,
			final RandomAccessibleInterval<? extends S> target) {

		Views.flatIterable(Views.interval(Views.pair(source, target), target)).forEach(
				pair -> pair.getB().setReal(pair.getA().getRealDouble()));
	}

}
