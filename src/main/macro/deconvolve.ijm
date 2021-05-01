run("Close All");

//open("/Users/haase/Downloads/images/Bars-G10-P15-stack-cropped.tif");
open("C:/structure/data/clij2fft/Bars-G10-P15-stack-cropped.tif");
run("32-bit");
image = getTitle();

//open("/Users/haase/Downloads/images/PSF-Bars-stack-cropped-64.tif");
open("C:/structure/data/clij2fft/PSF-Bars-stack-cropped-64.tif");
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
