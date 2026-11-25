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

package net.haesleinhuepf.clijx.imglib2cache;

import java.util.Locale;

import net.haesleinhuepf.clijx.plugins.FFTSizes;

/**
 * Chooses the cell (tile) size for non-circulant Richardson-Lucy so each cell fits
 * the GPU, while doing as little extra work on overlap and padding as possible.
 * <p>
 * Pure arithmetic: no GPU and no GUI, so it can be unit tested and used for
 * reporting. The sizing rule (see docs/design/nd-deconvolution.md) is the one the
 * Python package should share:
 * <ul>
 * <li>padded length per axis: {@link FFTSizes#next(long)} of
 * {@code piece + overlap on sides with a neighbor + psf}, as the padding code pads</li>
 * <li>GPU memory per cell: {@code memoryFactor x 4 bytes x padded voxels}, within
 * {@code budgetFraction} of the device's global memory</li>
 * <li>the largest buffer, a complex spectrum of {@code 8 x (N0/2+1) x N1 x N2}
 * bytes, within the device's maximum allocation</li>
 * <li>among splits that fit, the least total padded volume, with splitting z
 * costing {@code zSplitPenalty} extra, since PSFs are usually long in z</li>
 * </ul>
 * Cells follow the grid {@code Lazy.generate} builds: cell size
 * {@code ceil(size / n)}, the last cell taking the remainder.
 */
public final class CellPlanner {

	/** GPU copies of the padded volume the RL needs, with a small margin. */
	public static final double DEFAULT_MEMORY_FACTOR = 12;
	/** Fraction of the device's global memory to plan for. */
	public static final double DEFAULT_BUDGET_FRACTION = 0.8;
	/** Extra cost, as a fraction, for plans that split z. */
	public static final double DEFAULT_Z_SPLIT_PENALTY = 0.25;
	/** Cells smaller than this along an axis are not considered. */
	public static final long MIN_CELL = 16;

	private CellPlanner() {}

	/** The result: the chosen cells, or why none fit. */
	public static final class Plan {
		public final boolean feasible;
		public final long[] imageSize;
		/** Cell size per axis, as passed to {@code Lazy.generate}. */
		public final long[] cellSize;
		/** Number of cells per axis. */
		public final long[] divisions;
		/** Padded size of the largest cell, per axis. */
		public final long[] largestPaddedCell;
		/** Estimated GPU memory for the largest cell. */
		public final double bytesPerCell;
		/** Total padded voxels over all cells, divided by image voxels, minus one. */
		public final double extraWork;
		/** Estimated GPU memory to deconvolve the whole image as one cell. */
		public final double bytesWholeImage;
		public final double budgetBytes;
		public final long maxAllocationBytes;

		Plan(boolean feasible, long[] imageSize, long[] cellSize, long[] divisions,
			long[] largestPaddedCell, double bytesPerCell, double extraWork,
			double bytesWholeImage, double budgetBytes, long maxAllocationBytes)
		{
			this.feasible = feasible;
			this.imageSize = imageSize;
			this.cellSize = cellSize;
			this.divisions = divisions;
			this.largestPaddedCell = largestPaddedCell;
			this.bytesPerCell = bytesPerCell;
			this.extraWork = extraWork;
			this.bytesWholeImage = bytesWholeImage;
			this.budgetBytes = budgetBytes;
			this.maxAllocationBytes = maxAllocationBytes;
		}

		public long numberOfCells() {
			long n = 1;
			for (long d : divisions) n *= d;
			return n;
		}

		public boolean needsCells() {
			return numberOfCells() > 1;
		}

		/** A few lines for the GUI log. */
		public String describe() {
			StringBuilder s = new StringBuilder();
			s.append(String.format(Locale.ROOT,
				"Whole volume needs ~%s of GPU memory (budget %s, largest buffer allowed %s)",
				gb(bytesWholeImage), gb(budgetBytes), gb(maxAllocationBytes)));
			if (!feasible) {
				s.append("\nNo cell size fits: even ").append(MIN_CELL)
					.append("-pixel cells are too large for this GPU with this PSF.");
				return s.toString();
			}
			if (!needsCells()) {
				s.append("\nFits in one piece; no cells needed.");
				return s.toString();
			}
			s.append(String.format(Locale.ROOT,
				"%n%d cells of %s (x, y, z), ~%s each, ~%.0f%% extra work for overlap and padding",
				numberOfCells(), xyz(cellSize), gb(bytesPerCell), 100 * extraWork));
			if (divisions.length > 2 && divisions[2] > 1) {
				s.append("\nSplits z: check for seams between z cells.");
			}
			return s.toString();
		}

		private static String xyz(long[] v) {
			StringBuilder s = new StringBuilder();
			for (int i = 0; i < v.length; i++) s.append(i == 0 ? "" : " x ").append(v[i]);
			return s.toString();
		}

		private static String gb(double bytes) {
			return String.format(Locale.ROOT, "%.1f GB", bytes / (1L << 30));
		}
	}

	/** Plans with the default memory factor, budget and z penalty. */
	public static Plan plan(long[] imageSize, long[] psfSize, long[] overlap,
		long gpuMemoryBytes, long maxAllocationBytes)
	{
		return plan(imageSize, psfSize, overlap, gpuMemoryBytes, maxAllocationBytes,
			DEFAULT_MEMORY_FACTOR, DEFAULT_BUDGET_FRACTION, DEFAULT_Z_SPLIT_PENALTY);
	}

