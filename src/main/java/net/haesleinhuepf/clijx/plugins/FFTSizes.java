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

package net.haesleinhuepf.clijx.plugins;

import java.util.Arrays;

/**
 * Lengths an axis is padded to before the FFT. The planner and the padding code both
 * use {@link #next(long)}, so a plan and a run agree on every padded size.
 */
public final class FFTSizes {

	private FFTSizes() {}

	/** FFT lengths, ascending. */
	private static final long[] SIZES = {
		1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 18, 20,
		21, 24, 25, 27, 28, 30, 32, 35, 36, 40, 42, 45, 48, 49, 50, 54,
		56, 60, 63, 64, 70, 72, 75, 80, 81, 84, 90, 96, 98, 100, 108, 112,
		120, 125, 126, 128, 135, 140, 144, 147, 150, 160, 162, 168, 175, 180, 189, 192,
		196, 200, 216, 224, 225, 240, 243, 245, 250, 252, 256, 270, 280, 288, 294, 300,
		320, 324, 336, 343, 350, 360, 375, 378, 384, 392, 400, 405, 432, 441, 448, 450,
		480, 486, 490, 500, 504, 512, 540, 560, 567, 576, 588, 600, 625, 640, 648, 672,
		675, 686, 700, 720, 729, 750, 756, 768, 784, 800, 810, 864, 875, 882, 896, 900,
		960, 972, 980, 1000, 1008, 1024, 1029, 1080, 1120, 1125, 1134, 1152, 1176, 1200, 1215, 1225,
		1250, 1280, 1296, 1323, 1344, 1350, 1372, 1400, 1440, 1458, 1500, 1512, 1536, 1568, 1600, 1620,
		1701, 1715, 1728, 1750, 1764, 1792, 1800, 1875, 1920, 1944, 1960, 2000, 2016, 2025, 2048, 2058,
		2160, 2187, 2240, 2250, 2268, 2304, 2352, 2400, 2401, 2430, 2450, 2500, 2560, 2592, 2646, 2688,
		2700, 2744, 2800, 2880, 2916, 3000, 3024, 3072, 3087, 3125, 3136, 3200, 3240, 3375, 3402, 3430,
		3456, 3500, 3528, 3584, 3600, 3645, 3750, 3840, 3888, 3920, 3969, 4000, 4032, 4050, 4096, 4116,
		4320, 4374, 4375, 4480, 4500, 4536, 4608, 4704, 4800, 4802, 4860, 4900, 5000, 5103, 5120, 5184,
		5292, 5376, 5400, 5488, 5600, 5625, 5760, 5832, 6000, 6048, 6075, 6125, 6144, 6174, 6250, 6272,
		6400, 6480, 6561, 6750, 6804, 6860, 6912, 7000, 7056, 7168, 7200, 7203, 7290, 7500, 7680, 7776,
		7840, 7938, 8000, 8064, 8100, 8192, 8232, 8575, 8640, 8748, 8750, 8960, 9000, 9072, 9216, 9261,
		9375, 9408, 9600, 9604, 9720, 9800, 10000, 10125, 10206, 10240, 10368, 10584, 10752, 10800, 10935, 10976,
		11200, 11250, 11520, 11664, 11907, 12000, 12005, 12096, 12150, 12250, 12288, 12348, 12500, 12544, 12800, 12960,
		13122, 13500, 13608, 13720, 13824, 14000, 14112, 14336, 14400, 14406, 14580, 15000, 15309, 15360, 15552, 15625,
		15680, 15876, 16000, 16128, 16200, 16384,
	};

	/**
	 * The smallest length from the table that is at least {@code n}; beyond the table,
	 * the smallest number of the form 2^a 3^b 5^c 7^d that is at least {@code n}.
	 */
	public static long next(long n) {
		if (n <= 1) return 1;
		int i = Arrays.binarySearch(SIZES, n);
		if (i >= 0) return SIZES[i];
		int insertion = -i - 1;
		if (insertion < SIZES.length) return SIZES[insertion];
		for (long m = n;; m++) {
			long r = m;
			for (long p : new long[] { 2, 3, 5, 7 }) while (r % p == 0) r /= p;
			if (r == 1) return m;
		}
	}

	/** {@link #next(long)} for each axis. */
	public static long[] next(long[] lengths) {
		long[] out = new long[lengths.length];
		for (int d = 0; d < lengths.length; d++) out[d] = next(lengths[d]);
		return out;
	}
}
