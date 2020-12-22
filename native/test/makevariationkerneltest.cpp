#ifdef _WIN64
#include <direct.h>
#define GetCurrentDir _getcwd
#else
#include <unistd.h>
#define GetCurrentDir getcwd
#endif

#if defined(__APPLE__) || defined(__MACOSX)
#include <OpenCL/cl.h>
#else
#include <CL/cl.h>
#endif

#include <iostream>

#include "../clij2fft/clij2fft.h"

using namespace std;

/**
 * 
 * Tests loading and compiling the variation kernel
 * 
 **/
int main() {
    
    cout<<"Make variation kernel test\n"<<flush;
  
    cl_platform_id platformId = NULL;
	cl_device_id deviceID = NULL;
	cl_uint retNumDevices;
	cl_uint retNumPlatforms;

    cl_int ret = clGetPlatformIDs(1, &platformId, &retNumPlatforms);

    cout<<"created platform "<<ret<<" "<<platformId<<" "<<retNumPlatforms<<"\n";

	ret = clGetDeviceIDs(platformId, CL_DEVICE_TYPE_DEFAULT, 1, &deviceID, &retNumDevices);

    cout<<"getDeviceIDs "<<ret<<" "<<deviceID<<" "<<retNumDevices<<"\n";
	
    // Creating context.
	cl_context context = clCreateContext(NULL, 1, &deviceID, NULL, NULL,  &ret);

    cout<<"Create context "<<ret<<"\n";
  
    const char * fileName = "/home/bnorthan/code/imagej/clij2-fft/native/clij2fft/totalvariationterm.cl";
    
    size_t sizer=getFileSize(fileName);
    
    cout<<"size is "<<sizer<<"\n";

    char * program_str = (char*)malloc(sizer);

    getProgramFromFile(fileName, program_str, sizer);

    cout<<program_str<<"\n";
    
    cl_program program2 = makeProgram(context, deviceID, program_str);

}

