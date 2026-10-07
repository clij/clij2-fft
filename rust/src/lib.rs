//! Richardson-Lucy deconvolution on the GPU, through clij2-fft's native
//! library (OpenCL and clFFT).
//!
//! A stopgap until a maintained GPU FFT is available from Rust: clFFT is no
//! longer developed. The library is opened at run time, so a crate that uses
//! this builds and runs without it; only deconvolution fails if it's missing.
//!
//! Arrays are (z, y, x), in standard (C) order. The native code wants float32
//! and 7-smooth sizes, so these functions pad to such a size and crop the
//! result back, as clij2-fft's Python wrapper does. One difference: the PSF's
//! centre (index n / 2 on each axis) goes to index 0 exactly, whatever the
//! padded size. The Python wrapper centres the PSF by array size, which puts
//! an even-sized PSF a pixel off when the padded size is odd.

use std::fmt;
use std::os::raw::c_int;
use std::path::Path;

use libloading::Library;
use ndarray::{s, Array3, ArrayView3};

/// The native library's file name on this platform.
pub const LIBRARY: &str = if cfg!(windows) {
    "clij2fft.dll"
} else if cfg!(target_os = "macos") {
    "libclij2fft.dylib"
} else {
    "libclij2fft.so"
};

#[derive(Debug)]
pub enum Error {
    /// The native library, or a function in it, could not be found.
    Load(libloading::Error),
    /// The native code returned an OpenCL or clFFT error status.
    Native { function: &'static str, status: i32 },
}

impl fmt::Display for Error {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            Error::Load(e) => write!(f, "could not load clij2fft: {e}"),
            Error::Native { function, status } => {
                write!(f, "{function} failed with OpenCL/clFFT status {status}")
            }
        }
    }
}

impl std::error::Error for Error {}

type ConvCorr = unsafe extern "C" fn(
    usize, usize, usize, *mut f32, *mut f32, *mut f32, bool, c_int, c_int,
) -> c_int;
type DeconvTv = unsafe extern "C" fn(
    c_int, f32, usize, usize, usize, *mut f32, *mut f32, *mut f32, *mut f32, c_int, c_int,
) -> c_int;
type Cleanup = unsafe extern "C" fn() -> c_int;

/// The native library, opened. Which OpenCL platform and device to run on
/// are public fields; both default to 0.
pub struct Clij2Fft {
    lib: Library,
    pub platform: i32,
    pub device: i32,
}

impl Clij2Fft {
    /// Open the native library: the file at *path*, or else the one named by
    /// the `CLIJ2FFT_LIBRARY` environment variable, or else [`LIBRARY`] on
    /// the system's library search path. clFFT must sit beside it.
    pub fn open(path: Option<&Path>) -> Result<Self, Error> {
        let from_env = std::env::var_os("CLIJ2FFT_LIBRARY");
        let lib = unsafe {
            match (path, from_env) {
                (Some(path), _) => Library::new(path),
                (None, Some(path)) => Library::new(path),
                (None, None) => Library::new(LIBRARY),
            }
        }
        .map_err(Error::Load)?;
        Ok(Self { lib, platform: 0, device: 0 })
    }

    /// Circulant Richardson-Lucy: the image wraps around at its edges.
    ///
    /// Padded to at least the PSF's size, mirrored at the edges, as
    /// clij2-fft's Python `richardson_lucy` does. *regularization* above 0
    /// adds total-variation regularization.
    pub fn richardson_lucy(
        &self,
        image: ArrayView3<f32>,
        psf: ArrayView3<f32>,
        iterations: i32,
        regularization: f32,
    ) -> Result<Array3<f32>, Error> {
        let shape = image.dim();
        let (pz, py, px) = psf.dim();
        let size = (
            next_smooth(shape.0.max(pz)),
            next_smooth(shape.1.max(py)),
            next_smooth(shape.2.max(px)),
        );
        let (mut padded, before) = pad_reflect(image, size);
        let mut shifted = shift_psf(psf, size);
        let mut normal = Array3::<f32>::ones(size);
        let mut estimate = padded.clone();
        self.deconvolve(
            &mut padded, &mut shifted, &mut estimate, &mut normal, iterations, regularization,
        )?;
        Ok(crop(&estimate, before, shape))
    }

