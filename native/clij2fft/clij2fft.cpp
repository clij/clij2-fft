#include <stdio.h>

#ifdef _WIN64
#include <direct.h>
#define GetCurrentDir _getcwd
#else
#include <unistd.h>
#define GetCurrentDir getcwd
#endif

// #include "CL/cl.h"
#if defined(__APPLE__) || defined(__MACOSX)
#include <OpenCL/cl.h>
#else
#include <CL/cl.h>
#endif

#include "clFFT.h"
#include <math.h>
#include "clij2fft.h"
#include <iostream>

#include "kernels/cle_totalvariationterm.h"

#define MULTI "test" \
"test test"

#define MAX_SOURCE_SIZE (0x100000)

int globaldebug=0;

#define CHECKRETURN(x,message,debug) do { \
  int retval = (x); \
  if (retval != 0) { \
    printf("Runtime error: %s returned %d at %s:%d", #x, retval, __FILE__, __LINE__); \
    return ret;/* or throw or whatever */; \
  } \
  if ( (debug==1) && (globaldebug==1) ) { \
    printf("%s %d\n", message, retval); \
  } \
} while (0)

#define CHECK(x,message,debug) do { \
  int retval = (x); \
  if (retval != 0) { \
    printf("Runtime error: %s returned %d at %s:%d", #x, retval, __FILE__, __LINE__); \
  } \
  if ( (debug==1) && (globaldebug==1) ) { \
    printf("%s %d\n", message, retval); \
  } \
} while (0) 

#define ENTEREXIT(enter,message) do{ \
  if ( (enter == 1) && (globaldebug==1) ) { \
    printf("\nEnter %s\n",message); \
  } \
  if ( (enter == 0) && (globaldebug==1) ) { \
    printf("Exit %s\n\n",message); \
  } \
} while (0)

#define MAXPLATFORMS 10
#define MAXDEVICESPERPLATFORM 10

// Author: Brian Northan
// License: BSD

// OpenCL kernel. Each work item takes care of one element of c
const char * programString =                                       "\n" \
"#pragma OPENCL EXTENSION cl_khr_fp64 : enable                    \n" \
"__kernel void vecAdd(  __global float *a,                       \n" \
"                       __global float *b,                       \n" \
"                       __global float *c,                       \n" \
"                       const unsigned int n)                    \n" \
"{                                                               \n" \
"    //Get our global thread ID                                  \n" \
"    int id = get_global_id(0);                                  \n" \
"                                                                \n" \
"    //Make sure we do not go out of bounds                      \n" \
"    if (id < n)                                                 \n" \
"        c[id] = a[id] + b[id];                                  \n" \
"}                                                               \n" \
"\n" \
"#pragma OPENCL EXTENSION cl_khr_fp64 : enable                    \n" \
"__kernel void vecComplexMultiply(  __global float *a,                       \n" \
"                       __global float *b,                       \n" \
"                       __global float *c,                       \n" \
"                       const unsigned int n)                    \n" \
"{                                                               \n" \
"    //Get our global thread ID                                  \n" \
"    int id = get_global_id(0);                                  \n" \
"                                                                \n" \
"    //Make sure we do not go out of bounds                      \n" \
"    if (id < n)  {                                               \n" \
"        float real = a[2*id] * b[2*id]-a[2*id+1]*b[2*id+1];                                  \n" \
"        float imag = a[2*id]*b[2*id+1] + a[2*id+1]*b[2*id];                            \n" \
"        c[2*id]=real; \n" \
"        c[2*id+1]=imag; \n" \
"        }                           \n" \
"}                                                               \n" \
"#pragma OPENCL EXTENSION cl_khr_fp64 : enable                    \n" \
"__kernel void vecComplexConjugateMultiply(  __global float *a,                       \n" \
"                       __global float *b,                       \n" \
"                       __global float *c,                       \n" \
"                       const unsigned int n)                    \n" \
"{                                                               \n" \
"    //Get our global thread ID                                  \n" \
"    int id = get_global_id(0);                                  \n" \
"                                                                \n" \
"    //Make sure we do not go out of bounds                      \n" \
"    if (id < n)  {                                               \n" \
"        float real= a[2*id] * b[2*id]+a[2*id+1]*b[2*id+1];                                  \n" \
"        float imag = -a[2*id]*b[2*id+1] + a[2*id+1]*b[2*id];                            \n" \
"        c[2*id]=real; \n" \
"        c[2*id+1]=imag; \n" \
"     }                           \n" \
"}                                                               \n" \
"#pragma OPENCL EXTENSION cl_khr_fp64 : enable                    \n" \
"__kernel void vecDiv(  __global float *a,                       \n" \
"                       __global float *b,                       \n" \
"                       __global float *c,                       \n" \
"                       const unsigned int n)                    \n" \
"{                                                               \n" \
"    //Get our global thread ID                                  \n" \
"    int id = get_global_id(0);                                  \n" \
"                                                                \n" \
"    //Make sure we do not go out of bounds                      \n" \
"    if (id < n)  {                                               \n" \
"     if (b[id]>0)  {                                               \n" \
"        c[id] = a[id]/b[id];        \n" \
"       }                           \n" \
"       else {                           \n" \
"        c[id]=0;                        \n" \
"     }                           \n" \
"     }                           \n" \
"}                                                               \n" \
"#pragma OPENCL EXTENSION cl_khr_fp64 : enable                    \n" \
"__kernel void vecMul(  __global float *a,                       \n" \
"                       __global float *b,                       \n" \
"                       __global float *c,                       \n" \
"                       const unsigned int n)                    \n" \
"{                                                               \n" \
"    //Get our global thread ID                                  \n" \
"    int id = get_global_id(0);                                  \n" \
"                                                                \n" \
"    //Make sure we do not go out of bounds                      \n" \
"    if (id < n)  {                                               \n" \
"        c[id] = a[id]*b[id];        \n" \
"        }                           \n" \
"}                                                               \n" \
 "#pragma OPENCL EXTENSION cl_khr_fp64 : enable                    \n" \
"__kernel void removeSmallValues(  __global float *a,                       \n" \
"                       const unsigned int n)                    \n" \
"{                                                               \n" \
"    //Get our global thread ID                                  \n" \
"    int id = get_global_id(0);                                  \n" \
"                                                                \n" \
"    //Make sure we do not go out of bounds                      \n" \
"    if (id < n)  {                                               \n" \
"        if (a[id]<0.00001) {        \n" \
"        a[id] = 1.0;        \n" \
"       }                           \n" \
"    }                           \n" \
"}                                                               \n" \
 "\n" ;




/**
 * Get fileSize.  Ussually called before reading a kernel from a .cl file
 * 
 * **/                                                                
size_t getFileSize(const char * fileName) {

    FILE *fp;
    char *source_str;
    size_t source_size, program_size;

    fp = fopen(fileName, "r");
    if (!fp) {
        printf("Failed to load file\n");
        return -1;
    }

    fseek(fp, 0, SEEK_END);
    program_size = ftell(fp);
    
    fclose(fp);

    return program_size;
}

/**
 * Get a .cl program from a file.  program_str needs to be pre-allocated.  Call
 * getFileSize first 
 * */
