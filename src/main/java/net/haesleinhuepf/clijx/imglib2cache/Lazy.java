package net.haesleinhuepf.clijx.imglib2cache;


import static net.imglib2.type.PrimitiveType.BYTE;
import static net.imglib2.type.PrimitiveType.DOUBLE;
import static net.imglib2.type.PrimitiveType.FLOAT;
import static net.imglib2.type.PrimitiveType.INT;
import static net.imglib2.type.PrimitiveType.LONG;
import static net.imglib2.type.PrimitiveType.SHORT;

import java.util.Set;
import java.util.function.Consumer;

import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.Cache;
import net.imglib2.cache.img.CachedCellImg;
import net.imglib2.cache.img.CellLoader;
import net.imglib2.cache.img.LoadedCellCacheLoader;
import net.imglib2.cache.ref.SoftRefLoaderCache;
import net.imglib2.img.basictypeaccess.AccessFlags;
import net.imglib2.img.basictypeaccess.ArrayDataAccessFactory;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.integer.GenericByteType;
import net.imglib2.type.numeric.integer.GenericIntType;
import net.imglib2.type.numeric.integer.GenericLongType;
import net.imglib2.type.numeric.integer.GenericShortType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Intervals;

/**
 * Convenience methods to create lazy evaluated cached cell images with ops or consumers.
 *
 * @author Stephan Saalfeld
 */
public interface Lazy {

	/**
	 * Create a memory {@link CachedCellImg} with a cell {@link Cache}.  Unless
	 * you are doing something special, you will likely not use this method.
	 *
	 * @param grid
	 * @param cache
	 * @param type
	 * @param accessFlags
	 * @return
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <T extends NativeType<T>> CachedCellImg<T, ?> createImg(
			final CellGrid grid,
			final Cache<Long, Cell<?>> cache,
			final T type,
			final Set<AccessFlags> accessFlags) {

		final CachedCellImg<T, ?> img;

		if (GenericByteType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(BYTE, accessFlags));
		} else if (GenericShortType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(SHORT, accessFlags));
		} else if (GenericIntType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(INT, accessFlags));
		} else if (GenericLongType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(LONG, accessFlags));
		} else if (FloatType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(FLOAT, accessFlags));
		} else if (DoubleType.class.isInstance(type)) {
			img = new CachedCellImg(grid, type, cache, ArrayDataAccessFactory.get(DOUBLE, accessFlags));
		} else {
			img = null;
		}
		return img;
	}

	/**
	 * Create a memory {@link CachedCellImg} with a {@link CellLoader}.
	 * Unless you are doing something special, you will likely not use this
	 * method.
	 *
	 * @param targetInterval
	 * @param blockSize
	 * @param type
	 * @param accessFlags
	 * @param loader
	 * @return
	 */
	public static <T extends NativeType<T>> CachedCellImg<T, ?> createImg(
			final Interval targetInterval,
			final int[] blockSize,
			final T type,
			final Set<AccessFlags> accessFlags,
			final CellLoader<T> loader) {

		final long[] dimensions = Intervals.dimensionsAsLongArray(targetInterval);
		final CellGrid grid = new CellGrid(dimensions, blockSize);

		@SuppressWarnings({"unchecked", "rawtypes"})
		final Cache<Long, Cell<?>> cache =
				new SoftRefLoaderCache().withLoader(LoadedCellCacheLoader.get(grid, loader, type, accessFlags));

		return createImg(grid, cache, type, accessFlags);
	}

	/**
	 * Create a memory {@link CachedCellImg} with a cell generator implemented
	 * as a {@link Consumer}.  This is the most general purpose method for
	 * anything new.  Note that any inputs are managed by the cell generator,
	 * not but this method.
	 *
	 * @param targetInterval
	 * @param blockSize
	 * @param type
	 * @param accessFlags
	 * @param op
	 * @return
	 */
	public static <T extends NativeType<T>> CachedCellImg<T, ?> generate(
			final Interval targetInterval,
			final int[] blockSize,
			final T type,
			final Set<AccessFlags> accessFlags,
			final Consumer<RandomAccessibleInterval<T>> op) {

		return createImg(
				targetInterval,
				blockSize,
				type,
				accessFlags,
				op::accept);
	}
}

