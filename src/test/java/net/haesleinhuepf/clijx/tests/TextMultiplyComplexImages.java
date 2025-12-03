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

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.plugins.CombineComplexImage;
import net.haesleinhuepf.clijx.plugins.MultiplyComplexImages;
import net.haesleinhuepf.clijx.plugins.SplitComplexImage;
import org.junit.Test;

public class TextMultiplyComplexImages {
    @Test
    public void testMultiplyComplexImages() {
        CLIJ2 clij2 = CLIJ2.getInstance();

        ClearCLBuffer real = clij2.pushString("" +
                "1 2\n" +
                "3 4");

        ClearCLBuffer imaginary = clij2.pushString("" +
                "5 6\n" +
                "7 8");

        ClearCLBuffer complex_input = clij2.create(real.getWidth() * 2, real.getHeight());

        CombineComplexImage.combineComplexImage(clij2, real, imaginary, complex_input);

        System.out.println("Complex input:");
        clij2.print(complex_input);

        ClearCLBuffer complex_output = clij2.create(real.getWidth() * 2, real.getHeight());

        System.out.println("Computing input squared...");
        MultiplyComplexImages.multiplyComplexImages(clij2, complex_input, complex_input, complex_output);

        System.out.println("Complex output:");
        clij2.print(complex_output);

        clij2.set(real, 0);
        clij2.set(imaginary, 0);

        SplitComplexImage.splitComplexImage(clij2, complex_output, real, imaginary);
        System.out.println("Real output");
        clij2.print(real);
        System.out.println("Imaginary output");
        clij2.print(imaginary);

        // clean up
        clij2.clear();
    }
}
