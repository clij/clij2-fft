__constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

__kernel void split_complex_image(
    IMAGE_complex_src_TYPE complex_src,
    IMAGE_real_dst_TYPE real_dst,
    IMAGE_imaginary_dst_TYPE imaginary_dst
)
{
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  const int z = get_global_id(2);

  const float value_r = READ_IMAGE(complex_src, sampler, POS_complex_src_INSTANCE(x * 2, y, z, 0)).x;
  const float value_i = READ_IMAGE(complex_src, sampler, POS_complex_src_INSTANCE(x * 2 + 1, y, z, 0)).x;

  WRITE_IMAGE(real_dst,      POS_real_dst_INSTANCE(x, y, z, 0),      CONVERT_real_dst_PIXEL_TYPE(value_r));
  WRITE_IMAGE(imaginary_dst, POS_imaginary_dst_INSTANCE(x, y, z, 0), CONVERT_imaginary_dst_PIXEL_TYPE(value_i));
}