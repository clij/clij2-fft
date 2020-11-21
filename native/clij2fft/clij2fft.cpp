#include <stdio.h>
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

#define MAX_SOURCE_SIZE (0x100000)

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
"     if (true)  {                                               \n" \
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
 


                                                                "\n" ;

cl_int callKernel(cl_kernel kernel, cl_mem in1, cl_mem in2, cl_mem out, const unsigned int n, cl_command_queue commandQueue, size_t globalItemSize, size_t localItemSize) {
   // Set arguments for kernel
	cl_int ret = clSetKernelArg(kernel, 0, sizeof(cl_mem), (void *)&in1);

  if (ret!=0) {	
    printf("\nset variable 1 %d\n", ret);
    return ret;
  }

	ret = clSetKernelArg(kernel, 1, sizeof(cl_mem), (void *)&in2);	
   
  if (ret!=0) {	
    printf("\nset variable 2 %d\n", ret);
    return ret;
  }

	ret = clSetKernelArg(kernel, 2, sizeof(cl_mem), (void *)&out);	
  if (ret!=0) {	
    printf("\nset variable 3 %d\n", ret);
    return ret;
  }

  ret = clSetKernelArg(kernel, 3, sizeof(unsigned int), &n);	
  
  if (ret!=0) {	
    printf("\nset variable 4 %d\n", ret);
    return ret;
  }
  
  ret = clEnqueueNDRangeKernel(commandQueue, kernel, 1, NULL, &globalItemSize, &localItemSize, 0, NULL, NULL);	
  
  if (ret!=0) {	
    printf("\nEnqueue Kernel %d\n", ret);
    return ret;
  }

  ret = clFinish(commandQueue);

  return ret;
}

cl_int setupFFT() {
   // Setup clFFT
  clfftSetupData fftSetup;
  cl_int ret = clfftInitSetupData(&fftSetup);
  printf("clfft init %d\n", ret);
  ret = clfftSetup(&fftSetup);

  return ret;
}

clfftPlanHandle bake_2d_forward_32f(long N0, long N1, cl_context context, cl_command_queue commandQueue) {

  cl_int ret;
  // FFT library related declarations 
  clfftPlanHandle planHandleForward;
  clfftDim dim = CLFFT_2D;
  size_t clLengths[2] = {N0, N1};
  size_t inStride[3] = {1, N0};
  // note each output row has N0/2+1 complex numbers 
  size_t outStride[3] = {1,N0/2+1};

  printf("clfft setup %d\n", ret);
  // Create a default plan for a complex FFT.
  ret = clfftCreateDefaultPlan(&planHandleForward, context, dim, clLengths);

  printf("Create Default Plan %d\n", ret);

  clfftPrecision precision = CLFFT_SINGLE;
  clfftLayout inLayout = CLFFT_REAL;
  clfftLayout outLayout = CLFFT_HERMITIAN_INTERLEAVED;
  clfftResultLocation resultLocation = CLFFT_OUTOFPLACE;
  
  // Set plan parameters. 
  ret = clfftSetPlanPrecision(planHandleForward, precision);
  printf("clfft precision %d\n", ret);
  ret = clfftSetLayout(planHandleForward, inLayout, outLayout);
  printf("clfft set layout real hermittian interveaved %d\n", ret);
  ret = clfftSetResultLocation(planHandleForward, resultLocation);
  printf("clfft set result location %d\n", ret);
  ret=clfftSetPlanInStride(planHandleForward, dim, inStride);
  printf("clfft set instride %d\n", ret);
  ret=clfftSetPlanOutStride(planHandleForward, dim, outStride);
  printf("clfft set out stride %d\n", ret);

  // Bake the plan.
  ret = clfftBakePlan(planHandleForward, 1, &commandQueue, NULL, NULL);

  printf("Bake %d\n", ret);
  ret = clFinish(commandQueue);
  printf("Finish Command Queue %d\n", ret);

  return planHandleForward;

}

