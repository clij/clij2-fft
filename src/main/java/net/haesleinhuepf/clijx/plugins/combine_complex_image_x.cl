__constant sampler_t sampler = CLK_NORMALIZED_COORDS_FALSE | CLK_ADDRESS_CLAMP_TO_EDGE | CLK_FILTER_NEAREST;

__kernel void combine_complex_image(
    IMAGE_real_src_TYPE real_src,
    IMAGE_imaginary_src_TYPE imaginary_src,
    IMAGE_complex_dst_TYPE complex_dst
)
{
  const int x = get_global_id(0);
  const int y = get_global_id(1);
  const int z = get_global_id(2);

  const float value_r = READ_IMAGE(real_src, sampler,      POS_real_src_INSTANCE(x, y, z, 0)).x;
  const float value_i = READ_IMAGE(imaginary_src, sampler, POS_imaginary_src_INSTANCE(x, y, z, 0)).x;

  WRITE_IMAGE(complex_dst, POS_complex_dst_INSTANCE(x * 2, y, z, 0),     CONVERT_complex_dst_PIXEL_TYPE(value_r));
  WRITE_IMAGE(complex_dst, POS_complex_dst_INSTANCE(x * 2 + 1, y, z, 0), CONVERT_complex_dst_PIXEL_TYPE(value_i));
}