int getProgramFromFile(const char* fileName, char * program_str, size_t program_size) {
  printf("get program from file %s\n",fileName);

  FILE *fp;

  fp = fopen(fileName, "r");
  if (!fp) {
      printf("Failed to load kernel\n");
      return -1;
  }

  program_str[program_size] = '\0';
  size_t readSize=fread(program_str, sizeof(char), program_size, fp);
  fclose(fp);

  return 0;

}

/**
 * Compile a cl_program from source_str
 * */
cl_program makeProgram(cl_context context, cl_device_id deviceID, const char * source_str) {
  ENTEREXIT(1,"make program");

  int ret;

  // Create program from kernel source
	//cl_program program = clCreateProgramWithSource(context, 1, (const char **)source_str, NULL, &ret);	
	cl_program program = clCreateProgramWithSource(context, 1, (const char **)&source_str, NULL, &ret);	
  CHECK(ret, "makeProgram createprogram",1);

	ret = clBuildProgram(program, 1, &deviceID, NULL, NULL, NULL);

  size_t size;
	// get size of build log
  ret = clGetProgramBuildInfo(program, deviceID, CL_PROGRAM_BUILD_LOG ,0,NULL,&size);
  CHECK(ret, "makeProgram createprogram",1);
  
  // allocate and get build log
  char *buildlog=(char*)malloc(size);
  clGetProgramBuildInfo(program, deviceID, CL_PROGRAM_BUILD_LOG ,size,buildlog,NULL);

  // print build log
  if (globaldebug==1) {   
    printf("\n\nBuildlog:   %s\n\n",buildlog);
  }

  free(buildlog);

  return program;
}

/**
 * Call a kernel on a vector (or an image stored contigously that can be treated as a vector)
 * **/
cl_int callInPlaceKernel(cl_kernel kernel, cl_mem in1, const unsigned int n, cl_command_queue commandQueue, size_t globalItemSize, size_t localItemSize) {
   // Set arguments for kernel
	cl_int ret = clSetKernelArg(kernel, 0, sizeof(cl_mem), (void *)&in1);
  CHECKRETURN(ret, "callInPlaceKernel set in1",1);
   
  ret = clSetKernelArg(kernel, 1, sizeof(unsigned int), &n);	
  CHECKRETURN(ret, "callInPlaceKernel set n",1);
  
  ret = clEnqueueNDRangeKernel(commandQueue, kernel, 1, NULL, &globalItemSize, &localItemSize, 0, NULL, NULL);	
  CHECKRETURN(ret, "callInPlaceKernel enqueue kernel",1);
   
  ret = clFinish(commandQueue);

  return ret;
}

/**
 * Call a kernel on a vector (or an image stored contigously that can be treated as a vector)
 * **/
cl_int callKernel(cl_kernel kernel, cl_mem in1, cl_mem in2, cl_mem out, const unsigned int n, cl_command_queue commandQueue, size_t globalItemSize, size_t localItemSize) {
   // Set arguments for kernel
	cl_int ret = clSetKernelArg(kernel, 0, sizeof(cl_mem), (void *)&in1);
  CHECKRETURN(ret, "calleKernel set in1",1);

	ret = clSetKernelArg(kernel, 1, sizeof(cl_mem), (void *)&in2);	
  CHECKRETURN(ret, "calleKernel set in2",1);

	ret = clSetKernelArg(kernel, 2, sizeof(cl_mem), (void *)&out);	
  CHECKRETURN(ret, "calleKernel set out",1);

  ret = clSetKernelArg(kernel, 3, sizeof(unsigned int), &n);	
  CHECKRETURN(ret, "calleKernel set n",1);

  ret = clEnqueueNDRangeKernel(commandQueue, kernel, 1, NULL, &globalItemSize, &localItemSize, 0, NULL, NULL);	
  CHECKRETURN(ret, "calleKernel enqueue kernel",1);
   
  ret = clFinish(commandQueue);

  return ret;
}

/**
 * Call the total variation kernel
 * */
cl_int callVariationKernel(cl_kernel kernel, cl_mem in, cl_mem correction, cl_mem out, const unsigned int Nx, const unsigned int Ny, const unsigned int Nz,
                    float hx, float hy, float hz, float regularizationFactor, cl_command_queue commandQueue, size_t globalItemSize, size_t localItemSize) {

   // Set arguments for kernel
	cl_int ret = clSetKernelArg(kernel, 0, sizeof(cl_mem), (void *)&in);
  CHECKRETURN(ret, "call variation Kernel set in",1);

	ret = clSetKernelArg(kernel, 1, sizeof(cl_mem), (void *)&correction);	
  CHECKRETURN(ret, "call variation Kernel set correction",1);

	ret = clSetKernelArg(kernel, 2, sizeof(cl_mem), (void *)&out);	
  CHECKRETURN(ret, "call variation Kernel set out",1);

  ret = clSetKernelArg(kernel, 3, sizeof(unsigned int), &Nx);	
  CHECKRETURN(ret, "call variation Kernel set Nx",1);
 
  ret = clSetKernelArg(kernel, 4, sizeof(unsigned int), &Ny);	
  CHECKRETURN(ret, "call variation Kernel set Ny",1);
 
  ret = clSetKernelArg(kernel, 5, sizeof(unsigned int), &Nz);	
  CHECKRETURN(ret, "call variation Kernel set Nz",1);

  ret = clSetKernelArg(kernel, 6, sizeof(float), &hx);	
  CHECKRETURN(ret, "call variation Kernel set hx",1);

  ret = clSetKernelArg(kernel, 7, sizeof(float), &hy);	
  CHECKRETURN(ret, "call variation Kernel set hy",1);

  ret = clSetKernelArg(kernel, 8, sizeof(float), &hz);	
  CHECKRETURN(ret, "call variation Kernel set hz",1);

  ret = clSetKernelArg(kernel, 9, sizeof(float), &regularizationFactor);	
  CHECKRETURN(ret, "call variation Kernel set regularizationFactor",1);

  size_t * global = (size_t*)malloc(sizeof(size_t)*3);
  size_t * local = (size_t*)malloc(sizeof(size_t)*3);

  global[0]=Nx;
  global[1]=Ny;
  global[2]=Nz;

  local[0]=512;
  local[1]=512;
  local[2]=64;

  ret = clEnqueueNDRangeKernel(commandQueue, kernel, 3, NULL, global, NULL, 0, NULL, NULL);	
  CHECKRETURN(ret, "call variation Kernel enqueue kernel",1);

  ret = clFinish(commandQueue);
  CHECKRETURN(ret, "call variation Kernel finish",1);

  free(global);
  free(local);
 
  return ret;
} 


cl_int setupFFT() {
   // Setup clFFT
  clfftSetupData fftSetup;
  cl_int ret = clfftInitSetupData(&fftSetup);
  CHECKRETURN(ret, "setupFFT initSetupData",1);
  ret = clfftSetup(&fftSetup);

  return ret;
}