clfftPlanHandle bake_3d_forward_32f(long N0, long N1, long N2, cl_context context, cl_command_queue commandQueue) {

  cl_int ret;
  // FFT library related declarations 
  clfftPlanHandle planHandleForward;
  clfftDim dim = CLFFT_3D;
  size_t clLengths[3] = {N0, N1, N2};
  size_t inStride[3] = {1, N0, N0*N1};
  // note each output row has N0/2+1 complex numbers 
  size_t outStride[3] = {1, N0/2+1, (N0/2+1)*N1};

  printf("clfft setup %d\n", ret);
  // Create a default plan for a complex FFT.
  ret = clfftCreateDefaultPlan(&planHandleForward, context, dim, clLengths);

  printf("Create Default Plan %d\n", ret);

  clfftPrecision precision = CLFFT_SINGLE;
  clfftLayout inLayout = CLFFT_REAL;
  clfftLayout outLayout = CLFFT_HERMITIAN_INTERLEAVED;
  clfftResultLocation resultLocation = CLFFT_OUTOFPLACE;
  
  // Set plan parameters. 
  ret = clfftSetPlanPrecision(planHandleForward, precision);
  printf("clfft precision %d\n", ret);
  ret = clfftSetLayout(planHandleForward, inLayout, outLayout);
  printf("clfft set layout real hermittian interveaved %d\n", ret);
  ret = clfftSetResultLocation(planHandleForward, resultLocation);
  printf("clfft set result location %d\n", ret);
  ret=clfftSetPlanInStride(planHandleForward, dim, inStride);
  printf("clfft set instride %d\n", ret);
  ret=clfftSetPlanOutStride(planHandleForward, dim, outStride);
  printf("clfft set out stride %d\n", ret);

  // Bake the plan.
  ret = clfftBakePlan(planHandleForward, 1, &commandQueue, NULL, NULL);

  printf("Bake %d\n", ret);
  ret = clFinish(commandQueue);
  printf("Finish Command Queue %d\n", ret);

  return planHandleForward;

}

clfftPlanHandle bake_2d_backward_32f(long N0, long N1, cl_context context, cl_command_queue commandQueue) {
  cl_int ret;

  // FFT library realted declarations 
  clfftPlanHandle planHandleBackward;
  clfftDim dim = CLFFT_2D;
  size_t clLengths[2] = {N0, N1};
  size_t inStride[3] = {1, N0/2+1};
  // note each output row has N0/2+1 complex numbers 
  size_t outStride[3] = {1,N0};

  // Setup clFFT. 
  clfftSetupData fftSetup;
  ret = clfftInitSetupData(&fftSetup);
  printf("clfft init %d\n", ret);
  ret = clfftSetup(&fftSetup);

  printf("clfft setup %d\n", ret);
  
  // Create a default plan for a complex FFT. 
  ret = clfftCreateDefaultPlan(&planHandleBackward, context, dim, clLengths);

  printf("Create Default Plan %d\n", ret);
  
  // Set plan parameters. 
  ret = clfftSetPlanPrecision(planHandleBackward, CLFFT_SINGLE);
  printf("clfft precision %d\n", ret);
  ret = clfftSetLayout(planHandleBackward, CLFFT_HERMITIAN_INTERLEAVED, CLFFT_REAL);
  printf("clfft set layout real hermittian interveaved %d\n", ret);
  ret = clfftSetResultLocation(planHandleBackward, CLFFT_OUTOFPLACE);
  printf("clfft set result location %d\n", ret);
  ret=clfftSetPlanInStride(planHandleBackward, dim, inStride);
  printf("clfft set instride %d\n", ret);
  ret=clfftSetPlanOutStride(planHandleBackward, dim, outStride);
  printf("clfft set out stride %d\n", ret);

  // Bake the plan.
  ret = clfftBakePlan(planHandleBackward, 1, &commandQueue, NULL, NULL);

  printf("Bake %d\n", ret);
  ret = clFinish(commandQueue);
  printf("Finish Command Queue %d\n", ret);

  return planHandleBackward;

}

