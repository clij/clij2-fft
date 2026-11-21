package net.haesleinhuepf.clijx.imglib2cache;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import net.haesleinhuepf.clijx.imglib2cache.CellPlanner.Plan;

public class CellPlannerTest {

	private static final long GB = 1L << 30;
	// RTX 3090 as OpenCL reports it
	private static final long GPU = 24 * GB, MAX_ALLOC = 6 * GB;
	private static final long[] OVERLAP = { 10, 10, 10 };
	// the measured LLS7 PSF, x y z
	private static final long[] LATTICE_PSF = { 45, 68, 128 };

	@Test
	public void nextSmooth() {
		assertEquals(1, CellPlanner.nextSmooth(1));
		assertEquals(12, CellPlanner.nextSmooth(11));
		assertEquals(14, CellPlanner.nextSmooth(13));
		assertEquals(1024, CellPlanner.nextSmooth(1024));
		assertEquals(1080, CellPlanner.nextSmooth(1069));
	}

	@Test
	public void smallImageNeedsNoCells() {
		Plan p = CellPlanner.plan(new long[] { 256, 256, 64 }, new long[] { 33, 33, 33 },
			OVERLAP, GPU, MAX_ALLOC);
		assertTrue(p.feasible);
		assertFalse(p.needsCells());
		assertArrayEquals(new long[] { 1, 1, 1 }, p.divisions);
	}

	@Test
	public void latticeChannelIsSplitAndEveryCellFits() {
		Plan p = CellPlanner.plan(new long[] { 1024, 450, 2001 }, LATTICE_PSF, OVERLAP,
			GPU, MAX_ALLOC);
		assertTrue(p.feasible);
		assertTrue(p.needsCells());
		assertTrue(p.bytesWholeImage > p.budgetBytes);
		assertTrue(p.bytesPerCell <= p.budgetBytes);
		long[] c = p.largestPaddedCell;
		assertTrue(8.0 * (c[0] / 2 + 1) * c[1] * c[2] <= MAX_ALLOC);
		// the cells cover the image
		for (int d = 0; d < 3; d++) {
			assertTrue(p.cellSize[d] * p.divisions[d] >= p.imageSize[d]);
			assertTrue(p.cellSize[d] * (p.divisions[d] - 1) < p.imageSize[d]);
		}
	}

	@Test
	public void fewerCellsThanTheOldGuiDefault() {
		// the GUI default of 256 x 256 x 128 gives 4 x 2 x 16 = 128 cells for this image
		Plan p = CellPlanner.plan(new long[] { 1024, 450, 2001 }, LATTICE_PSF, OVERLAP,
			GPU, MAX_ALLOC);
		assertTrue(p.numberOfCells() < 128);
	}

	@Test
	public void prefersNotSplittingZWhenXYSplitsFit() {
		// tall in z, PSF long in z: an xy split fits, so z should stay whole
		Plan p = CellPlanner.plan(new long[] { 2048, 2048, 200 }, new long[] { 32, 32, 64 },
			OVERLAP, GPU, MAX_ALLOC);
		assertTrue(p.feasible);
		assertTrue(p.needsCells());
		assertEquals(1, p.divisions[2]);
	}

	@Test
	public void infeasibleWhenNothingFits() {
		Plan p = CellPlanner.plan(new long[] { 512, 512, 512 }, new long[] { 300, 300, 300 },
			OVERLAP, 64L << 20, 16L << 20);
		assertFalse(p.feasible);
		assertTrue(p.describe().contains("No cell size fits"));
	}
}