clfftPlanHandle bake_2d_forward_32f(long N0, long N1, cl_context context, cl_command_queue commandQueue) {
  ENTEREXIT(1,"bake_3d_forward_32f");

  cl_int ret;
  // FFT library related declarations 
  clfftPlanHandle planHandleForward;
  clfftDim dim = CLFFT_2D;
  size_t clLengths[2] = {(size_t)N0, (size_t)N1};
  size_t inStride[3] = {1, (size_t)N0};
  // note each output row has N0/2+1 complex numbers 
  size_t outStride[3] = {1,(size_t)N0/2+1};

  // Create a default plan for a complex FFT.
  ret = clfftCreateDefaultPlan(&planHandleForward, context, dim, clLengths);
  CHECK(ret,"bake_2d_forward_32f create default plan",1);

  clfftPrecision precision = CLFFT_SINGLE;
  clfftLayout inLayout = CLFFT_REAL;
  clfftLayout outLayout = CLFFT_HERMITIAN_INTERLEAVED;
  clfftResultLocation resultLocation = CLFFT_OUTOFPLACE;
  
  // Set plan parameters. 
  ret = clfftSetPlanPrecision(planHandleForward, precision);
  //printf("clfft precision %d\n", ret);
  ret = clfftSetLayout(planHandleForward, inLayout, outLayout);
  //printf("clfft set layout real hermittian interveaved %d\n", ret);
  ret = clfftSetResultLocation(planHandleForward, resultLocation);
  //printf("clfft set result location %d\n", ret);
  ret=clfftSetPlanInStride(planHandleForward, dim, inStride);
  //printf("clfft set instride %d\n", ret);
  ret=clfftSetPlanOutStride(planHandleForward, dim, outStride);
  //printf("clfft set out stride %d\n", ret);

  // Bake the plan.
  ret = clfftBakePlan(planHandleForward, 1, &commandQueue, NULL, NULL);
  CHECK(ret,"bake_2d_forward_32f bake plan",1);

  ret = clFinish(commandQueue);
  CHECK(ret,"bake_2d_forward_32f finish",1);

  ENTEREXIT(0,"bake_3d_forward_32f");
  return planHandleForward;

}

clfftPlanHandle bake_3d_forward_32f(long N0, long N1, long N2, cl_context context, cl_command_queue commandQueue) {
  ENTEREXIT(1,"bake_3d_forward_32f");
  cl_int ret;
  // FFT library related declarations 
  clfftPlanHandle planHandleForward;
  clfftDim dim = CLFFT_3D;
  size_t clLengths[3] = {(size_t)N0, (size_t)N1, (size_t)N2};
  size_t inStride[3] = {1, (size_t)N0, (size_t)N0*(size_t)N1};
  // note each output row has N0/2+1 complex numbers 
  size_t outStride[3] = {1, (size_t)N0/2+1, ((size_t)N0/2+1)*(size_t)N1};

  // Create a default plan for a complex FFT.
  ret = clfftCreateDefaultPlan(&planHandleForward, context, dim, clLengths);
  CHECK(ret,"bake_3d_forward_32f create default plan",1);

  clfftPrecision precision = CLFFT_SINGLE;
  clfftLayout inLayout = CLFFT_REAL;
  clfftLayout outLayout = CLFFT_HERMITIAN_INTERLEAVED;
  clfftResultLocation resultLocation = CLFFT_OUTOFPLACE;
  
  // Set plan parameters. 
  ret = clfftSetPlanPrecision(planHandleForward, precision);
  //printf("clfft precision %d\n", ret);
  ret = clfftSetLayout(planHandleForward, inLayout, outLayout);
  //printf("clfft set layout real hermittian interveaved %d\n", ret);
  ret = clfftSetResultLocation(planHandleForward, resultLocation);
  //printf("clfft set result location %d\n", ret);
  ret=clfftSetPlanInStride(planHandleForward, dim, inStride);
  //printf("clfft set instride %d\n", ret);
  ret=clfftSetPlanOutStride(planHandleForward, dim, outStride);
  //printf("clfft set out stride %d\n", ret);

  // Bake the plan.
  ret = clfftBakePlan(planHandleForward, 1, &commandQueue, NULL, NULL);
  CHECK(ret,"bake_3d_forward_32f bake plan",1);
  
  ret = clFinish(commandQueue);
  CHECK(ret,"bake_3d_forward_32f finish",1);

  ENTEREXIT(0,"bake_3d_forward_32f");
  
  return planHandleForward;
}

clfftPlanHandle bake_2d_backward_32f(long N0, long N1, cl_context context, cl_command_queue commandQueue) {
  ENTEREXIT(1,"bake_2d_backward_32f");
  
  cl_int ret;

  // FFT library realted declarations 
  clfftPlanHandle planHandleBackward;
  clfftDim dim = CLFFT_2D;
  size_t clLengths[2] = {(size_t)N0, (size_t)N1};
  size_t inStride[3] = {1, (size_t)N0/2+1};
  // note each output row has N0/2+1 complex numbers 
  size_t outStride[3] = {1,(size_t)N0};

  // Setup clFFT. 
  clfftSetupData fftSetup;
  ret = clfftInitSetupData(&fftSetup);
  printf("clfft init %d\n", ret);
  ret = clfftSetup(&fftSetup);
  printf("clfft setup %d\n", ret);
  
  // Create a default plan for a complex FFT. 
  ret = clfftCreateDefaultPlan(&planHandleBackward, context, dim, clLengths);
  CHECK(ret,"bake_2d_backward_32f create default plan",1);
  
  // Set plan parameters. 
  ret = clfftSetPlanPrecision(planHandleBackward, CLFFT_SINGLE);
  //printf("clfft precision %d\n", ret);
  ret = clfftSetLayout(planHandleBackward, CLFFT_HERMITIAN_INTERLEAVED, CLFFT_REAL);
  //printf("clfft set layout real hermittian interveaved %d\n", ret);
  ret = clfftSetResultLocation(planHandleBackward, CLFFT_OUTOFPLACE);
  //printf("clfft set result location %d\n", ret);
  ret=clfftSetPlanInStride(planHandleBackward, dim, inStride);
  //printf("clfft set instride %d\n", ret);
  ret=clfftSetPlanOutStride(planHandleBackward, dim, outStride);
  //printf("clfft set out stride %d\n", ret);

  // Bake the plan.
  ret = clfftBakePlan(planHandleBackward, 1, &commandQueue, NULL, NULL);
  CHECK(ret,"bake_2d_backward_32f bake plan",1);

  ret = clFinish(commandQueue);
  CHECK(ret,"bake_2d_backward_32f finish",1);

  return planHandleBackward;

}