clfftPlanHandle bake_3d_backward_32f(long N0, long N1, long N2, cl_context context, cl_command_queue commandQueue) {

  cl_int ret;
  // FFT library related declarations 
  clfftPlanHandle planHandleBackward;
  clfftDim dim = CLFFT_3D;
  size_t clLengths[3] = {N0, N1, N2};
  size_t inStride[3] = {1, N0/2+1, (N0/2+1)*N1};
  // note each output row has N0/2+1 complex numbers 
  size_t outStride[3] = {1, N0, N0*N1};

  printf("clfft setup %d\n", ret);
  // Create a default plan for a complex FFT.
  ret = clfftCreateDefaultPlan(&planHandleBackward, context, dim, clLengths);

  printf("Create Default Plan %d\n", ret);

  clfftPrecision precision = CLFFT_SINGLE;
  clfftResultLocation resultLocation = CLFFT_OUTOFPLACE;
  
  // Set plan parameters. 
  ret = clfftSetPlanPrecision(planHandleBackward, precision);
  printf("clfft precision %d\n", ret);
  ret = clfftSetLayout(planHandleBackward, CLFFT_HERMITIAN_INTERLEAVED, CLFFT_REAL);
  printf("clfft set layout real hermittian interveaved %d\n", ret);
  ret = clfftSetResultLocation(planHandleBackward, CLFFT_OUTOFPLACE);
  printf("clfft set result location %d\n", ret);
  ret=clfftSetPlanInStride(planHandleBackward, dim, inStride);
  printf("clfft set instride %d\n", ret);
  ret=clfftSetPlanOutStride(planHandleBackward, dim, outStride);
  printf("clfft set out stride %d\n", ret);

  // Bake the plan.
  ret = clfftBakePlan(planHandleBackward, 1, &commandQueue, NULL, NULL);

  printf("Bake %d\n", ret);
  ret = clFinish(commandQueue);
  printf("Finish Command Queue %d\n", ret);

  return planHandleBackward;

}

int fft2d_32f_lp(long N0, long N1, long d_image, long d_out, long l_context, long l_queue) {
  printf("input address %ld", d_image);
  printf("input address %lu", (unsigned long)d_image);
 
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
  printf("Forward FFT %d\n", ret);
  
  ret = clFinish(commandQueue);
  printf("Finish Command Queue for forward FFT %d\n", ret);
  
   // Release the plan. 
   ret = clfftDestroyPlan( &planHandleForward );

   clfftTeardown();
   
   printf("FFT finished\n");

   return 0; 
}

int fft2d_32f(size_t N0, size_t N1, float *h_image, float * h_out) {
 
  cl_platform_id platformId = NULL;
	cl_device_id deviceID = NULL;
	cl_uint retNumDevices;
	cl_uint retNumPlatforms;

  // create platform
  cl_int ret = clGetPlatformIDs(1, &platformId, &retNumPlatforms);

  printf("\ncreated platform\n");

  // get device ids
	ret = clGetDeviceIDs(platformId, CL_DEVICE_TYPE_DEFAULT, 1, &deviceID, &retNumDevices);

	// Creating context.
	cl_context context = clCreateContext(NULL, 1, &deviceID, NULL, NULL,  &ret);

  printf("\ncreated context\n");

	// Creating command queue
	cl_command_queue commandQueue = clCreateCommandQueue(context, deviceID, 0, &ret);

  printf("\ncreated command queue\n");
	
  // Memory buffers for each array
	cl_mem aMemObj = clCreateBuffer(context, CL_MEM_READ_WRITE, N1 * N0 * sizeof(float), NULL, &ret);
  printf("\ncreate variable 1 %d\n", ret);
	
   // Copy lists to memory buffers
	ret = clEnqueueWriteBuffer(commandQueue, aMemObj, CL_TRUE, 0, N1 * N0 * sizeof(float), h_image, 0, NULL, NULL);;
  printf("\ncopy to GPU  %d\n", ret);

  // number of elements in Hermitian (interleaved) output 
  unsigned long nFreq=N1*(N0/2+1);

  // create output buffer (note each complex number is represented by 2 floats)
  cl_mem FFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2*nFreq*sizeof(float), NULL, &ret);
  printf("\ncreate FFT %d\n", ret);
	 
  ret = fft2d_32f_lp(N0, N1, (long)aMemObj, (long)FFT, (long)context, (long)commandQueue);
  printf("FFT refactored\n");
  
  // transfer from device back to GPU
  ret = clEnqueueReadBuffer( commandQueue, FFT, CL_TRUE, 0, 2*nFreq*sizeof(float), h_out, 0, NULL, NULL );
  printf("copy back to host %d\n", ret);
  
  // Release OpenCL memory objects. 
  clReleaseMemObject( FFT );
  clReleaseMemObject( aMemObj);

  // Release OpenCL working objects.
  clReleaseCommandQueue( commandQueue );
  clReleaseContext( context );
 
  return 0; 
}

