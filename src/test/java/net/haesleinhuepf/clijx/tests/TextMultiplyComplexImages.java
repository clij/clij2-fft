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
