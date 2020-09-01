run("Close All");

open("C:/structure/data/Deconvolution_Brian/Bars-G10-P15-stack-cropped.tif");
run("32-bit");
image = getTitle();

open("C:/structure/data/Deconvolution_Brian/PSF-Bars-stack-cropped-64.tif");
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