int fft2dinv_32f_lp(long N0, long N1, long d_fft, long d_out, long l_context, long l_queue) {
  printf("input address %ld", d_fft);
  printf("input address %lu", (unsigned long)d_fft);
 
	// cast long to context 
	cl_context context = (cl_context)l_context;
  
	// cast long to queue 
	cl_command_queue commandQueue = (cl_command_queue)l_queue;

  cl_int ret = setupFFT();
  cl_mem cl_mem_image=(cl_mem)d_fft;
  cl_mem cl_mem_out=(cl_mem)d_out;
 
  clfftPlanHandle planHandleBackward = bake_2d_backward_32f(N0, N1, context, commandQueue); 
  
  // number of elements in Hermitian (interleaved) output 
  unsigned long nFreq=N1*(N0/2+1);

  // Execute the plan.
  ret = clfftEnqueueTransform(planHandleBackward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &cl_mem_image, &cl_mem_out, NULL);

  printf("Backward FFT %d\n", ret);
  ret = clFinish(commandQueue);
  printf("Finish Command Queue for backward FFT %d\n", ret);
 
   // Release the plan. 
   ret = clfftDestroyPlan( &planHandleBackward);

   clfftTeardown();
   
   printf("Backward FFT finished\n");

   return 0; 
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

	// Creating context.
	cl_context context = clCreateContext(NULL, 1, &deviceID, NULL, NULL,  &ret);

  printf("\ncreated context\n");

	// Creating command queue
	cl_command_queue commandQueue = clCreateCommandQueue(context, deviceID, 0, &ret);

  printf("\ncreated command queue\n");

  // number of elements in Hermitian (interleaved) output 
  unsigned long nFreq = (N0/2+1)*N1;
	
  // declare FFT memory on GPU
	cl_mem d_FFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2 *nFreq * sizeof(float), NULL, &ret);
  printf("\ncreate variable 1 %d\n", ret);
	
  printf("\nallocated memory\n");

   // Copy fft to GPU
	ret = clEnqueueWriteBuffer(commandQueue, d_FFT, CL_TRUE, 0, 2 * nFreq * sizeof(float), h_fft, 0, NULL, NULL);;
  printf("\ncopy to GPU  %d\n", ret);

  // create output buffer 
  cl_mem out = clCreateBuffer(context, CL_MEM_READ_WRITE, N0*N1*sizeof(float), NULL, &ret);
  printf("\ncreate img on GPU %d\n", ret);

  fft2dinv_32f_lp(N0, N1, (long)d_FFT, (long)out, (long)context, (long)commandQueue);
  
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