clfftPlanHandle bake_3d_backward_32f(long N0, long N1, long N2, cl_context context, cl_command_queue commandQueue) {

  ENTEREXIT(1,"bake_3d_backward_32f");

  cl_int ret;
  // FFT library related declarations 
  clfftPlanHandle planHandleBackward;
  clfftDim dim = CLFFT_3D;
  size_t clLengths[3] = {(size_t)N0, (size_t)N1, (size_t)N2};
  size_t inStride[3] = {1, (size_t)N0/2+1, ((size_t)N0/2+1)*(size_t)N1};
  // note each output row has N0/2+1 complex numbers 
  size_t outStride[3] = {1, (size_t)N0, (size_t)N0*(size_t)N1};

  // Create a default plan for a complex FFT.
  ret = clfftCreateDefaultPlan(&planHandleBackward, context, dim, clLengths);
  CHECK(ret,"bake_3d_backward_32f create default plan",1);

  clfftPrecision precision = CLFFT_SINGLE;
  clfftResultLocation resultLocation = CLFFT_OUTOFPLACE;
  
  // Set plan parameters. 
  ret = clfftSetPlanPrecision(planHandleBackward, precision);
  //printf("clfft precision %d\n", ret);
  ret = clfftSetLayout(planHandleBackward, CLFFT_HERMITIAN_INTERLEAVED, CLFFT_REAL);
  //printf("clfft set layout real hermittian interveaved %d\n", ret);
  ret = clfftSetResultLocation(planHandleBackward, CLFFT_OUTOFPLACE);
  //printf("clfft set result location %d\n", ret);
  ret=clfftSetPlanInStride(planHandleBackward, dim, inStride);
  //printf("clfft set instride %d\n", ret);
  ret=clfftSetPlanOutStride(planHandleBackward, dim, outStride);
  //printf("clfft set out stride %d\n", ret);

  // Bake the plan.
  ret = clfftBakePlan(planHandleBackward, 1, &commandQueue, NULL, NULL);
  CHECK(ret,"bake_3d_backward_32f bake plan",1);

  ret = clFinish(commandQueue);
  CHECK(ret,"bake_3d_backward_32f finish",1);

  ENTEREXIT(0,"bake_3d_backward_32f");
  
  return planHandleBackward;

}

int fft2d_32f_lp(long long N0, long long N1, long long d_image, long long d_out, long long l_context, long long l_queue) {
 
	// cast long to context 
	cl_context context = (cl_context)l_context;
  
	// cast long to queue 
	cl_command_queue commandQueue = (cl_command_queue)l_queue;  

  // number of elements in Hermitian (interleaved) output 
  unsigned long nFreq=N1*(N0/2+1);

  clfftPlanHandle planHandleForward = bake_2d_forward_32f(N0, N1, context, commandQueue); 

  cl_int ret = setupFFT();
  cl_mem cl_mem_image=(cl_mem)d_image;
  cl_mem cl_mem_out=(cl_mem)d_out;
  
  // Execute the plan. 
  ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &cl_mem_image, &cl_mem_out, NULL);
  CHECK(ret,"fft2d_32f_lp enqueue",1);
  
  ret = clFinish(commandQueue);
  CHECK(ret,"fft2d_32f_lp finish",1);
  
   // Release the plan. 
   ret = clfftDestroyPlan( &planHandleForward );

   clfftTeardown();

   return 0; 
}

int fft3d_32f_lp(long long N0, long long N1, long long N2, long long d_image, long long d_out, long long l_context, long long l_queue) {
  
	// cast long to context 
	cl_context context = (cl_context)l_context;
  
	// cast long to queue 
	cl_command_queue commandQueue = (cl_command_queue)l_queue;  

  // number of elements in Hermitian (interleaved) output 
  unsigned long nFreq=N2*N1*(N0/2+1);

  clfftPlanHandle planHandleForward = bake_3d_forward_32f(N0, N1, N2, context, commandQueue); 

  cl_int ret = setupFFT();
  cl_mem cl_mem_image=(cl_mem)d_image;
  cl_mem cl_mem_out=(cl_mem)d_out;
  
  // Execute the plan. 
  ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &cl_mem_image, &cl_mem_out, NULL);
  CHECK(ret,"fft3d_32f_lp enqueue",1);
  
  ret = clFinish(commandQueue);
  CHECK(ret,"fft3d_32f_lp finish",1);
  
   // Release the plan. 
   ret = clfftDestroyPlan( &planHandleForward );

   clfftTeardown();

   return 0; 
}

int fft2d_32f(size_t N0, size_t N1, float *h_image, float * h_out) {
 
  cl_platform_id platformId = NULL;
	cl_device_id deviceID = NULL;
	cl_uint retNumDevices;
	cl_uint retNumPlatforms;

  // create platform
  cl_int ret = clGetPlatformIDs(1, &platformId, &retNumPlatforms);
  CHECK(ret,"fft2d_32f platform id",1);

  // get device ids
	ret = clGetDeviceIDs(platformId, CL_DEVICE_TYPE_DEFAULT, 1, &deviceID, &retNumDevices);
  CHECK(ret,"fft2d_32f device id",1);

	// Creating context.
	cl_context context = clCreateContext(NULL, 1, &deviceID, NULL, NULL,  &ret);
  CHECK(ret,"fft2d_32f create context",1);

	// Creating command queue
	cl_command_queue commandQueue = clCreateCommandQueue(context, deviceID, 0, &ret);
  CHECK(ret,"fft2d_32f create commandqueue",1);
	
  // Memory buffers for each array
	cl_mem aMemObj = clCreateBuffer(context, CL_MEM_READ_WRITE, N1 * N0 * sizeof(float), NULL, &ret);
  CHECK(ret,"fft2d_32f create buffer",1);
	
   // Copy lists to memory buffers
	ret = clEnqueueWriteBuffer(commandQueue, aMemObj, CL_TRUE, 0, N1 * N0 * sizeof(float), h_image, 0, NULL, NULL);;
  CHECK(ret,"fft2d_32f copy buffer",1);

  // number of elements in Hermitian (interleaved) output 
  unsigned long nFreq=N1*(N0/2+1);

  // create output buffer (note each complex number is represented by 2 floats)
  cl_mem FFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2*nFreq*sizeof(float), NULL, &ret);
  CHECK(ret,"fft2d_32f create buffer",1);
	 
  ret = fft2d_32f_lp(N0, N1, (long long)aMemObj, (long long)FFT, (long long)context, (long long)commandQueue);
  CHECK(ret,"fft2d_32f call long pointer FFT",1);
  
  // transfer from device back to GPU
  ret = clEnqueueReadBuffer( commandQueue, FFT, CL_TRUE, 0, 2*nFreq*sizeof(float), h_out, 0, NULL, NULL );
  CHECK(ret,"fft2d_32f copy buffer",1);
  
  // Release OpenCL memory objects. 
  clReleaseMemObject( FFT );
  clReleaseMemObject( aMemObj);

  // Release OpenCL working objects.
  clReleaseCommandQueue( commandQueue );
  clReleaseContext( context );
 
  return 0; 
}

int fft2dinv_32f_lp(long long N0, long long N1, long long d_fft, long long d_out, long long l_context, long long l_queue) {
 
	// cast long long to context 
	cl_context context = (cl_context)l_context;
  
	// cast long long to queue 
	cl_command_queue commandQueue = (cl_command_queue)l_queue;

  cl_int ret = setupFFT();
  cl_mem cl_mem_image=(cl_mem)d_fft;
  cl_mem cl_mem_out=(cl_mem)d_out;
 
  clfftPlanHandle planHandleBackward = bake_2d_backward_32f(N0, N1, context, commandQueue); 
  
  // number of elements in Hermitian (interleaved) output 
  unsigned long nFreq=N1*(N0/2+1);

  // Execute the plan.
  ret = clfftEnqueueTransform(planHandleBackward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &cl_mem_image, &cl_mem_out, NULL);
  CHECK(ret,"fft2dinv_32f_lp enqueue transform",1);

  ret = clFinish(commandQueue);
  CHECK(ret,"fft2dinv_32f_lp finish",1);
 
  // Release the plan. 
  ret = clfftDestroyPlan( &planHandleBackward);

  clfftTeardown();
   
  return ret; 
}


