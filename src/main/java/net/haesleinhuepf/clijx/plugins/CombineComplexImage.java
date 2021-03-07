package net.haesleinhuepf.clijx.plugins;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.utilities.HasClassifiedInputOutput;
import net.haesleinhuepf.clij2.utilities.IsCategorized;
import org.scijava.plugin.Plugin;

import java.util.HashMap;

import static net.haesleinhuepf.clij.utilities.CLIJUtilities.assertDifferent;
import static net.haesleinhuepf.clij2.utilities.CLIJUtilities.checkDimensions;

/**
 * Author: @haesleinhuepf
 *         February 2021
 */

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_combineComplexImage")
public class CombineComplexImage extends AbstractCLIJ2Plugin implements CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, IsCategorized, HasClassifiedInputOutput {
    @Override
    public String getInputType() {
        return "Image, Image";
    }

    @Override
    public String getOutputType() {
        return "Complex Image";
    }

    @Override
    public String getCategories() {
        return "Math";
    }

    @Override
    public String getParameterHelpText() {
        return "Image real, Image imaginary, ByRef Image complex_destination";
    }

    @Override
    public boolean executeCL() {
        return combineComplexImage(getCLIJ2(), (ClearCLBuffer)( args[0]), (ClearCLBuffer)(args[1]), (ClearCLBuffer)(args[2]));
    }

    public static boolean combineComplexImage(CLIJ2 clij2, ClearCLImageInterface real_src, ClearCLImageInterface imaginary_src, ClearCLImageInterface complex_dst) {
        assertDifferent(real_src, complex_dst);
        assertDifferent(imaginary_src, complex_dst);

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("real_src", real_src);
        parameters.put("imaginary_src", imaginary_src);
        parameters.put("complex_dst", complex_dst);

        if (!checkDimensions(real_src.getDimension(), imaginary_src.getDimension(), complex_dst.getDimension())) {
            throw new IllegalArgumentException("Error: number of dimensions don't match! (combine_complex_image)");
        }

        clij2.execute(CombineComplexImage.class, "combine_complex_image_x.cl", "combine_complex_image", real_src.getDimensions(), real_src.getDimensions(), parameters);
        return true;
    }

    @Override
    public String getDescription() {
        return "Takes real and imaginary parts of an image as two separate images and puts them together in a complex image.\n\n";
    }

    @Override
    public String getAvailableForDimensions() {
        return "2D, 3D";
    }

    @Override
    public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
        if (input.getDimension() == 2) {
            return getCLIJ2().create(new long[]{input.getWidth() * 2, input.getHeight()}, NativeTypeEnum.Float);
        } else { // dim : 3
            return getCLIJ2().create(new long[]{input.getWidth() * 2, input.getHeight(), input.getDepth()}, NativeTypeEnum.Float);
        }
    }
}
