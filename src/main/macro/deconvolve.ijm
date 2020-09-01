run("Close All");

open("C:/structure/data/Deconvolution_Brian/CElegans-CY3.tif");
run("32-bit");
image = getTitle();

open("C:/structure/data/Deconvolution_Brian/PSF-CElegans-CY3.tif");
run("32-bit");
makeRectangle(311, 332, 51, 51);
run("Crop");
psf = getTitle();

run("CLIJ2 Macro Extensions", "cl_device=[GeForce RTX 2070]");
Ext.CLIJ2_clear();

Ext.CLIJ2_push(image);
Ext.CLIJ2_push(psf);


deconvolved = "deconvolved";
Ext.CLIJx_deconvolveRichardsonLucyFFT(image, psf, deconvolved, 10);
//Ext.CLIJx_convolveFFT(image, psf, deconvolved);

Ext.CLIJ2_pull(deconvolved);