int fft3dinv_32f_lp(long long N0, long long N1, long long N2, long long d_fft, long long d_out, long long l_context, long long l_queue) {
 
	// cast long long to context 
	cl_context context = (cl_context)l_context;
  
	// cast long long to queue 
	cl_command_queue commandQueue = (cl_command_queue)l_queue;

  cl_int ret = setupFFT();
  cl_mem cl_mem_image=(cl_mem)d_fft;
  cl_mem cl_mem_out=(cl_mem)d_out;
 
  clfftPlanHandle planHandleBackward = bake_3d_backward_32f(N0, N1, N2, context, commandQueue); 
  
  // number of elements in Hermitian (interleaved) output 
  unsigned long nFreq=N2*N1*(N0/2+1);

  // Execute the plan.
  ret = clfftEnqueueTransform(planHandleBackward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &cl_mem_image, &cl_mem_out, NULL);
  CHECK(ret,"fft3dinv_32f_lp enqueue transform",1);

  ret = clFinish(commandQueue);
  CHECK(ret,"fft3dinv_32f_lp finish",1);
 
  // Release the plan. 
  ret = clfftDestroyPlan( &planHandleBackward);

  clfftTeardown();

  return ret; 
}

/*
Inverse complex to real FFT 

N0 - real width
N1 - real height
h_fft - a complex Hermitian interleaved FFT of size (N0/2+1) by N1 
h_out - a (contiguous) N0 by N1 float array
*/
int fftinv2d_32f(size_t N0, size_t N1, float *h_fft, float * h_out) {
 
  cl_platform_id platformId = NULL;
	cl_device_id deviceID = NULL;
	cl_uint retNumDevices;
	cl_uint retNumPlatforms;
  cl_int ret = clGetPlatformIDs(1, &platformId, &retNumPlatforms);

  printf("\ncreated platform\n"); 

	ret = clGetDeviceIDs(platformId, CL_DEVICE_TYPE_DEFAULT, 1, &deviceID, &retNumDevices);
  CHECK(ret,"fftinv2d_32f get device id",1);

	// Creating context.
	cl_context context = clCreateContext(NULL, 1, &deviceID, NULL, NULL,  &ret);
  CHECK(ret,"fftinv2d_32f create context",1);

	// Creating command queue
	cl_command_queue commandQueue = clCreateCommandQueue(context, deviceID, 0, &ret);
  CHECK(ret,"fftinv2d_32f create command queue",1);


  // number of elements in Hermitian (interleaved) output 
  unsigned long nFreq = (N0/2+1)*N1;
	
  // declare FFT memory on GPU
	cl_mem d_FFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2 *nFreq * sizeof(float), NULL, &ret);
  CHECK(ret,"fftinv2d_32f create buffer",1);

   // Copy fft to GPU
	ret = clEnqueueWriteBuffer(commandQueue, d_FFT, CL_TRUE, 0, 2 * nFreq * sizeof(float), h_fft, 0, NULL, NULL);;
  CHECK(ret,"fftinv2d_32f enqueue buffer",1);

  // create output buffer 
  cl_mem out = clCreateBuffer(context, CL_MEM_READ_WRITE, N0*N1*sizeof(float), NULL, &ret);
  CHECK(ret,"fftinv2d_32f create buffer",1);

  fft2dinv_32f_lp(N0, N1, (long long)d_FFT, (long long)out, (long long)context, (long long)commandQueue);
  
  // transfer from device back to GPU
  ret = clEnqueueReadBuffer( commandQueue, out, CL_TRUE, 0, N0*N1*sizeof(float), h_out, 0, NULL, NULL );

  // Release OpenCL memory objects. 
  
  clReleaseMemObject( d_FFT );
  clReleaseMemObject( out );

  // Release clFFT library. 
  clfftTeardown( );

  // Release OpenCL working objects.
  clReleaseCommandQueue( commandQueue );
  clReleaseContext( context );
 
  return 0; 

}

int conv3d_32f_lp(size_t N0, size_t N1, size_t N2, long long l_image, long long l_psf,  long long l_output, bool correlate, long long l_context, long long l_queue, long long l_device) {

  ENTEREXIT(1,"conv32_32f_lp");

  cl_int ret;

  // most of the inputs are long long pointers we'll need to cast them to the right cl types

	// cast long long to context 
	cl_context context = (cl_context)l_context;
  
	// cast long long to queue 
	cl_command_queue commandQueue = (cl_command_queue)l_queue;
	
  // and long long to deviceID
  cl_device_id deviceID = (cl_device_id)l_device;
  
  // cast long long pointers to cl_mem 
	cl_mem d_image = (cl_mem)l_image;
	cl_mem d_psf =  (cl_mem)l_psf;
	cl_mem d_output = (cl_mem)l_output;

  // size in spatial domain
  unsigned long n = N0*N1*N2;

  // size in frequency domain
  unsigned long nFreq=(N0/2+1)*N1*N2;
 
  // create memory for FFT of estimate and PSF 
	cl_mem imageFFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2*nFreq * sizeof(float), NULL, &ret);
  CHECKRETURN(ret,"conv3d_32f_lp estimate fft createbuffer",1);
 
  cl_mem psfFFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2*nFreq * sizeof(float), NULL, &ret);
  CHECKRETURN(ret,"conv3d_32f_lp psf fft createbuffer",1);
		
  // Create program from kernel source
	cl_program program = clCreateProgramWithSource(context, 1, (const char **)&programString, NULL, &ret);	
  CHECKRETURN(ret,"conv3d_32f_lp createprogram",1);

	// Build opencl program
	ret = clBuildProgram(program, 1, &deviceID, NULL, NULL, NULL);
  CHECKRETURN(ret,"conv3d_32f_lp buildprogram",1);

  if (ret!=0) {
    return ret;
  }

	// Create complex multiply kernel
  cl_kernel kernel;

  if (correlate==false) {
	  kernel = clCreateKernel(program, "vecComplexMultiply", &ret);
  } else {
    kernel = clCreateKernel(program, "vecComplexConjugateMultiply", &ret);
  }
  CHECKRETURN(ret,"conv3d_32f_lp createkernel",1);

  clfftPlanHandle planHandleForward=bake_3d_forward_32f(N0, N1, N2, context, commandQueue);
  clfftPlanHandle planHandleBackward=bake_3d_backward_32f(N0, N1, N2, context, commandQueue);
  
  // compute item sizes 
  size_t localItemSize=64;
	size_t globalItemSize= ceil((N2*N1*N0)/(float)localItemSize)*localItemSize;
	size_t globalItemSizeFreq = ceil((nFreq)/(float)localItemSize)*localItemSize;
  //printf("nFreq %lu glbalItemSizeFreq %lu\n",nFreq, globalItemSizeFreq);
 
  // FFT of PSF
  ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &d_psf, &psfFFT, NULL);
  CHECKRETURN(ret,"conv3d_32f_lp fft psf",1);
  
  // FFT of image
  ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &d_image, &imageFFT, NULL);
  CHECKRETURN(ret,"conv3d_32f_lp fft image",1);

  // complex multipy image FFT and PSF FFT
  ret = callKernel(kernel, imageFFT, psfFFT, imageFFT, nFreq, commandQueue, globalItemSizeFreq, localItemSize);
  CHECKRETURN(ret,"conv3d_32f_lp complex multiply",1);
  
  // Inverse to get convolved
  ret = clfftEnqueueTransform(planHandleBackward, CLFFT_BACKWARD, 1, &commandQueue, 0, NULL, NULL, &imageFFT, &d_output, NULL);
  CHECKRETURN(ret,"conv3d_32f_lp inverse fft",1);
 
  // Release OpenCL memory objects. 
  clReleaseMemObject( psfFFT );
  clReleaseMemObject( imageFFT );

   // Release the plan. 
   ret = clfftDestroyPlan( &planHandleForward);
   ret = clfftDestroyPlan( &planHandleBackward );
   
   // release kernels
   clReleaseKernel(kernel);
   
   // release program
   ret = clReleaseProgram(program);

   // Release clFFT library. 
   clfftTeardown( );

  ENTEREXIT(0,"conv32_32f_lp");
  
  return ret;
}

