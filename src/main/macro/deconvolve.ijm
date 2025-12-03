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
run("Close All");

//open("/Users/haase/Downloads/images/Bars-G10-P15-stack-cropped.tif");
open("C:/structure/data/clij2fft/Bars-G10-P15-stack-cropped.tif");
run("32-bit");
image = getTitle();

//open("/Users/haase/Downloads/images/PSF-Bars-stack-cropped-64.tif");
open("C:/structure/data/clij2fft/PSF-Bars-stack-cropped-64.tif");
run("32-bit");
psf = getTitle();

run("CLIJ2 Macro Extensions", "cl_device=[GeForce RTX 2070]");
Ext.CLIJ2_clear();

Ext.CLIJ2_push(image);
Ext.CLIJ2_push(psf);

// ensure that PSF intensities are between 0 and 1
Ext.CLIJx_normalize(psf, normalized_psf);

// deconvolve
num_iterations = 10;
Ext.CLIJx_deconvolveRichardsonLucyFFT(image, normalized_psf, deconvolved, num_iterations);

Ext.CLIJ2_pull(deconvolved);
