
package net.haesleinhuepf.clijx.plugins;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;
import net.haesleinhuepf.clij.macro.CLIJOpenCLProcessor;
import net.haesleinhuepf.clij.macro.documentation.OffersDocumentation;
import net.haesleinhuepf.clij2.AbstractCLIJ2Plugin;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clij2.utilities.HasAuthor;
import net.haesleinhuepf.clij2.utilities.HasClassifiedInputOutput;
import net.haesleinhuepf.clij2.utilities.IsCategorized;
import net.imagej.ops.OpService;
import net.imagej.ops.filter.fftSize.NextPowerOfTwo;
import org.jocl.NativePointerObject;
import org.scijava.Context;
import org.scijava.command.CommandService;
import org.scijava.plugin.Plugin;

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_normalize")
public class Normalize extends AbstractCLIJ2Plugin implements
	CLIJMacroPlugin, CLIJOpenCLProcessor, OffersDocumentation, HasAuthor, HasClassifiedInputOutput, IsCategorized
{
	@Override
	public boolean executeCL() {
		boolean result = normalize(getCLIJ2(), (ClearCLBuffer) (args[0]), (ClearCLBuffer) (args[1]));
		return result;
	}

	public static boolean normalize(CLIJ2 clij2, ClearCLBuffer input, ClearCLBuffer destination)
	{
		double mininum = clij2.minimumOfAllPixels(input);
		double maxinum = clij2.maximumOfAllPixels(input);
		double range = maxinum - mininum;

		ClearCLBuffer temp = clij2.create(destination);

		clij2.addImageAndScalar(input, temp, -mininum);
		clij2.multiplyImageAndScalar(temp, destination, 1.0 / range);

		temp.close();

		return true;
	}

	@Override
	public ClearCLBuffer createOutputBufferFromSource(ClearCLBuffer input) {
		ClearCLBuffer in = (ClearCLBuffer) args[0];
		return getCLIJ2().create(in.getDimensions(), NativeTypeEnum.Float);
	}

	@Override
	public String getParameterHelpText() {
		return "Image input, ByRef Image destination";
	}

	@Override
	public String getDescription() {
		return "Normalizes an image so that its intensities range from 0 to 1.";
	}

	@Override
	public String getAvailableForDimensions() {
		return "2D, 3D";
	}

	@Override
	public String getAuthorName() {
		return "Robert Haase";
	}

	@Override
	public String getInputType() {
		return "Image";
	}

	@Override
	public String getOutputType() {
		return "Image";
	}

	@Override
	public String getCategories() {
		return "Filter";
	}
}