int conv3d_32f(size_t N0, size_t N1, size_t N2, float *h_image, float *h_psf, float *h_out, int platformIndex, int deviceIndex) {
  return convcorr3d_32f(N0, N1, N2, h_image, h_psf, h_out, 0, platformIndex, deviceIndex);
}

int convcorr3d_32f(size_t N0, size_t N1, size_t N2, float *h_image, float *h_psf, float *h_out, bool correlate, int platformIndex, int deviceIndex) {

  ENTEREXIT(1,"convcorr3d_32f");

  cl_platform_id *platformIds = new cl_platform_id[MAXPLATFORMS];
	cl_uint retNumPlatforms;
  cl_int ret = clGetPlatformIDs(MAXPLATFORMS, platformIds, &retNumPlatforms);
  CHECKRETURN(ret, "deconv3d_32f_tf getPlatformIDs", 1);
  char * platformName = new char[1000];
  clGetPlatformInfo(platformIds[platformIndex], CL_PLATFORM_NAME, 1000, platformName, NULL);
  
  cl_context_properties properties[] =
  {
    CL_CONTEXT_PLATFORM, (cl_context_properties)platformIds[platformIndex],
    0 // signals end of property list
  };
  
  cl_device_id *deviceIDs = new cl_device_id[MAXDEVICESPERPLATFORM];
	cl_uint retNumDevices;

  ret = clGetDeviceIDs(platformIds[platformIndex], CL_DEVICE_TYPE_ALL, MAXDEVICESPERPLATFORM, deviceIDs, &retNumDevices);
  
  char * deviceName = new char[1000];
  clGetDeviceInfo(deviceIDs[0], CL_DEVICE_NAME, 1000, deviceName, NULL);
  std::cout<<std::flush;
  CHECKRETURN(ret, "deconv3d_32f_tf getDeviceIDs", 1);
  
  // Creating context.
	cl_context context = clCreateContext(properties, 1, &deviceIDs[deviceIndex], NULL, NULL,  &ret);
  CHECKRETURN(ret, "deconv3d_32f_tf createContext", 1);

	// Creating command queue
	cl_command_queue commandQueue = clCreateCommandQueue(context, deviceIDs[deviceIndex], 0, &ret);
  CHECKRETURN(ret, "deconv3d_32f_tf createCommandQueue", 1);

  // Memory buffers for each array
	cl_mem d_image = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  CHECKRETURN(ret,"convcorr3d_32f gpu image createbuffer",1);
	cl_mem d_psf = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  CHECKRETURN(ret,"convcorr3d_32f gpu psf createbuffer",1);
	cl_mem d_out = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  CHECKRETURN(ret,"convcorr3d_32f gpu out createbuffer",1);

   // Copy to memory buffers
	ret = clEnqueueWriteBuffer(commandQueue, d_image, CL_TRUE, 0, N2*N1*N0 * sizeof(float), h_image, 0, NULL, NULL);;
  CHECKRETURN(ret,"convcorr3d_32f copy image",1);
	ret = clEnqueueWriteBuffer(commandQueue, d_psf, CL_TRUE, 0, N2*N1*N0 * sizeof(float), h_psf, 0, NULL, NULL);
  CHECKRETURN(ret,"convcorr3d_32f copy PSF",1);

  conv3d_32f_lp(N0, N1, N2, (long long)d_image, (long long)d_psf, (long long)d_out, correlate, (long long)context, (long long)commandQueue, (long long)deviceIDs[deviceIndex]);

  // copy back to host 
  ret = clEnqueueReadBuffer( commandQueue, d_out, CL_TRUE, 0, N0*N1*N2*sizeof(float), h_out, 0, NULL, NULL );
 
  // Release OpenCL memory objects. 
  clReleaseMemObject( d_image);
  clReleaseMemObject( d_psf);
  clReleaseMemObject( d_out);

  // Release OpenCL working objects.
  clReleaseCommandQueue( commandQueue );
  clReleaseContext( context );
  
  delete platformIds;
  delete deviceIDs;
 
  ENTEREXIT(0,"convcorr3d_32f");

  return 0;
}

int deconv3d_32f_lp(int iterations, size_t N0, size_t N1, size_t N2, long long l_observed, long long l_psf, long long l_estimate, long long l_normal, long long l_context, long long l_queue, long long l_device) {

  return deconv3d_32f_lp_tv(iterations, 0., N0, N1, N2, l_observed, l_psf, l_estimate, l_normal, l_context, l_queue, l_device);  

}

