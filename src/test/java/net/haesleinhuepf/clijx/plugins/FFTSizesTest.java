package net.haesleinhuepf.clijx.plugins;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class FFTSizesTest {

	// past the end of the table, so the fallback is covered too
	private static final long LIMIT = 20000;

	private static long withoutFactors2357(long n) {
		for (long p : new long[] { 2, 3, 5, 7 }) while (n % p == 0) n /= p;
		return n;
	}

	@Test
	public void nextIsAtLeastNAndOnlyHasFactors2357() {
		for (long n = 1; n <= LIMIT; n++) {
			long m = FFTSizes.next(n);
			assertTrue("next(" + n + ") = " + m, m >= n);
			assertEquals("next(" + n + ") = " + m, 1, withoutFactors2357(m));
			assertEquals("a padded length is kept", m, FFTSizes.next(m));
		}
	}

	@Test
	public void nextNeverDecreases() {
		for (long n = 1; n < LIMIT; n++) {
			assertTrue("at " + n, FFTSizes.next(n) <= FFTSizes.next(n + 1));
		}
	}
}
