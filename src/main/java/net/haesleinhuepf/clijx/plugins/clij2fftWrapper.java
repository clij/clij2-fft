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

import org.bytedeco.javacpp.Loader;
import org.bytedeco.javacpp.annotation.Platform;
import org.bytedeco.javacpp.annotation.Properties;

@Properties(value = { @Platform(
	include = "clij2fft.h", 
	link = {"clij2fft", 
					"clFFT" }), 
	@Platform(value = "windows-x86_64", 
		linkpath = {
			"C:/Program Files/NVIDIA GPU Computing Toolkit/CUDA/v11.2/lib/x64/"
		}, 
		preloadpath = {
			"C:/OpenCL/clFFT-2.12.2-Windows-x64/bin/" }, 
			preload = { "clFFT" }),
	@Platform(value = "linux-x86_64",
			includepath = {"/usr/local/cuda-10.0/include/"}, 
			linkpath = {
			"/usr/local/cuda-10.0/lib64/" }, 
			preload = { "clFFT" }) })
public class clij2fftWrapper {

	static {
		Loader.load();
	}

	public static native long fft2d_32f_lp(long N1, long N2, long inPointer,
		long outPointer, long contextPointer, long queuePointer);
		
	public static native long fft2dinv_32f_lp(long N1, long N2, long inPointer,
		long outPointer, long contextPointer, long queuePointer);
	
	public static native long fft3d_32f_lp(long N0, long N1, long N2, long inPointer,
		long outPointer, long contextPointer, long queuePointer);
		
	public static native long fft3dinv_32f_lp(long N0, long N1, long N2, long inPointer,
		long outPointer, long contextPointer, long queuePointer);
	
	public static native int conv3d_32f_lp(long N0, long N1, long N2, long l_image,
		long l_psf, long l_output, boolean correlate, long l_context, long l_queue,
		long l_device);

	public static native int deconv3d_32f_lp(int iterations, long N0, long N1,
		long N2, long d_image, long d_psf, long d_update, long d_normal,
		long l_context, long l_queuee, long l_device);

	public static native int deconv3d_32f_lp_tv(int iterations, float regularizationFactor, 
		long N0, long N1, long N2, long d_image, long d_psf, long d_update, long d_normal,
		long l_context, long l_queuee, long l_device);
	
	public static native int diagnostic();
	
	public static native int cleanup();

	public static void load() {
		Loader.load();
	};

}
