#pragma once

#if defined(__APPLE__) || defined(__MACOSX)
#include <OpenCL/cl.h>
#else
#include <CL/cl.h>
#endif

#ifdef _WIN64
 __declspec(dllexport) int conv3d_32f(size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out);
 __declspec(dllexport) int conv3d_32f_lp(size_t N0, size_t N1, size_t N2, long l_image, long l_psf,  long l_output, bool correlate, long l_context, long l_queue, long l_device);
 __declspec(dllexport) int deconv3d_32f(int iterations, size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out, float * normal);
 __declspec(dllexport) int deconv3d_32f_tv(int iterations, float regularizationFactor, size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out, float * normal);
 __declspec(dllexport) int deconv3d_32f_lp(int iterations, size_t N0, size_t N1, size_t N2, long d_image, long d_psf, long d_update, long d_normal, long l_context, long l_queuee, long l_device); 
 __declspec(dllexport) int deconv3d_32f_lp_tv(int iterations, , float regularizationFactor, size_t N0, size_t N1, size_t N2, long d_image, long d_psf, long d_update, long d_normal, long l_context, long l_queuee, long l_device); 
 __declspec(dllexport) int fft2d_32f(size_t N1, size_t N2, float *h_image, float * h_out);
 __declspec(dllexport) int fft2d_32f_lp(long N1, long N2, long h_image, long h_out, long l_context, long l_queue);
 __declspec(dllexport) int fftinv2d_32f(size_t N1, size_t N2, float *h_fft, float * h_out);
#else
extern "C" {
  int conv3d_32f(size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out);
  int conv3d_32f_lp(size_t N0, size_t N1, size_t N2, long l_image, long l_psf,  long l_output, bool correlate, long l_context, long l_queue, long l_device);
  int deconv3d_32f(int iterations, size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out, float * normal);
  int deconv3d_32f_tv(int iterations, float regularizationFactor, size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out, float * normal);
  int deconv3d_32f_lp(int iterations, size_t N0, size_t N1, size_t N2, long d_image, long d_psf, long d_update, long d_normal, long l_context, long l_queuee, long l_device); 
  int deconv3d_32f_lp_tv(int iterations, float regularizationFactor, size_t N0, size_t N1, size_t N2, long d_image, long d_psf, long d_update, long d_normal, long l_context, long l_queuee, long l_device); 
  int fft2d_32f(size_t N1, size_t N2, float *h_image, float * h_out);
  int fft2d_32f_lp(long N1, long N2, long h_image, long h_out, long l_context, long l_queue);
  int fftinv2d_32f(size_t N1, size_t N2, float *h_fft, float * h_out);
  int fft2dinv_32f_lp(long N1, long N2, long h_fft, long h_out, long l_context, long l_queue);
  int diagnostic(); 
}
#endif

size_t getFileSize(const char* fileName);
int getProgramFromFile(const char* fileName, char * programString, size_t programLength);
cl_program makeProgram(cl_context context, cl_device_id deviceID, char * programString);

