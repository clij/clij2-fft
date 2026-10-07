"""Compare the Rust bindings with clij2-fft's Python wrapper, on this GPU.

Same image, same PSF, both deconvolutions, both ways. The PSF is odd-sized:
for an even one the two place its centre a pixel apart (see src/lib.rs).

    cd rust
    LD_LIBRARY_PATH=../lib/linux-x86_64 python compare_with_python.py
"""

import subprocess
import sys
import tempfile
from pathlib import Path

import numpy as np

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "python"))
from clij2fft.richardson_lucy import richardson_lucy, richardson_lucy_nc  # noqa: E402

rng = np.random.default_rng(0)
image = (rng.random((20, 64, 80)) * 100 + 10).astype(np.float32)
z, y, x = np.mgrid[-4:5, -7:8, -7:8]
psf = np.exp(-(z**2 / 8 + (y**2 + x**2) / 4)).astype(np.float32)
iterations = 10

with tempfile.TemporaryDirectory() as folder:
    folder = Path(folder)
    image.tofile(folder / "image.f32")
    psf.tofile(folder / "psf.f32")
    subprocess.run(
        ["cargo", "run", "-q", "--release", "--example", "compare", "--", str(folder),
         *map(str, image.shape), *map(str, psf.shape), str(iterations)],
        cwd=HERE, check=True,
    )
    for name, deconvolve in (("rl", richardson_lucy), ("rl_nc", richardson_lucy_nc)):
        rust = np.fromfile(folder / f"rust_{name}.f32", np.float32).reshape(image.shape)
        python = deconvolve(image, psf, iterations)
        difference = np.abs(rust - python).max() / np.abs(python).max()
        print(f"{name}: largest difference {difference:.1e} of the peak")
