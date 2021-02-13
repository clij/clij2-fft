__constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

__kernel void multiply_complex_images(
    IMAGE_src1_TYPE  src1,
    IMAGE_src2_TYPE  src2,
    IMAGE_dst_TYPE  dst
)
{
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  const int z = get_global_id(2);

  const float value1_r = READ_IMAGE(src1, sampler, POS_src1_INSTANCE(x * 2, y, z, 0)).x;
  const float value1_i = READ_IMAGE(src1, sampler, POS_src1_INSTANCE(x * 2 + 1, y, z, 0)).x;

  const float value2_r = READ_IMAGE(src2, sampler, POS_src2_INSTANCE(x * 2, y, z, 0)).x;
  const float value2_i = READ_IMAGE(src2, sampler, POS_src2_INSTANCE(x * 2 + 1, y, z, 0)).x;

  // from https://github.com/clij/clij2-fft/blob/master/native/clij2fft/clij2fft.cpp
  // float real = a[2*id] * b[2*id]-a[2*id+1]*b[2*id+1];
  // float imag = a[2*id]*b[2*id+1] + a[2*id+1]*b[2*id];

  const float result_r = value1_r * value2_r - value1_i * value2_i;
  const float result_i = value1_r * value2_i + value1_i * value2_r;

  WRITE_IMAGE(dst, POS_dst_INSTANCE(x * 2, y, z, 0),     CONVERT_dst_PIXEL_TYPE(result_r));
  WRITE_IMAGE(dst, POS_dst_INSTANCE(x * 2 + 1, y, z, 0), CONVERT_dst_PIXEL_TYPE(result_i));
}