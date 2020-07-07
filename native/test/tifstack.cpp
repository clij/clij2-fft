#include <iostream>
#include "tiff.h"
#include "tiffio.h"

using namespace std;

float* read3DTiff(char name[], unsigned int * size) {
    cout<<name;
    TIFF *tiffFile = TIFFOpen(name, "r");

    uint16 bitDepth;
	
    TIFFGetField(tiffFile, TIFFTAG_IMAGEWIDTH, &size[0]);           
	TIFFGetField(tiffFile, TIFFTAG_IMAGELENGTH, &size[1]);        
	TIFFGetField(tiffFile, TIFFTAG_BITSPERSAMPLE, &bitDepth);       

    unsigned int zDim=0;
    if (tiffFile) {

        // find z dim 
	    do{
			zDim++;
		} while (TIFFReadDirectory(tiffFile));

        size[2]=zDim;

        (void)TIFFClose(tiffFile);
        
        tiffFile = TIFFOpen(name, "r");
    
        float * data = new float[size[0]*size[1]*size[2]];
	    
        if (bitDepth==16) {
            // not supported yet
            return NULL;
        }
        else {
            int n = 0; 
            do{
                for (uint32 row = 0; row < size[1]; row++) {
                    TIFFReadScanline(tiffFile, &data[row*size[0] + n*size[0] * size[1]], row, 0);
                }
                n++;
            } while (TIFFReadDirectory(tiffFile));

            (void)TIFFClose(tiffFile);
        
            return data;
        }
    }
    else {
        return NULL;
    }
}