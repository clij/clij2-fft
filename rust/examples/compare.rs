//! Deconvolve raw float32 files both ways, for comparing with Python.
//!
//!     cargo run --release --example compare -- DIR Z Y X PZ PY PX ITERATIONS
//!
//! Reads DIR/image.f32 and DIR/psf.f32 (little-endian, C order); writes
//! DIR/rust_rl.f32 and DIR/rust_rl_nc.f32. See compare_with_python.py.

use std::{env, fs, path::Path};

use clij2fft::Clij2Fft;
use ndarray::Array3;

fn read(path: &Path, shape: (usize, usize, usize)) -> Array3<f32> {
    let bytes = fs::read(path).expect("read");
    let values = bytes
        .chunks_exact(4)
        .map(|b| f32::from_le_bytes(b.try_into().unwrap()))
        .collect();
    Array3::from_shape_vec(shape, values).expect("shape")
}

fn write(path: &Path, array: &Array3<f32>) {
    let bytes: Vec<u8> = array.iter().flat_map(|v| v.to_le_bytes()).collect();
    fs::write(path, bytes).expect("write");
}

fn main() -> Result<(), Box<dyn std::error::Error>> {
    let args: Vec<String> = env::args().skip(1).collect();
    let dir = Path::new(&args[0]);
    let n: Vec<usize> = args[1..7].iter().map(|a| a.parse().unwrap()).collect();
    let iterations: i32 = args[7].parse()?;
    let image = read(&dir.join("image.f32"), (n[0], n[1], n[2]));
    let psf = read(&dir.join("psf.f32"), (n[3], n[4], n[5]));

    let lib = Clij2Fft::open(None)?;
    let rl = lib.richardson_lucy(image.view(), psf.view(), iterations, 0.0)?;
    write(&dir.join("rust_rl.f32"), &rl);
    let rl_nc = lib.richardson_lucy_nc(image.view(), psf.view(), iterations, 0.0)?;
    write(&dir.join("rust_rl_nc.f32"), &rl_nc);
    Ok(())
}