int conv3d_32f_lp(size_t N0, size_t N1, size_t N2, long l_image, long l_psf,  long l_output, bool correlate, long l_context, long l_queue, long l_device) {

  printf("enter convolve");

  cl_int ret;

  // most of the inputs are long pointers we'll need to cast them to the right cl types

	// cast long to context 
	cl_context context = (cl_context)l_context;
  
	// cast long to queue 
	cl_command_queue commandQueue = (cl_command_queue)l_queue;
	
  // and long to deviceID
  cl_device_id deviceID = (cl_device_id)l_device;
  
  // cast long pointers to cl_mem 
	cl_mem d_image = (cl_mem)l_image;
	cl_mem d_psf =  (cl_mem)l_psf;
	cl_mem d_output = (cl_mem)l_output;

  // size in spatial domain
  unsigned long n = N0*N1*N2;

  // size in frequency domain
  unsigned long nFreq=(N0/2+1)*N1*N2;
 
  // create memory for FFT of estimate and PSF 
	cl_mem estimateFFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2*nFreq * sizeof(float), NULL, &ret);
  printf("\ncreate PSF FFT %d\n", ret);
 
  cl_mem psfFFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2*nFreq * sizeof(float), NULL, &ret);
  printf("\ncreate Object FFT %d\n", ret);
		
  // Create program from kernel source
	cl_program program = clCreateProgramWithSource(context, 1, (const char **)&programString, NULL, &ret);	
  printf("\ncreate program %d\n", ret);

	// Build opencl program
	ret = clBuildProgram(program, 1, &deviceID, NULL, NULL, NULL);

  printf("\nbuild program %d\n", ret);

  if (ret!=0) {
    return ret;
  }

	// Create complex multiply kernel
	cl_kernel kernelComplexMultiply = clCreateKernel(program, "vecComplexMultiply", &ret);
  printf("\ncreate KERNEL in GPU %d\n", ret);

  clfftPlanHandle planHandleForward=bake_3d_forward_32f(N0, N1, N2, context, commandQueue);
  clfftPlanHandle planHandleBackward=bake_3d_backward_32f(N0, N1, N2, context, commandQueue);
  
  // compute item sizes 
  size_t localItemSize=64;
	size_t globalItemSize= ceil((N2*N1*N0)/(float)localItemSize)*localItemSize;
	size_t globalItemSizeFreq = ceil((nFreq)/(float)localItemSize)*localItemSize;
  printf("nFreq %lu glbalItemSizeFreq %lu\n",nFreq, globalItemSizeFreq);
 
  // FFT of PSF
  ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &d_psf, &psfFFT, NULL);
  printf("fft psf %d\n", ret);
  
  // FFT of estimate
  ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &d_image, &estimateFFT, NULL);
  printf("fft estimate %d\n", ret);

  // complex multipy estimate FFT and PSF FFT
  ret = callKernel(kernelComplexMultiply, estimateFFT, psfFFT, estimateFFT, nFreq, commandQueue, globalItemSizeFreq, localItemSize);
  printf("kernel complex %d\n", ret);
  
  // Inverse to get convolved
  ret = clfftEnqueueTransform(planHandleBackward, CLFFT_BACKWARD, 1, &commandQueue, 0, NULL, NULL, &estimateFFT, &d_output, NULL);
  printf("fft inverse %d\n", ret);
 
  // Release OpenCL memory objects. 
  clReleaseMemObject( psfFFT );
  clReleaseMemObject( estimateFFT );

   // Release the plan. 
   ret = clfftDestroyPlan( &planHandleForward);
   ret = clfftDestroyPlan( &planHandleBackward );

   // Release clFFT library. 
   clfftTeardown( );

  return ret;
}

int conv3d_32f(size_t N0, size_t N1, size_t N2, float *h_image, float *h_psf, float *h_out) {

  cl_platform_id platformId = NULL;
	cl_device_id deviceID = NULL;
	cl_uint retNumDevices;
	cl_uint retNumPlatforms;
  cl_int ret = clGetPlatformIDs(1, &platformId, &retNumPlatforms);

  printf("\ncreated platform\n");

	ret = clGetDeviceIDs(platformId, CL_DEVICE_TYPE_DEFAULT, 1, &deviceID, &retNumDevices);

	// Creating context.
	cl_context context = clCreateContext(NULL, 1, &deviceID, NULL, NULL,  &ret);

  printf("\ncreated context\n");

	// Creating command queue
	cl_command_queue commandQueue = clCreateCommandQueue(context, deviceID, 0, &ret);

  printf("\ncreated command queue\n");
	
  // Memory buffers for each array
	cl_mem d_image = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  printf("\ncreate gpu mem for image %d\n", ret);
	cl_mem d_psf = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  printf("\ncreate gpu mem for psf %d\n", ret);
	cl_mem d_out = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  printf("\ncreate variable 3 %d\n", ret);
  
  printf("\nallocated memory\n");

   // Copy to memory buffers
	ret = clEnqueueWriteBuffer(commandQueue, d_image, CL_TRUE, 0, N2*N1*N0 * sizeof(float), h_image, 0, NULL, NULL);;
  printf("\ncopy to GPU  %d\n", ret);
	ret = clEnqueueWriteBuffer(commandQueue, d_psf, CL_TRUE, 0, N2*N1*N0 * sizeof(float), h_psf, 0, NULL, NULL);
  printf("\ncopy to GPU  %d\n", ret);

  conv3d_32f_lp(N0, N1, N2, (long)d_image, (long)d_psf, (long)d_out, 0, (long)context, (long)commandQueue, (long)deviceID);

  // copy back to host 
  ret = clEnqueueReadBuffer( commandQueue, d_out, CL_TRUE, 0, N0*N1*N2*sizeof(float), h_out, 0, NULL, NULL );

  return 0;
}

