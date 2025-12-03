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

import java.util.ArrayList;

import net.imagej.DatasetService;
import net.imagej.ImgPlus;
import net.imagej.axis.Axes;
import net.imagej.axis.AxisType;
import net.imagej.ops.OpService;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.ComplexType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.Util;
import net.imglib2.view.Views;

public class ImageUtility {

	static public <T extends ComplexType<T> & NativeType<T>> Img<T>
		cropSymmetric(RandomAccessibleInterval<T> in, long[] cropSize, OpService ops)
	{
		long[] min = new long[cropSize.length];
		long[] max= new long[cropSize.length];

		for (int d = 0; d < cropSize.length; d++) {
			min[d] = in.dimension(d) / 2 - cropSize[d] / 2;
			max[d] = min[d]+cropSize[d]-1;
		}

		Interval interval = new FinalInterval(min, max);

		RandomAccessibleInterval<T> cropped=Views.interval(in, interval);
		
		Img<T> out=ops.create().img(cropped, Util.getTypeFromInterval(cropped));
		
		ops.copy().rai(out, cropped);
		
		return out;
	}

	static public Img<FloatType> normalize(RandomAccessibleInterval<FloatType> in,
		OpService ops)
	{

		final FloatType sum = new FloatType(ops.stats().sum(Views.iterable(in))
			.getRealFloat());

		return (Img<FloatType>) ops.math().divide(Views.iterable(in), sum);

	}

	static public RandomAccessibleInterval<FloatType> subtractMin(RandomAccessibleInterval<FloatType> in,
		OpService ops)
	{

		final FloatType min = new FloatType(ops.stats().min(Views.iterable(in))
			.getRealFloat());

		return (RandomAccessibleInterval)ops.math().subtract(Views.iterable(in), min);

	}



}
