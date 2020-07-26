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

    float * data = read3DTiff("../../../images/bridge32f.tif", size);

    if (data==NULL) {
        cout<<"File not found\n"<<flush;
        return -1;
    }

    // input image size    
    unsigned int N0=size[0];
    unsigned int N1=size[1];
    unsigned long n=N0*N1;

    // output fft size
    unsigned int M0=size[0]/2+1;
    unsigned int M1=size[1];
    unsigned long nFreq=M1*M0;

    // memory for fft and inverse fft
    float * fft = new float[nFreq*2];
    float * invfft = new float[n];

    // call opencl fft
    fft2d_32f(N0, N1, data, fft);

    // call opencl inverse fft
    fftinv2d_32f(N0, N1, fft, invfft);

    float * abs = new float[nFreq];

    // take log of absolute value of fft coefficients so we can visualize
    for (int i=0;i<nFreq;i++) {
        float real=fft[2*i];
        float imag=fft[2*i+1];

        abs[i]=log(sqrt(real*real+imag*imag));
    }

    float * rescaled = new float[size[0]*size[1]];
    float * rescaledinv = new float[n];
    float * rescaledabs = new float[nFreq];
    rescale(data, rescaled, 1., n);
    rescale(abs, rescaledabs, 1., nFreq);
    rescale(invfft, rescaledinv, 1., n);

    Mat cvImg(size[0], size[1], CV_32F, rescaled);
    Mat cvAbs(M1, M0, CV_32F, rescaledabs);
    Mat cvInv(N1, N0, CV_32F, rescaledinv);

    //namedWindow( "Display window", WINDOW_AUTOSIZE );// Create a window for display.
    imshow( "Img", cvImg);                   // Show our image inside it.
    imshow( "FFT Abs", cvAbs);                   // Show our image inside it.
    imshow( "Inv FFT", cvInv);                   // Show our image inside it.
    waitKey(0);

    delete data;
    delete fft;
    delete abs;
    delete rescaled;
    delete rescaledabs;
}