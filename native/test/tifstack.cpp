#include <iostream>

#include <opencv2/imgcodecs.hpp>
#include <opencv2/core/core.hpp>
#include <opencv2/highgui/highgui.hpp>
#include <opencv2/opencv.hpp>

using namespace std;
using namespace cv;

void openTifStack(string fileName, float ** mem, unsigned int*cols, unsigned int*rows, unsigned int*slices) {
    cout<<"Open Tiff File "<<fileName<<"\n";
   
     // Create a vector to store the images
    std::vector<cv::Mat> images;

    // Read the 3D TIFF file using imreadmulti
    imreadmulti(fileName, images, IMREAD_ANYCOLOR | IMREAD_ANYDEPTH);

    // Check if the images were successfully loaded
    if (images.empty()) {
        // Images are empty, which means imreadmulti failed to read the file
        std::cerr << "Error: Could not open or read the image file." << std::endl;
        throw std::runtime_error("File not read");
    }

    uchar depth = images[0].type() & CV_MAT_DEPTH_MASK;

    string r;
    switch ( depth ) {
    case CV_8U:  r = "8U"; break;
    case CV_8S:  r = "8S"; break;
    case CV_16U: r = "16U"; break;
    case CV_16S: r = "16S"; break;
    case CV_32S: r = "32S"; break;
    case CV_32F: r = "32F"; break;
    case CV_64F: r = "64F"; break;
    default:     r = "User"; break;
    }

    cout<<"type is "<<r<<"\n"<<flush;
 
    long n=images[0].cols*images[0].rows*images.size();; 
    *mem=new float[n];
    *cols=images[0].cols;
    *rows=images[0].rows;
    *slices=images.size();

    if (depth==CV_32F) {
        for (int z=0;z<images.size();z++) {
            for (int y=0;y<images[0].rows;y++) {
                for (int x=0;x<images[0].cols;x++) {
                    (*mem)[z*images[0].rows*images[0].cols+y*images[0].cols+x]=images[z].at<float>(y,x);
                }
            }
        }
    }  
    else {
        for (int z=0;z<images.size();z++) {
            for (int y=0;y<images[0].rows;y++) {
                for (int x=0;x<images[0].cols;x++) {
                    (*mem)[z*images[0].rows*images[0].cols+y*images[0].cols+x]=images[z].at<uchar>(y,x);
                }
            }
        }
    }
}
