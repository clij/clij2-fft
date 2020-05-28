
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

	static public <T extends ComplexType<T>> ImgPlus<T> createMultiChannelImgPlus(
		DatasetService data, RandomAccessibleInterval<T>... rais)
	{
		ArrayList<RandomAccessibleInterval<T>> channelList = new ArrayList<>();

		for (RandomAccessibleInterval<T> rai : rais) {
			channelList.add(rai);
		}

		AxisType[] axisTypes = new AxisType[] { Axes.X, Axes.Y, Axes.Z,
			Axes.CHANNEL };

		RandomAccessibleInterval stack = Views.stack(channelList);

		return new ImgPlus(data.create(stack), "image", axisTypes);

	}

}