int deconv3d_32f_lp_tv(int iterations, float regularizationFactor, size_t N0, size_t N1, size_t N2, long long l_observed, long long l_psf, long long l_estimate, long long l_normal, long long l_context, long long l_queue, long long l_device) {
  ENTEREXIT(1,"deconv3d_32f_lp_tf");

  cl_int ret;
  
  bool tv=false;

  if (regularizationFactor>0) {
    tv=true;
  }
	
  // cast long long to context 
	cl_context context = (cl_context)l_context;
  
	// cast long long to queue 
	cl_command_queue commandQueue = (cl_command_queue)l_queue;
	
  // cast long long pointers to cl_mem 
	cl_mem d_observed = (cl_mem)l_observed;
	cl_mem d_psf =  (cl_mem)l_psf;
	cl_mem d_estimate = (cl_mem)l_estimate; 

  cl_mem d_normal = NULL;

  if (l_normal!=0) {
    d_normal = (cl_mem)l_normal;
  }

  cl_device_id deviceID = (cl_device_id)l_device;

  // size in spatial domain
  unsigned long n = N0*N1*N2;

  // size in frequency domain
  unsigned long nFreq=(N0/2+1)*N1*N2;

  // create memory for reblurred 	
  cl_mem d_reblurred = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  CHECKRETURN(ret, "deconv3d_32f_lp_tv create reblurred", 1);
 
  // create memory for FFT of estimate and PSF 
	cl_mem estimateFFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2*nFreq * sizeof(float), NULL, &ret);
  CHECKRETURN(ret, "deconv3d_32f_lp_tv create buffer psf", 1);
 
  cl_mem psfFFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2*nFreq * sizeof(float), NULL, &ret);
  CHECKRETURN(ret, "deconv3d_32f_lp_tv create buffer object", 1);

  cl_mem d_variation;

  if (tv==true) {
    d_variation = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  }
  else {
    d_variation = NULL;
  }
	
  // Create kernels 	
  // Create program from kernel source
	cl_program program = clCreateProgramWithSource(context, 1, (const char **)&programString, NULL, &ret);	

	// Build opencl program
	ret = clBuildProgram(program, 1, &deviceID, NULL, NULL, NULL);
  CHECKRETURN(ret, "deconv3d_32f_lp_tv create program", 1);

  if (ret!=0) {
    return ret;
  }

	// Create complex multiply kernel
	cl_kernel kernelComplexMultiply = clCreateKernel(program, "vecComplexMultiply", &ret);
  CHECKRETURN(ret, "deconv3d_32f_lp_tv create complex multiply kernel", 1);
 
 	// Create complex conjugate multiply kernel
	cl_kernel kernelComplexConjugateMultiply = clCreateKernel(program, "vecComplexConjugateMultiply", &ret);
  CHECKRETURN(ret, "deconv3d_32f_lp_tv create complex conjugate multiply kernel", 1);
 	
  // Create divide kernel
	cl_kernel kernelDiv = clCreateKernel(program, "vecDiv", &ret);
  CHECKRETURN(ret, "deconv3d_32f_lp_tv create divide kernel", 1);
 
  // Create multiply kernel
	cl_kernel kernelMul = clCreateKernel(program, "vecMul", &ret);
  CHECKRETURN(ret, "deconv3d_32f_lp_tv create multiply kernel", 1);

  // Create remove small values kernel
	cl_kernel kernelRemoveSmallValues = clCreateKernel(program, "removeSmallValues", &ret);
  CHECKRETURN(ret, "deconv3d_32f_lp_tv create remove small values kernel", 1);

  cl_kernel kernelTV;

  if (tv==true) {
   
    cl_program program2 = makeProgram(context, deviceID, __cle_totalvariationterm_h);
    kernelTV = clCreateKernel(program2, "totalVariationTerm", &ret);
    CHECKRETURN(ret, "deconv3d_32f_lp_tv create total variation kernel", 1);
    clReleaseProgram(program2);
    //free(program_str);
  }
  else {
    kernelTV=NULL;
  }

  clfftPlanHandle planHandleForward=bake_3d_forward_32f(N0, N1, N2, context, commandQueue);
  
  clfftPlanHandle planHandleBackward=bake_3d_backward_32f(N0, N1, N2, context, commandQueue);

  // compute item sizes 
  size_t localItemSize=64;
	size_t globalItemSize= ceil((N2*N1*N0)/(float)localItemSize)*localItemSize;
	size_t globalItemSizeFreq = ceil((nFreq+1000)/(float)localItemSize)*localItemSize;
  //printf("nFreq %lu glbalItemSizeFreq %lu\n",nFreq, globalItemSizeFreq);
  
   // FFT of PSF
  ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &d_psf, &psfFFT, NULL);

  if (d_normal!=NULL) {
    ret = callInPlaceKernel(kernelRemoveSmallValues, d_normal, n, commandQueue, globalItemSize, localItemSize);
  }

  printf("\nRichardson Lucy Started");
  for (int i=0;i<iterations;i++) {
      // FFT of estimate
      ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &d_estimate, &estimateFFT, NULL);
      CHECKRETURN(ret, "deconv3d_32f_lp_tv FFT Estimate", 1);

      // complex multipy estimate FFT and PSF FFT
      ret = callKernel(kernelComplexMultiply, estimateFFT, psfFFT, estimateFFT, nFreq, commandQueue, globalItemSizeFreq, localItemSize);
      CHECKRETURN(ret, "deconv3d_32f_lp_tv kernel complex multiply", 1);
      
      // Inverse to get reblurred
      ret = clfftEnqueueTransform(planHandleBackward, CLFFT_BACKWARD, 1, &commandQueue, 0, NULL, NULL, &estimateFFT, &d_reblurred, NULL);
      CHECKRETURN(ret, "deconv3d_32f_lp_tv inverse FFT", 1);
      
      // divide observed by reblurred
      ret = callKernel(kernelDiv, d_observed, d_reblurred, d_reblurred, n, commandQueue, globalItemSize, localItemSize);
      CHECKRETURN(ret, "deconv3d_32f_lp_tv divide observed by reblurred", 1);
     
      // FFT of observed/reblurred 
      ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &d_reblurred, &estimateFFT, NULL);
      CHECKRETURN(ret, "deconv3d_32f_lp_tv FFT observed/reblurred", 1);
      
      // Correlate above result with PSF 
      ret = callKernel(kernelComplexConjugateMultiply, estimateFFT, psfFFT, estimateFFT, nFreq, commandQueue, globalItemSizeFreq, localItemSize);
      CHECKRETURN(ret, "deconv3d_32f_lp_tv correlate", 1);
      
      // Inverse FFT to get update factor 
      ret = clfftEnqueueTransform(planHandleBackward, CLFFT_BACKWARD, 1, &commandQueue, 0, NULL, NULL, &estimateFFT, &d_reblurred, NULL);
      CHECKRETURN(ret, "deconv3d_32f_lp_tv inverse FFT to get update factor", 1);
     
      // if using total variation multiply by variation factor
      if (tv) {
        ret = callVariationKernel(kernelTV, d_estimate, d_reblurred, d_variation, N0, N1, N2, 1.0, 1.0, 3.0, regularizationFactor, commandQueue, globalItemSize, localItemSize);
        CHECKRETURN(ret, "deconv3d_32f_lp_tv variation kernel", 1);

        ret = callKernel(kernelMul, d_estimate, d_variation, d_estimate, n, commandQueue, globalItemSize, localItemSize);
        CHECKRETURN(ret, "deconv3d_32f_lp_tv multiply", 1);
      }
      else {
        // multiply estimate by update factor 
        ret = callKernel(kernelMul, d_estimate, d_reblurred, d_estimate, n, commandQueue, globalItemSize, localItemSize);
        CHECKRETURN(ret, "deconv3d_32f_lp_tv multiply", 1);
      }
 
      if (d_normal!=NULL) {
        // divide estimate by normal
        ret = callKernel(kernelDiv, d_estimate, d_normal, d_estimate, n, commandQueue, globalItemSize, localItemSize);
        CHECKRETURN(ret, "deconv3d_32f_lp_tv divide by normal", 1);
      }      

      ret = clFinish(commandQueue);

      if (i==0) {
        printf("\n");
      }
      
      if (remainder(i,10)==0) {
        printf("%d ",i);
        std::cout<<std::flush;
      }
  } 

  printf("\nRichardson Lucy Finished\n");
  std::cout<<std::flush;

  // Release OpenCL memory objects. 
  clReleaseMemObject( d_reblurred);
  clReleaseMemObject( psfFFT );
  clReleaseMemObject( estimateFFT );

  if (tv==true) {
    clReleaseMemObject(d_variation);
  }

   // Release the plan. 
   ret = clfftDestroyPlan( &planHandleForward );
   ret = clfftDestroyPlan( &planHandleBackward );

  clReleaseKernel(kernelComplexMultiply);
  clReleaseKernel(kernelComplexConjugateMultiply);
  clReleaseKernel(kernelDiv);
  clReleaseKernel(kernelMul);
  clReleaseKernel(kernelRemoveSmallValues);

  clReleaseProgram(program);

  if (tv==true) {
    clReleaseKernel(kernelTV);
  }

   // Release clFFT library. 
   clfftTeardown( );

   ENTEREXIT(0,"deconv3d_32f_lp_tf");
   return 0;

}