    /// Non-circulant Richardson-Lucy: the image is treated as part of a
    /// larger scene, so its edges are not wrapped onto each other.
    ///
    /// Padded by half the PSF on each side, then to a 7-smooth size, with a
    /// normalization over the valid region, as clij2-fft's Python
    /// `richardson_lucy_nc` does
    /// (<http://bigwww.epfl.ch/deconvolution/challenge2013/index.html?p=doc_math_rl>).
    pub fn richardson_lucy_nc(
        &self,
        image: ArrayView3<f32>,
        psf: ArrayView3<f32>,
        iterations: i32,
        regularization: f32,
    ) -> Result<Array3<f32>, Error> {
        let shape = image.dim();
        let (pz, py, px) = psf.dim();
        let size = (
            next_smooth(shape.0 + 2 * (pz / 2)),
            next_smooth(shape.1 + 2 * (py / 2)),
            next_smooth(shape.2 + 2 * (px / 2)),
        );
        let total = psf.sum();
        let psf = psf.mapv(|v| v / total);
        let (mut padded, before) = pad_zeros(image, size);
        let mut shifted = shift_psf(psf.view(), size);
        // The first guess is the mean of the padded image, zeros included.
        let mean = padded.iter().map(|&v| v as f64).sum::<f64>() / padded.len() as f64;
        let mut estimate = Array3::<f32>::from_elem(size, mean as f32);

        // The normalization is the valid region -- the image -- correlated
        // with the PSF.
        let mut valid = Array3::<f32>::zeros(size);
        valid
            .slice_mut(s![
                before.0..before.0 + shape.0,
                before.1..before.1 + shape.1,
                before.2..before.2 + shape.2
            ])
            .fill(1.0);
        let mut normal = Array3::<f32>::zeros(size);
        self.correlate(&mut valid, &mut shifted, &mut normal)?;

        self.deconvolve(
            &mut padded, &mut shifted, &mut estimate, &mut normal, iterations, regularization,
        )?;
        Ok(crop(&estimate, before, shape))
    }

    fn correlate(
        &self,
        image: &mut Array3<f32>,
        psf: &mut Array3<f32>,
        out: &mut Array3<f32>,
    ) -> Result<(), Error> {
        let (n3, n2, n1) = image.dim();
        let status = unsafe {
            let f = self.lib.get::<ConvCorr>(b"convcorr3d_32f\0").map_err(Error::Load)?;
            f(
                n1, n2, n3, image.as_mut_ptr(), psf.as_mut_ptr(), out.as_mut_ptr(), true,
                self.platform, self.device,
            )
        };
        check("convcorr3d_32f", status)
    }

    /// The native iteration. *estimate* is the first guess on the way in and
    /// the result on the way out.
    fn deconvolve(
        &self,
        image: &mut Array3<f32>,
        psf: &mut Array3<f32>,
        estimate: &mut Array3<f32>,
        normal: &mut Array3<f32>,
        iterations: i32,
        regularization: f32,
    ) -> Result<(), Error> {
        let (n3, n2, n1) = image.dim();
        let status = unsafe {
            let f = self.lib.get::<DeconvTv>(b"deconv3d_32f_tv\0").map_err(Error::Load)?;
            f(
                iterations, regularization, n1, n2, n3, image.as_mut_ptr(), psf.as_mut_ptr(),
                estimate.as_mut_ptr(), normal.as_mut_ptr(), self.platform, self.device,
            )
        };
        check("deconv3d_32f_tv", status)
    }
}

impl Drop for Clij2Fft {
    /// Tear clFFT down, as the Python wrapper does after a call.
    fn drop(&mut self) {
        unsafe {
            if let Ok(f) = self.lib.get::<Cleanup>(b"cleanup\0") {
                f();
            }
        }
    }
}

fn check(function: &'static str, status: c_int) -> Result<(), Error> {
    if status == 0 {
        Ok(())
    } else {
        Err(Error::Native { function, status })
    }
}

/// The smallest 7-smooth number at least *n*: a size clFFT can transform.
pub fn next_smooth(n: usize) -> usize {
    let mut m = n.max(1);
    loop {
        let mut rest = m;
        for p in [2, 3, 5, 7] {
            while rest % p == 0 {
                rest /= p;
            }
        }
        if rest == 1 {
            return m;
        }
        m += 1;
    }
}

