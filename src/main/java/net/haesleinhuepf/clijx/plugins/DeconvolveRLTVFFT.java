package net.haesleinhuepf.clijx.plugins;

import net.haesleinhuepf.clij.macro.CLIJMacroPlugin;

import org.scijava.plugin.Plugin;

@Plugin(type = CLIJMacroPlugin.class, name = "CLIJx_deconvolveRLTVFFT")
public class DeconvolveRLTVFFT extends DeconvolveRichardsonLucyFFT {

	@Override
	public String getParameterHelpText() {
		return "Image input, Image convolution_kernel, ByRef Image destination, Number num_iterations, Number Regularization_Factor, Boolean non_circulant";
	}

	@Override
	public String getDescription() {
		return "Applies Richardson-Lucy deconvolution with Total Variation noise regularization and option non-circulant edge handling.  Currently 3D images only";
	}
}