int deconv3d_32f_lp(int iterations, size_t N0, size_t N1, size_t N2, long l_observed, long l_psf, long l_estimate, long l_normal, long l_context, long l_queue, long l_device) {

  cl_int ret;
  
	// cast long to context 
	cl_context context = (cl_context)l_context;
  
	// cast long to queue 
	cl_command_queue commandQueue = (cl_command_queue)l_queue;
	
  // cast long pointers to cl_mem 
	cl_mem d_observed = (cl_mem)l_observed;
	cl_mem d_psf =  (cl_mem)l_psf;
	cl_mem d_estimate = (cl_mem)l_estimate; 
  cl_device_id deviceID = (cl_device_id)l_device;

  // size in spatial domain
  unsigned long n = N0*N1*N2;

  // size in frequency domain
  unsigned long nFreq=(N0/2+1)*N1*N2;

  // create memory for reblurred 	
  cl_mem d_reblurred = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  printf("\ncreate memory for reblurred %d\n", ret);
 
  // create memory for FFT of estimate and PSF 
	cl_mem estimateFFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2*nFreq * sizeof(float), NULL, &ret);
  printf("\ncreate PSF FFT %d\n", ret);
 
  cl_mem psfFFT = clCreateBuffer(context, CL_MEM_READ_WRITE, 2*nFreq * sizeof(float), NULL, &ret);
  printf("\ncreate Object FFT %d\n", ret);
	
  // Create kernels 	
  // Create program from kernel source
	cl_program program = clCreateProgramWithSource(context, 1, (const char **)&programString, NULL, &ret);	

  printf("\ncreate program %d\n", ret);

	// Build opencl program
	ret = clBuildProgram(program, 1, &deviceID, NULL, NULL, NULL);

  printf("\nbuild program %d\n", ret);

  if (ret!=0) {
    return ret;
  }

	// Create complex multiply kernel
	cl_kernel kernelComplexMultiply = clCreateKernel(program, "vecComplexMultiply", &ret);
  printf("\ncreate KERNEL in GPU %d\n", ret);
 
 	// Create complex conjugate multiply kernel
	cl_kernel kernelComplexConjugateMultiply = clCreateKernel(program, "vecComplexConjugateMultiply", &ret);
  printf("\ncreate KERNEL in GPU %d\n", ret);
 	
  // Create divide kernel
	cl_kernel kernelDiv = clCreateKernel(program, "vecDiv", &ret);
  printf("\ncreate Divide KERNEL in GPU %d\n", ret);
 
  // Create multiply kernel
	cl_kernel kernelMul = clCreateKernel(program, "vecMul", &ret);
  printf("\ncreate Divide KERNEL in GPU %d\n", ret);

  clfftPlanHandle planHandleForward=bake_3d_forward_32f(N0, N1, N2, context, commandQueue);
  
  clfftPlanHandle planHandleBackward=bake_3d_backward_32f(N0, N1, N2, context, commandQueue);

  // compute item sizes 
  size_t localItemSize=64;
	size_t globalItemSize= ceil((N2*N1*N0)/(float)localItemSize)*localItemSize;
	size_t globalItemSizeFreq = ceil((nFreq+1000)/(float)localItemSize)*localItemSize;
  printf("nFreq %lu glbalItemSizeFreq %lu\n",nFreq, globalItemSizeFreq);
  
   // FFT of PSF
  ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &d_psf, &psfFFT, NULL);

  printf("FFT of PSF %d\n", ret);

  for (int i=0;i<iterations;i++) {
      // FFT of estimate
      ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &d_estimate, &estimateFFT, NULL);
      //printf("fft1 %d\n", ret);

      // complex multipy estimate FFT and PSF FFT
      ret = callKernel(kernelComplexMultiply, estimateFFT, psfFFT, estimateFFT, nFreq, commandQueue, globalItemSizeFreq, localItemSize);
      //printf("kernel complex %d\n", ret);
      
      // Inverse to get reblurred
      ret = clfftEnqueueTransform(planHandleBackward, CLFFT_BACKWARD, 1, &commandQueue, 0, NULL, NULL, &estimateFFT, &d_reblurred, NULL);
      //printf("fft2 %d\n", ret);
      
      // divide observed by reblurred
      ret = callKernel(kernelDiv, d_observed, d_reblurred, d_reblurred, n, commandQueue, globalItemSize, localItemSize);
      
      if (ret!=0) {
        printf("kernel div %d\n", ret);
      }
      
      // FFT of observed/reblurred 
      ret = clfftEnqueueTransform(planHandleForward, CLFFT_FORWARD, 1, &commandQueue, 0, NULL, NULL, &d_reblurred, &estimateFFT, NULL);
      //printf("fft %d\n", ret);
      
      // Correlate above result with PSF 
      ret = callKernel(kernelComplexConjugateMultiply, estimateFFT, psfFFT, estimateFFT, nFreq, commandQueue, globalItemSizeFreq, localItemSize);
      printf("correlate %d\n", ret);
      
      // Inverse FFT to get update factor 
      ret = clfftEnqueueTransform(planHandleBackward, CLFFT_BACKWARD, 1, &commandQueue, 0, NULL, NULL, &estimateFFT, &d_reblurred, NULL);

      // multiply estimate by update factor 
      ret = callKernel(kernelMul, d_estimate, d_reblurred, d_estimate, n, commandQueue, globalItemSize, localItemSize);
      //printf("update %d\n", ret);
      
      ret = clFinish(commandQueue);

      printf("Finished iteration %d\n",i);

  }  
 
   // Release OpenCL memory objects. 
  clReleaseMemObject( d_reblurred);
  clReleaseMemObject( psfFFT );
  clReleaseMemObject( estimateFFT );

   // Release the plan. 
   ret = clfftDestroyPlan( &planHandleBackward );

   // Release clFFT library. 
   clfftTeardown( );

}

