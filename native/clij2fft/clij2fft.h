#pragma once

#ifdef _WIN64
 __declspec(dllexport) int conv3d_32f(size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out);
 __declspec(dllexport) int conv3d_32f_lp(size_t N0, size_t N1, size_t N2, long long l_image, long long l_psf,  long long l_output, bool correlate, long long l_context, long long l_queue, long long l_device);
 __declspec(dllexport) int deconv3d_32f(int iterations, size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out, float * normal);
 __declspec(dllexport) int deconv3d_32f_tv(int iterations, float regularizationFactor, size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out, float * normal);
 __declspec(dllexport) int deconv3d_32f_lp(int iterations, size_t N0, size_t N1, size_t N2, long long d_image, long long d_psf, long long d_update, long long d_normal, long long l_context, long long l_queuee, long long l_device); 
 __declspec(dllexport) int deconv3d_32f_lp_tv(int iterations, float regularizationFactor, size_t N0, size_t N1, size_t N2, long long d_image, long long d_psf, long long d_update, long long d_normal, long long l_context, long long l_queuee, long long l_device); 
 __declspec(dllexport) int fft2d_32f(size_t N1, size_t N2, float *h_image, float * h_out);
 __declspec(dllexport) int fft2d_32f_lp(long long N1, long long N2, long long h_image, long long h_out, long long l_context, long long l_queue);
 __declspec(dllexport) int fftinv2d_32f(size_t N1, size_t N2, float *h_fft, float * h_out);
 __declspec(dllexport) int fft2dinv_32f_lp(long long N1, long long N2, long long h_fft, long long h_out, long long l_context, long long l_queue);
 __declspec(dllexport) int diagnostic(); 
#else
extern "C" {
  int conv3d_32f(size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out);
  int conv3d_32f_lp(size_t N0, size_t N1, size_t N2, long long l_image, long long l_psf,  long long l_output, bool correlate, long long l_context, long long l_queue, long long l_device);
  int deconv3d_32f(int iterations, size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out, float * normal);
  int deconv3d_32f_tv(int iterations, float regularizationFactor, size_t N1, size_t N2, size_t N3, float *h_image, float *h_psf, float *h_out, float * normal);
  int deconv3d_32f_lp(int iterations, size_t N0, size_t N1, size_t N2, long long d_image, long long d_psf, long long d_update, long long d_normal, long long l_context, long long l_queuee, long long l_device); 
  int deconv3d_32f_lp_tv(int iterations, float regularizationFactor, size_t N0, size_t N1, size_t N2, long long d_image, long long d_psf, long long d_update, long long d_normal, long long l_context, long long l_queuee, long long l_device); 
  int fft2d_32f(size_t N1, size_t N2, float *h_image, float * h_out);
  int fft2d_32f_lp(long long N1, long long N2, long long h_image, long long h_out, long long l_context, long long l_queue);
  int fftinv2d_32f(size_t N1, size_t N2, float *h_fft, float * h_out);
  int fft2dinv_32f_lp(long long N1, long long N2, long long h_fft, long long h_out, long long l_context, long long l_queue);
  int diagnostic(); 
}
#endif

