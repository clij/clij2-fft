package net.haesleinhuepf.clijx.plugins;

import org.jocl.NativePointerObject;
import org.scijava.command.Command;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.imglib2.img.Img;

/**
 * FFT Filter command
 * 
 * Applies a box filter to an Image
 * 
 * It's used to test the FFT, however we had to move it from the test package because commands in a test
 * package are not added to the ImageJ UI menus. 
 * 
 * @author bnort
 *
 */
@Plugin(type = Command.class, menuPath = "Plugins>Test>TestCliFFT")
public class FFTFilterCommand implements Command {

	@Parameter
	Img img;

	@Override
	public void run() {

		CLIJ2 clij2 = null;

		// get clij
		try {
			clij2 = CLIJ2.getInstance("RTX");
		} catch (Exception e) {
			System.out.println(e);
			return;
		}

		clij2.show(img, "img");

		// push to GPU
		ClearCLBuffer gpu_img = clij2.push(img);

		// input size
		int N0 = (int) gpu_img.getDimensions()[0];
		int N1 = (int) gpu_img.getDimensions()[1];

		// Complex FFT size of real input signal
		int M0 = N0 / 2 + 1;
		int M1 = N1;

		// buffer to put FFT in
		ClearCLBuffer FFT = clij2.create(2 * M0, M1);
		// FFT after filter is applied
		ClearCLBuffer FFT2 = clij2.create(2 * M0, M1);
		// filter
		ClearCLBuffer filter = clij2.create(2 * M0, M1);
		// output
		ClearCLBuffer out = clij2.create(new long[] { N0, N1 }, NativeTypeEnum.Float);

		// Get the CL Buffers, context, queue and device as long native pointers
		long longPointerImg = ((NativePointerObject) (gpu_img.getPeerPointer().getPointer())).getNativePointer();
		long longPointerFFT = ((NativePointerObject) (FFT.getPeerPointer().getPointer())).getNativePointer();
		long longPointerFFT2 = ((NativePointerObject) (FFT2.getPeerPointer().getPointer())).getNativePointer();
		long longPointerOut = ((NativePointerObject) (out.getPeerPointer().getPointer())).getNativePointer();
		long l_context = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext().getPeerPointer().getPointer()))
				.getNativePointer();
		long l_queue = ((NativePointerObject) (clij2.getCLIJ().getClearCLContext().getDefaultQueue().getPeerPointer()
				.getPointer())).getNativePointer();

		clij2fftWrapper.fft2d_32f_lp(N0, N1, longPointerImg, longPointerFFT, l_context, l_queue);

		clij2.show(FFT, "FFT");

		// apply a naive box filter to the image
		// consider FFT is complex with N0/2+1 coefficients in X, and N1 in Y but stored
		// as floats
		// the complex numbers are stored in the loat array in interleaving format
		// that is (real1, imag1, real2, imag2,... realn, imagn)
		// TODO in the future it would be nice if CLIJ supported a complex format
		clij2.drawBox(filter, 0, 0, N0 / 8, N1 / 16);
		clij2.drawBox(filter, 0, 15 * N1 / 16, N0 / 8, N1 / 16);

		clij2.show(filter, "filter");
		clij2.multiplyImages(FFT, filter, FFT2);

		clij2fftWrapper.fft2dinv_32f_lp(N0, N1, longPointerFFT2, longPointerOut, l_context, l_queue);

		clij2.show(out, "inverse");

	}

}