int deconv3d_32f(int iterations, size_t N0, size_t N1, size_t N2, float *h_image, float *h_psf, float *h_out, float * normal) {

  cl_platform_id platformId = NULL;
	cl_device_id deviceID = NULL;
	cl_uint retNumDevices;
	cl_uint retNumPlatforms;

  cl_int ret = clGetPlatformIDs(1, &platformId, &retNumPlatforms);
  printf("\ncreated platform %d \n",ret);
	
  ret = clGetDeviceIDs(platformId, CL_DEVICE_TYPE_DEFAULT, 1, &deviceID, &retNumDevices);
  printf("\nget device IDs %d \n",ret);
	
  // Creating context.
	cl_context context = clCreateContext(NULL, 1, &deviceID, NULL, NULL,  &ret);
  printf("created context %d\n", ret);

	// Creating command queue
	cl_command_queue commandQueue = clCreateCommandQueue(context, deviceID, 0, &ret);
  printf("created command queue %d\n", ret);

  // create device memory buffers for each array
	cl_mem d_observed = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  printf("\ncreate gpu mem for image %d\n", ret);
	cl_mem d_psf = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  printf("\ncreate gpu mem for psf %d\n", ret);
	cl_mem d_estimate = clCreateBuffer(context, CL_MEM_READ_WRITE, N2*N1*N0 * sizeof(float), NULL, &ret);
  printf("\ncreate variable 3 %d\n", ret);
 
  printf("\nallocated memory\n");

  // Copy lists to memory buffers
	ret = clEnqueueWriteBuffer(commandQueue, d_observed, CL_TRUE, 0, N2*N1*N0 * sizeof(float), h_image, 0, NULL, NULL);;
  printf("\ncopy to GPU  %d\n", ret);
	ret = clEnqueueWriteBuffer(commandQueue, d_psf, CL_TRUE, 0, N2*N1*N0 * sizeof(float), h_psf, 0, NULL, NULL);
  printf("\ncopy to GPU  %d\n", ret);
	ret = clEnqueueWriteBuffer(commandQueue, d_estimate, CL_TRUE, 0, N2*N1*N0 * sizeof(float), h_out, 0, NULL, NULL);
  printf("\ncopy to GPU  %d\n", ret);

  unsigned long n = N0*N1*N2;
  unsigned long nFreq=(N0/2+1)*N1*N2;
     
  printf("Call deconv with long pointers\n\n");
  deconv3d_32f_lp(iterations, N0, N1, N2, (long)d_observed, (long)d_psf, (long)d_estimate, (long)0, (long)context, (long)commandQueue, (long)deviceID); 
    
  // copy back to host 
  ret = clEnqueueReadBuffer( commandQueue, d_estimate, CL_TRUE, 0, N0*N1*N2*sizeof(float), h_out, 0, NULL, NULL );
 
  // Release OpenCL memory objects. 
  clReleaseMemObject( d_estimate);
  clReleaseMemObject( d_observed );
  clReleaseMemObject( d_psf);

   // Release OpenCL working objects.
   clReleaseCommandQueue( commandQueue );
   clReleaseContext( context );
  
  return ret;
}
