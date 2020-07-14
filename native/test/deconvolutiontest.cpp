#include <iostream>

#ifdef _WIN64
#include <direct.h>
#define GetCurrentDir _getcwd
#else
#include <unistd.h>
#define GetCurrentDir getcwd
#endif

#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>

#include "tifstack.h"
#include "../clij2fft/clij2fft.h"

#include "float.h"

using namespace std;
using namespace cv;

void rescale(float *in, float * out, float scale, int len) {
	
	float max=FLT_MIN;
	float min=FLT_MAX;

	for (int i=0;i<len;i++) {
        if (in[i]>max) {
            max=in[i];
        }        
        if (in[i]<min) {
            min=in[i];
        }
    }
   
	for (int i=0;i<len;i++) {
		out[i]=scale*((in[i]-min)/(max-min));
	}
}

int main() {
    cout<<"FFT Test\n"<<flush;

    char buff[FILENAME_MAX];
    GetCurrentDir( buff, FILENAME_MAX );
    cout<<"Current working dir: "<<buff<<"\n"<<flush;

    unsigned int size[3];

    // load test images.  These images are expected to be pre-conditioned
    // 1.  PSF and image extended to supported OpenCL FFT size
    // 2.  PSF and image saved as 32 bit float.
    // 3.  PSF center translated to 0,0,0.  
    float * img = read3DTiff("../../../images/barsext32f.tif", size);
    float * psf = read3DTiff("../../../images/barspsfext32f.tif", size);

    if (img==NULL) {
        cout<<"File not found\n"<<flush;
        return -1;
    }

    // input image size    
    unsigned int N0=size[0];
    unsigned int N1=size[1];
    unsigned int N2=size[2];

    unsigned long n=N0*N1;

    float * rescaled = new float[size[0]*size[1]];

    float * decon = new float[N0*N1*N2];  

    for (int i=0;i<N0*N1*N2;i++) {
        decon[i]=img[i];
    }

    deconv(50, N0, N1, N2, img, psf, decon, NULL);

    // rescale for visualization
    rescale(img, rescaled, 1., n);
    Mat cvImg(size[0], size[1], CV_32F, rescaled);
    //namedWindow( "Display window", WINDOW_AUTOSIZE );// Create a window for display.
    imshow( "Img", cvImg);                   // Show our image inside it.
     
    // resclae for visualization
    rescale(decon, rescaled, 1., n);
    Mat cvDecon(size[0], size[1], CV_32F, rescaled);
    //namedWindow( "Display window", WINDOW_AUTOSIZE );// Create a window for display.
    imshow( "Decon", cvDecon);                   // Show our image inside it.
    waitKey(0);

    delete img;
    delete psf;
}