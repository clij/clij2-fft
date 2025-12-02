## Test Clij 3D Deconvolution GUI with Fiji

### Add clij2-fft update site or build from source

Add following update site https://sites.imagej.net/clijx-deconvolution/

### Get bars test image

The bars test image can be found [here](https://www.dropbox.com/scl/fo/49jvlu3cpay647m1z84t1/AO3JePK-TP7rrz1KcYm_rVA?rlkey=jhsm89ififo518f7foovdh4e5&st=g4xkwpj1&dl=0)

###  Open CLIJ 3D Deconvolution Plugin

If you have many plugins installed you will have to scroll to near the bottom of the plugin menu then choose ```CLIJ->Deconvolution->CLIJ 3D Deconvolution GUI```  

![image](https://github.com/user-attachments/assets/8b3584b4-d4d2-41b2-b17d-6a6a7cdb8131)

###  Deconvolve Bars Image 

Open the bars image and the bars PSF then choose them on the ```Input Image``` and ```Input PSF``` Combo Boxes.  Press run...  

A deconvolved image should appear.  Scroll through the z stack and compare to the orginal to confirm the deconvolution did run (if an error occurs the image may be empty or may be exactly equal to the original)  

Feel free to play with the options.  The GUI has not been extensively tested yet and there will be more testing and documentation to come... 

![image](https://github.com/user-attachments/assets/172d083d-7fbd-4817-8460-8b978d98cfd9)




