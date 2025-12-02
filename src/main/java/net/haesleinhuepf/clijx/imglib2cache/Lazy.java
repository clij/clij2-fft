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
 * Provides convenience methods to create lazily evaluated, cached cell images
 * using {@link CellLoader} or {@link Consumer} for cell generation.
 * This is useful for large images where only parts of the image are accessed at a time,
 * allowing efficient memory usage and on-demand computation.
 */
public interface Lazy {

	/**
	 * Creates a memory-backed {@link CachedCellImg} with a custom cell {@link Cache}.
	 * This method is typically used for advanced or specialized use cases.
	 *
	 * @param <T> the pixel type of the image
	 * @param grid the cell grid defining the partitioning of the image
	 * @param cache the cache to store loaded cells
	 * @param type the pixel type of the image
	 * @param accessFlags flags to control memory access behavior
	 * @return a new {@link CachedCellImg} instance, or null if the type is not supported
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
	 * Creates a memory-backed {@link CachedCellImg} with a {@link CellLoader}.
	 * The loader is responsible for loading or computing the content of each cell.
	 *
	 * @param <T> the pixel type of the image
	 * @param targetInterval the interval defining the dimensions of the image
	 * @param blockSize the size of each cell block
	 * @param type the pixel type of the image
	 * @param accessFlags flags to control memory access behavior
	 * @param loader the loader to generate or load cell content
	 * @return a new {@link CachedCellImg} instance
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
	 * Creates a memory-backed {@link CachedCellImg} with a cell generator implemented
	 * as a {@link Consumer}. This is the most general-purpose method for creating
	 * lazily evaluated images. The consumer is called to generate the content of each cell.
	 *
	 * @param <T> the pixel type of the image
	 * @param targetInterval the interval defining the dimensions of the image
	 * @param blockSize the size of each cell block
	 * @param type the pixel type of the image
	 * @param accessFlags flags to control memory access behavior
	 * @param op the consumer that generates the content of each cell
	 * @return a new {@link CachedCellImg} instance
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

