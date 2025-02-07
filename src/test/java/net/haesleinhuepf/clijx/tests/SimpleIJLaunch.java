
package net.haesleinhuepf.clijx.tests;

import net.imagej.ImageJ;
import net.imglib2.type.NativeType;
import net.imglib2.type.numeric.RealType;

public class SimpleIJLaunch<T extends RealType<T> & NativeType<T>> {

	final static ImageJ ij = new ImageJ();

	public static void main(final String[] args) {
		ij.ui().showUI();

		// ij.command().run(DemoDeconvolutionCommand.class, true); // Uncomment to run automatically the demo
	}
}