type Shape = (usize, usize, usize);

/// Where an array of *small* size starts, centred in one of *big* size: the
/// extra pixel, if any, goes before. As numpy's pad in clij2-fft's `pad`.
fn before(big: Shape, small: Shape) -> Shape {
    (
        (big.0 - small.0 + 1) / 2,
        (big.1 - small.1 + 1) / 2,
        (big.2 - small.2 + 1) / 2,
    )
}

fn pad_zeros(image: ArrayView3<f32>, size: Shape) -> (Array3<f32>, Shape) {
    let at = before(size, image.dim());
    let (n0, n1, n2) = image.dim();
    let mut padded = Array3::<f32>::zeros(size);
    padded
        .slice_mut(s![at.0..at.0 + n0, at.1..at.1 + n1, at.2..at.2 + n2])
        .assign(&image);
    (padded, at)
}

/// Padded by mirroring at the edges, the edge pixel not repeated: numpy's
/// "reflect".
fn pad_reflect(image: ArrayView3<f32>, size: Shape) -> (Array3<f32>, Shape) {
    let at = before(size, image.dim());
    let (n0, n1, n2) = image.dim();
    let padded = Array3::from_shape_fn(size, |(z, y, x)| {
        image[[
            reflect(z as isize - at.0 as isize, n0),
            reflect(y as isize - at.1 as isize, n1),
            reflect(x as isize - at.2 as isize, n2),
        ]]
    });
    (padded, at)
}

fn reflect(i: isize, n: usize) -> usize {
    if n == 1 {
        return 0;
    }
    let period = 2 * (n as isize - 1);
    let m = i.rem_euclid(period);
    (if m < n as isize { m } else { period - m }) as usize
}

/// The PSF in an array of *size*, its centre (index n / 2 on each axis) at
/// index 0 and the rest wrapped around: what an FFT convolution wants.
fn shift_psf(psf: ArrayView3<f32>, size: Shape) -> Array3<f32> {
    let (p0, p1, p2) = psf.dim();
    let mut shifted = Array3::<f32>::zeros(size);
    for ((z, y, x), &v) in psf.indexed_iter() {
        let at = |i: usize, p: usize, n: usize| (i as isize - (p / 2) as isize).rem_euclid(n as isize) as usize;
        shifted[[at(z, p0, size.0), at(y, p1, size.1), at(x, p2, size.2)]] = v;
    }
    shifted
}

fn crop(padded: &Array3<f32>, at: Shape, shape: Shape) -> Array3<f32> {
    padded
        .slice(s![at.0..at.0 + shape.0, at.1..at.1 + shape.1, at.2..at.2 + shape.2])
        .to_owned()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn next_smooth_is_7_smooth_and_smallest() {
        assert_eq!(next_smooth(1), 1);
        assert_eq!(next_smooth(11), 12);
        assert_eq!(next_smooth(127), 128);
        assert_eq!(next_smooth(313), 315); // 3 * 3 * 5 * 7, odd
    }

    #[test]
    fn reflect_does_not_repeat_the_edge() {
        let row: Vec<usize> = (-2..5).map(|i| reflect(i, 3)).collect();
        assert_eq!(row, vec![2, 1, 0, 1, 2, 1, 0]);
    }

    #[test]
    fn the_psf_centre_lands_at_zero_for_odd_and_even_sizes() {
        for p in [8, 9] {
            for n in [30, 31] {
                let mut psf = Array3::<f32>::zeros((p, p, p));
                psf[[p / 2, p / 2, p / 2]] = 1.0;
                let shifted = shift_psf(psf.view(), (n, n, n));
                assert_eq!(shifted[[0, 0, 0]], 1.0, "psf {p}, padded {n}");
            }
        }
    }

    #[test]
    fn padding_then_cropping_gives_back_the_image() {
        let image = Array3::from_shape_fn((3, 4, 5), |(z, y, x)| (z * 100 + y * 10 + x) as f32);
        for size in [(4, 6, 7), (5, 9, 10)] {
            let (zeros, at) = pad_zeros(image.view(), size);
            assert_eq!(crop(&zeros, at, image.dim()), image);
            let (mirror, at) = pad_reflect(image.view(), size);
            assert_eq!(crop(&mirror, at, image.dim()), image);
        }
    }
}