int deconv3d_32f(int iterations, size_t N0, size_t N1, size_t N2, float *h_image, float *h_psf, float *h_out, float * normal, int platformIndex, int deviceIndex) {

  return deconv3d_32f_tv(iterations, 0.0, N0, N1,N2, h_image, h_psf, h_out, normal, platformIndex, deviceIndex);

}

int print_platforms_and_devices() {

  cl_platform_id *platformId = new cl_platform_id[MAXPLATFORMS];
	cl_uint retNumDevices;
	cl_uint retNumPlatforms;

  cl_int ret = clGetPlatformIDs(MAXPLATFORMS, platformId, &retNumPlatforms);
  CHECKRETURN(ret, "deconv3d_32f_tf getPlatformIDs", 1);

  for (int i=0;i<retNumPlatforms;i++) {
      char * platformName = new char[1000];
      clGetPlatformInfo(platformId[i], CL_PLATFORM_NAME, 1000, platformName, NULL);
      printf("platform %d %s\n",i, platformName);
                                                 
	    cl_device_id *deviceIDs = new cl_device_id[MAXDEVICESPERPLATFORM];

      ret = clGetDeviceIDs(platformId[i], CL_DEVICE_TYPE_ALL, MAXDEVICESPERPLATFORM, deviceIDs, &retNumDevices);
      
      for (int j=0;j<retNumDevices;j++) {
        char * deviceName = new char[1000];
        clGetDeviceInfo(deviceIDs[j], CL_DEVICE_NAME, 1000, deviceName, NULL);
        printf("     device name %d %s\n", j, deviceName);
        std::cout<<std::flush;
        delete deviceName;
      }
      printf("\n");

      delete platformName;
      delete deviceIDs;

      std::cout<<std::flush;
      CHECKRETURN(ret, "deconv3d_32f_tf getDeviceIDs", 1);
  }
 

  return 0;
}

int deconv3d_32f_tv(int iterations, float regularizationFactor, size_t N0, size_t N1, size_t N2, float *h_image, float *h_psf, float *h_out, float * h_normal, int platformIndex, int deviceIndex) {

  ENTEREXIT(1, "deconv3d_32f_tv");

  cl_platform_id *platformIds = new cl_platform_id[MAXPLATFORMS];
	cl_uint retNumPlatforms;
  cl_int ret = clGetPlatformIDs(MAXPLATFORMS, platformIds, &retNumPlatforms);
  CHECKRETURN(ret, "deconv3d_32f_tf getPlatformIDs", 1);
  char * platformName = new char[1000];
  clGetPlatformInfo(platformIds[platformIndex], CL_PLATFORM_NAME, 1000, platformName, NULL);
  printf("\nplatform %d %s\n", platformIndex, platformName);
  
  cl_context_properties properties[] =
  {
    CL_CONTEXT_PLATFORM, (cl_context_properties)platformIds[platformIndex],
    0 // signals end of property list
  };
  
  cl_device_id *deviceIDs = new cl_device_id[MAXDEVICESPERPLATFORM];
	cl_uint retNumDevices;

  ret = clGetDeviceIDs(platformIds[platformIndex], CL_DEVICE_TYPE_ALL, MAXDEVICESPERPLATFORM, deviceIDs, &retNumDevices);
  
  char * deviceName = new char[1000];
  clGetDeviceInfo(deviceIDs[0], CL_DEVICE_NAME, 1000, deviceName, NULL);
  printf("device name %d %s\n", deviceIndex, deviceName);
  std::cout<<std::flush;
  CHECKRETURN(ret, "deconv3d_32f_tf getDeviceIDs", 1);
  
  // Creating context.
	cl_context context = clCreateContext(properties, 1, &deviceIDs[deviceIndex], NULL, NULL,  &ret);
  CHECKRETURN(ret, "deconv3d_32f_tf createContext", 1);

	// Creating command queue
	cl_command_queue commandQueue = clCreateCommandQueue(context, deviceIDs[deviceIndex], 0, &ret);
  CHECKRETURN(ret, "deconv3d_32f_tf createCommandQueue", 1);
  
  // create device memory buffers for each array
	cl_mem d_observed = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
	cl_mem d_psf = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
	cl_mem d_estimate = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
	cl_mem d_normal = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
 
  CHECKRETURN(ret, "deconv3d_32f_tf create buffer", 1);

  // Copy lists to memory buffers
	ret = clEnqueueWriteBuffer(commandQueue, d_observed, CL_TRUE, 0, N2*N1*N0 * sizeof(float), h_image, 0, NULL, NULL);;
  CHECKRETURN(ret, "deconv3d_32f_tf copy observed", 1);
	ret = clEnqueueWriteBuffer(commandQueue, d_psf, CL_TRUE, 0, N2*N1*N0 * sizeof(float), h_psf, 0, NULL, NULL);
  CHECKRETURN(ret, "deconv3d_32f_tf copy psf", 1);
	ret = clEnqueueWriteBuffer(commandQueue, d_estimate, CL_TRUE, 0, N2*N1*N0 * sizeof(float), h_out, 0, NULL, NULL);
  CHECKRETURN(ret, "deconv3d_32f_tf copy estimate", 1);
	ret = clEnqueueWriteBuffer(commandQueue, d_normal, CL_TRUE, 0, N2*N1*N0 * sizeof(float), h_normal, 0, NULL, NULL);
  CHECKRETURN(ret, "deconv3d_32f_tf copy normal", 1);

  unsigned long n = N0*N1*N2;
  unsigned long nFreq=(N0/2+1)*N1*N2;
     
  deconv3d_32f_lp_tv(iterations, regularizationFactor, N0, N1, N2, (long long)d_observed, (long long)d_psf, (long long)d_estimate, (long long)d_normal, (long long)context, (long long)commandQueue, (long long)deviceIDs[deviceIndex]); 
    
  // copy back to host 
  ret = clEnqueueReadBuffer( commandQueue, d_estimate, CL_TRUE, 0, N0*N1*N2*sizeof(float), h_out, 0, NULL, NULL );

  cleanup3:
    // Release OpenCL memory objects. 
    clReleaseMemObject( d_estimate);
    clReleaseMemObject( d_observed );
    clReleaseMemObject( d_psf);
    clReleaseMemObject( d_normal);
  cleanup2:
    // Release OpenCL working objects.
    clReleaseCommandQueue( commandQueue );
  cleanup1:
    clReleaseContext( context );

  delete platformIds;
  delete deviceIDs;

  ENTEREXIT(0, "deconv3d_32f_tv");
  
  return ret;
}

int diagnostic() {

  char buff[FILENAME_MAX];
  GetCurrentDir( buff, FILENAME_MAX );
  std::cout<<"Current working dir: "<<buff<<"\n"<<std::flush;

  std::cout<<"diagnostic\n"<<std::flush;

  const char * fileName = "./lib/totalvariationterm.cl";
  
  size_t sizer=getFileSize(fileName);
  
  std::cout<<"size is "<<sizer<<"\n"<<std::flush;

  char * program_str = (char*)malloc(sizer);

  getProgramFromFile(fileName, program_str, sizer);

  std::cout<<program_str<<"\n"<<std::flush;

  return 0;
 
}