	public static Plan plan(long[] imageSize, long[] psfSize, long[] overlap,
		long gpuMemoryBytes, long maxAllocationBytes, double memoryFactor,
		double budgetFraction, double zSplitPenalty)
	{
		int n = imageSize.length;
		if (n != 3 || psfSize.length != 3 || overlap.length != 3) {
			throw new IllegalArgumentException("CellPlanner expects 3D (x, y, z) sizes");
		}
		double budget = budgetFraction * gpuMemoryBytes;
		double imageVoxels = (double) imageSize[0] * imageSize[1] * imageSize[2];

		long[] whole = new long[3];
		for (int d = 0; d < 3; d++) whole[d] = paddedLength(imageSize[d], 0, psfSize[d]);
		double bytesWhole = memoryFactor * 4 * volume(whole);

		// each axis is independent: work out its pieces once per candidate count
		AxisPieces[][] axes = new AxisPieces[3][];
		for (int d = 0; d < 3; d++) {
			long[] counts = divisionsToTry(imageSize[d]);
			axes[d] = new AxisPieces[counts.length];
			for (int i = 0; i < counts.length; i++) {
				axes[d][i] = axis(imageSize[d], cellSize(imageSize[d], counts[i]), overlap[d],
					psfSize[d]);
			}
		}

		Plan best = null;
		double bestCost = Double.POSITIVE_INFINITY;
		for (AxisPieces ax : axes[0]) for (AxisPieces ay : axes[1]) for (AxisPieces az : axes[2]) {
			long[] cell = { ax.cell, ay.cell, az.cell };
			long[] largest = { ax.largestPadded, ay.largestPadded, az.largestPadded };
			long[] divisions = { ax.count, ay.count, az.count };
			double totalPadded = (double) ax.sumPadded * ay.sumPadded * az.sumPadded;
			double bytesPerCell = memoryFactor * 4 * volume(largest);
			double largestBuffer = 8.0 * (largest[0] / 2 + 1) * largest[1] * largest[2];
			if (bytesPerCell > budget || largestBuffer > maxAllocationBytes) continue;

			double cost = totalPadded * (divisions[2] > 1 ? 1 + zSplitPenalty : 1);
			if (cost < bestCost) {
				bestCost = cost;
				best = new Plan(true, imageSize.clone(), cell, divisions, largest,
					bytesPerCell, totalPadded / imageVoxels - 1, bytesWhole, budget,
					maxAllocationBytes);
			}
		}
		if (best != null) return best;
		return new Plan(false, imageSize.clone(), imageSize.clone(), new long[] { 1, 1, 1 },
			whole, bytesWhole, 0, bytesWhole, budget, maxAllocationBytes);
	}

	/** Smallest number of the form 2^a 3^b 5^c 7^d that is at least {@code n}. */
	public static long nextSmooth(long n) {
		if (n <= 1) return 1;
		for (long m = n;; m++) {
			long r = m;
			for (long p : new long[] { 2, 3, 5, 7 }) while (r % p == 0) r /= p;
			if (r == 1) return m;
		}
	}

	/** Padded FFT length of one piece, for non-circulant RL: what the padding code pads to. */
	static long paddedLength(long piece, long overlapTotal, long psf) {
		return FFTSizes.next(piece + overlapTotal + psf);
	}

	/** The cell size {@code Lazy.generate} gets for {@code n} cells along an axis. */
	static long cellSize(long size, long n) {
		return (size + n - 1) / n;
	}

	private static final class AxisPieces {
		long cell, count, largestPadded, sumPadded;
	}

	/** The pieces along one axis: count, largest padded length, sum of padded lengths. */
	private static AxisPieces axis(long size, long cell, long overlap, long psf) {
		AxisPieces a = new AxisPieces();
		a.cell = cell;
		for (long start = 0; start < size; start += cell) {
			long piece = Math.min(cell, size - start);
			long left = Math.min(overlap, start);
			long right = Math.min(overlap, size - (start + piece));
			long padded = paddedLength(piece, left + right, psf);
			a.count++;
			a.largestPadded = Math.max(a.largestPadded, padded);
			a.sumPadded += padded;
		}
		return a;
	}

	/** Numbers of cells worth trying along an axis: each distinct cell size, down to {@link #MIN_CELL}. */
	private static long[] divisionsToTry(long size) {
		java.util.TreeSet<Long> counts = new java.util.TreeSet<>();
		long maxN = Math.max(1, size / MIN_CELL);
		long previousCell = -1;
		for (long n = 1; n <= maxN; n++) {
			long cell = cellSize(size, n);
			if (cell != previousCell) counts.add((size + cell - 1) / cell);
			previousCell = cell;
		}
		long[] out = new long[counts.size()];
		int i = 0;
		for (long c : counts) out[i++] = c;
		return out;
	}

	private static double volume(long[] v) {
		return (double) v[0] * v[1] * v[2];
	}

	/** Prints plans for given sizes: {@code x y z psfX psfY psfZ [overlap] [gpuGB maxAllocGB]}. */
	public static void main(String[] args) {
		long[] img = { Long.parseLong(args[0]), Long.parseLong(args[1]), Long.parseLong(args[2]) };
		long[] psf = { Long.parseLong(args[3]), Long.parseLong(args[4]), Long.parseLong(args[5]) };
		long ov = args.length > 6 ? Long.parseLong(args[6]) : 10;
		double gpuGB = args.length > 7 ? Double.parseDouble(args[7]) : 24;
		double maxGB = args.length > 8 ? Double.parseDouble(args[8]) : 6;
		Plan p = plan(img, psf, new long[] { ov, ov, ov }, (long) (gpuGB * (1L << 30)),
			(long) (maxGB * (1L << 30)));
		System.out.println(p.describe());
	}
}
