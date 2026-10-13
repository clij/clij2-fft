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

package net.haesleinhuepf.clijx.tests;

import static org.junit.Assert.assertEquals;

import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.plugins.OpenCLFFTUtility;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.array.ArrayImg;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.array.FloatArray;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.view.Views;

/**
 * Checks that {@link OpenCLFFTUtility#padShiftFFTKernel} moves every pixel of the kernel
 * to its place in the extended kernel, with the kernel center (index size/2) at index 0,
 * and leaves every other pixel at 0. Needs an OpenCL device.
 */
public class PadShiftFFTKernelTest {

	private static CLIJ2 clij2;

	@BeforeClass
	public static void getCLIJ2() {
		// skip the tests on machines without OpenCL, e.g. the CI runners
		try {
			clij2 = CLIJ2.getInstance();
		}
		catch (Throwable t) {
			Assume.assumeNoException("No OpenCL device available", t);
		}
	}

	@AfterClass
	public static void releaseCLIJ2() {
		if (clij2 != null) {
			clij2.clear();
		}
	}

	@Test
	public void testOddKernel3D() {
		check(new long[] { 5, 7, 3 }, new long[] { 16, 16, 8 });
	}

	@Test
	public void testEvenKernel3D() {
		check(new long[] { 4, 6, 2 }, new long[] { 16, 16, 8 });
	}

	@Test
	public void testMixedKernelOddExtendedSize3D() {
		check(new long[] { 5, 4, 3 }, new long[] { 15, 12, 9 });
	}

	@Test
	public void testSize1Kernel3D() {
		check(new long[] { 5, 1, 1 }, new long[] { 16, 8, 8 });
	}

	@Test
	public void testOddKernel2D() {
		check(new long[] { 7, 5 }, new long[] { 16, 15 });
	}

	@Test
	public void testEvenKernel2D() {
		check(new long[] { 6, 4 }, new long[] { 16, 16 });
	}

	private static void check(long[] kernelSize, long[] extendedSize) {
		int n = kernelSize.length;

		// every kernel pixel gets its own, non-zero value
		ArrayImg<FloatType, FloatArray> kernel = ArrayImgs.floats(kernelSize);
		float value = 1;
		for (FloatType t : kernel) {
			t.set(value++);
		}

		ClearCLBuffer gpuKernel = clij2.push(kernel);
		ClearCLBuffer gpuExtended = clij2.create(extendedSize, NativeTypeEnum.Float);
		// fill with garbage, padShiftFFTKernel must clear it
		clij2.set(gpuExtended, -1);

		OpenCLFFTUtility.padShiftFFTKernel(clij2, gpuKernel, gpuExtended);

		RandomAccessibleInterval<? extends RealType<?>> extended = clij2.pullRAI(gpuExtended);
		gpuKernel.close();
		gpuExtended.close();

		// expected extended kernel
		ArrayImg<FloatType, FloatArray> expected = ArrayImgs.floats(extendedSize);
		RandomAccess<FloatType> expectedAccess = expected.randomAccess();
		Cursor<FloatType> kernelCursor = kernel.localizingCursor();
		long[] position = new long[n];
		while (kernelCursor.hasNext()) {
			kernelCursor.fwd();
			for (int d = 0; d < n; d++) {
				long shifted = kernelCursor.getLongPosition(d) - kernelSize[d] / 2;
				position[d] = ((shifted % extendedSize[d]) + extendedSize[d]) % extendedSize[d];
			}
			expectedAccess.setPosition(position);
			expectedAccess.get().set(kernelCursor.get());
		}

		Cursor<FloatType> expectedCursor = expected.localizingCursor();
		RandomAccess<? extends RealType<?>> extendedAccess = extended.randomAccess();
		while (expectedCursor.hasNext()) {
			expectedCursor.fwd();
			extendedAccess.setPosition(expectedCursor);
			assertEquals("pixel " + java.util.Arrays.toString(expectedCursor.positionAsLongArray()),
					expectedCursor.get().getRealDouble(), extendedAccess.get().getRealDouble(), 0);
		}
	}
}
