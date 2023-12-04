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

    unsigned int N0, N1, N2;

    // load test images.  These images are expected to be pre-conditioned
    // 1.  PSF and image extended to supported OpenCL FFT size
    // 2.  PSF and image saved as 32 bit float.
    // 3.  PSF center translated to 0,0,0. 
    float * img, *psf;
    int rows, cols, slices; 

    openTifStack("D:\\images\\bars\\barsext32f.tif", &img, &N0, &N1, &N2);
    openTifStack("D:\\images\\bars\\barspsfext32f.tif", &psf, &N0, &N1, &N2);

    if (img==NULL) {
        cout<<"File not found\n"<<flush;
        return -1;
    }

    unsigned long n=N0*N1;

    float * rescaled = new float[n];

    float * decon = new float[N0*N1*N2];
    float * normal = new float[N0*N1*N2];  

    for (int i=0;i<N0*N1*N2;i++) {
        decon[i]=img[i];
        normal[i]=1;
    }

    deconv3d_32f_tv(50, 0.002, N0, N1, N2, img, psf, decon, normal, 0, 0);

    // rescale for visualization
    rescale(img, rescaled, 1., n);
    Mat cvImg(N0, N1, CV_32F, rescaled);
    //namedWindow( "Display window", WINDOW_AUTOSIZE );// Create a window for display.
    imshow( "Img", cvImg);                   // Show our image inside it.
     
    // rescale for visualization
    rescale(decon, rescaled, 1., n);
    Mat cvDecon(N0, N1, CV_32F, rescaled);
    //namedWindow( "Display window", WINDOW_AUTOSIZE );// Create a window for display.
    imshow( "Decon", cvDecon);                   // Show our image inside it.
    waitKey(0);

    delete img;
    delete psf;
}