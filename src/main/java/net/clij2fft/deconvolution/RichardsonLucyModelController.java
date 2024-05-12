package net.clij2fft.deconvolution;

import ij.ImagePlus;
import net.haesleinhuepf.clij.clearcl.ClearCLBuffer;
import net.haesleinhuepf.clij.coremem.enums.NativeTypeEnum;
import net.haesleinhuepf.clij2.CLIJ2;
import net.haesleinhuepf.clijx.plugins.DeconvolveRichardsonLucyFFT;

public class RichardsonLucyModelController {
	
	float xSpacing;

	public float getxSpacing() {
		return xSpacing;
	}

	public void setxSpacing(float xSpacing) {
		this.xSpacing = xSpacing;
	}
	
	public void runDeconvolution(ImagePlus imp, ImagePlus psf, int numIterations) {
		

		CLIJ2 clij2=null;
		
		// get clij
		try {
			clij2 = CLIJ2.getInstance("RTX");
		}
		catch(Exception e) {
			System.out.println(e);
			return;
		}

		ClearCLBuffer gpu_psf = clij2.push(psf);
		ClearCLBuffer gpu_image = clij2.push(imp);
		
		ClearCLBuffer gpu_deconvolved = clij2.create(gpu_image.getDimensions(), NativeTypeEnum.Float);
		
		DeconvolveRichardsonLucyFFT.deconvolveRichardsonLucyFFT(clij2, gpu_image, gpu_psf, gpu_deconvolved, 100, 0.002f, true);
		
		ImagePlus out = clij2.pull(gpu_deconvolved);
		
		out.show();
	}

}
