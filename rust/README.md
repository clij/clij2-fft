# clij2fft for Rust

Richardson-Lucy deconvolution on the GPU from Rust, through clij2-fft's
native library (OpenCL and clFFT). A stopgap until a maintained GPU FFT is
available from Rust: clFFT is no longer developed.

```rust
let lib = clij2fft::Clij2Fft::open(None)?;
let restored = lib.richardson_lucy_nc(image.view(), psf.view(), 100, 0.0)?;
```

Arrays are `ndarray::Array3<f32>`, (z, y, x). `richardson_lucy` is
circulant; `richardson_lucy_nc` is non-circulant. A regularization above 0
adds total variation.

The library is opened at run time: `libclij2fft` (with `libclFFT` beside
it) from the path given to `open`, or `CLIJ2FFT_LIBRARY`, or the system's
library search path. The prebuilt ones are in `../lib/<platform>/`. OpenCL
itself comes with the GPU driver.

Same results as the Python wrapper, with one deliberate difference: the
PSF's centre goes to index 0 exactly. The Python wrapper is a pixel off for
an even-sized PSF padded to an odd size.

    cargo test                                   # the padding, no GPU
    LD_LIBRARY_PATH=../lib/linux-x86_64 python compare_with_python.py
