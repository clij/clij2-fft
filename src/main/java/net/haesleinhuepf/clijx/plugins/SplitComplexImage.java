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

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_splitComplexImage")
public class SplitComplexImage extends AbstractCLIJ2Plugin implements CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, IsCategorized, HasClassifiedInputOutput {
    @Override
    public String getInputType() {
        return "Complex Image";
    }

    @Override
    public String getOutputType() {
        return "Image, Image";
    }

    @Override
    public String getCategories() {
        return "Math";
    }

    @Override
    public String getParameterHelpText() {
        return "Image complex, ByRef Image real, ByRef Image imaginary";
    }

    @Override
    public boolean executeCL() {
        return splitComplexImage(getCLIJ2(), (ClearCLBuffer)( args[0]), (ClearCLBuffer)(args[1]), (ClearCLBuffer)(args[2]));
    }

    public static boolean splitComplexImage(CLIJ2 clij2, ClearCLImageInterface complex_src, ClearCLImageInterface real_dst, ClearCLImageInterface imaginary_dst) {
        assertDifferent(complex_src, real_dst);
        assertDifferent(complex_src, imaginary_dst);

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("complex_src", complex_src);
        parameters.put("real_dst", real_dst);
        parameters.put("imaginary_dst", imaginary_dst);

        if (!checkDimensions(real_dst.getDimension(), imaginary_dst.getDimension(), complex_src.getDimension())) {
            throw new IllegalArgumentException("Error: number of dimensions don't match! (split_complex_image)");
        }

        clij2.execute(SplitComplexImage.class, "split_complex_image_x.cl", "split_complex_image", real_dst.getDimensions(), real_dst.getDimensions(), parameters);
        return true;
    }

    @Override
    public String getDescription() {
        return "Takes a complex image and splits it into its real and imaginary parts as two separate images.\n\n";
    }

    @Override
    public String getAvailableForDimensions() {
        return "2D, 3D";
    }

    @Override
    public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
        if (input.getDimension() == 2) {
            return getCLIJ2().create(new long[]{input.getWidth() / 2, input.getHeight()}, NativeTypeEnum.Float);
        } else { // dim : 3
            return getCLIJ2().create(new long[]{input.getWidth() / 2, input.getHeight(), input.getDepth()}, NativeTypeEnum.Float);
        }
    }
}
