package net.haesleinhuepf.clijx.plugins;

import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.clearcl.interfaces.ClearCLImageInterface;
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

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_multiplyComplexImages")
public class MultiplyComplexImages extends AbstractCLIJ2Plugin implements CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, IsCategorized, HasClassifiedInputOutput {
    @Override
    public String getInputType() {
        return "Complex Image";
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
        return "Image factor1, Image factor2, ByRef Image destination";
    }

    @Override
    public boolean executeCL() {
        return multiplyComplexImages(getCLIJ2(), (ClearCLBuffer)( args[0]), (ClearCLBuffer)(args[1]), (ClearCLBuffer)(args[2]));
    }

    public static boolean multiplyComplexImages(CLIJ2 clij2, ClearCLImageInterface src1, ClearCLImageInterface src2, ClearCLImageInterface dst) {
        assertDifferent(src1, dst);
        assertDifferent(src2, dst);

        HashMap<String, Object> parameters = new HashMap<>();
        parameters.put("src1", src1);
        parameters.put("src2", src2);
        parameters.put("dst", dst);

        if (!checkDimensions(src1.getDimension(), src2.getDimension(), dst.getDimension())) {
            throw new IllegalArgumentException("Error: number of dimensions don't match! (multiplyComplexImages)");
        }

        clij2.execute(MultiplyComplexImages.class, "multiply_complex_images_x.cl", "multiply_complex_images", dst.getDimensions(), dst.getDimensions(), parameters);
        return true;
    }




    @Override
    public String getDescription() {
        return "Multiplies two complex images pixel by pixel.\n\n";
    }

    @Override
    public String getAvailableForDimensions() {
        return "2D, 3D";
    }
}
