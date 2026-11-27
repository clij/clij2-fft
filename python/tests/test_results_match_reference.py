"""Richardson-Lucy results must not change when the implementation changes.

The reference (inputs and outputs) is stored in data/rl_reference.npz. To make it from the
current code, run this file: python tests/test_results_match_reference.py
"""
import os

import numpy as np
import pytest

from clij2fft.libs import getlib
from clij2fft.richardson_lucy import richardson_lucy, richardson_lucy_nc

REFERENCE = os.path.join(os.path.dirname(__file__), "data", "rl_reference.npz")
ITERATIONS = 20

# name: (function, regularization factor, pass a lib in)
CASES = {
    "nc": (richardson_lucy_nc, 0, True),
    "nc_tv": (richardson_lucy_nc, 0.0002, True),
    "rl": (richardson_lucy, 0, False),
}


def make_inputs():
    """Beads on a background, blurred by a Gaussian PSF, with Poisson noise."""
    rng = np.random.default_rng(42)
    shape = (20, 48, 48)
    truth = np.full(shape, 10, np.float32)
    for _ in range(15):
        truth[tuple(int(rng.integers(4, s - 4)) for s in shape)] += 1000
    g = np.meshgrid(*[np.arange(15) - 7] * 3, indexing="ij")
    psf = np.exp(-(g[0]**2 / (2 * 2.5**2) + g[1]**2 / (2 * 1.5**2) + g[2]**2 / (2 * 1.5**2)))
    psf = (psf / psf.sum()).astype(np.float32)
    psf_full = np.zeros(shape)
    start = [(s - 15) // 2 for s in shape]
    psf_full[start[0]:start[0] + 15, start[1]:start[1] + 15, start[2]:start[2] + 15] = psf
    blurred = np.real(np.fft.ifftn(np.fft.fftn(truth) * np.fft.fftn(np.fft.ifftshift(psf_full))))
    img = rng.poisson(np.maximum(blurred, 0)).astype(np.float32)
    return img, psf


def run(name, img, psf, lib):
    fn, reg, pass_lib = CASES[name]
    return fn(img, psf, ITERATIONS, reg, lib=lib if pass_lib else None)


@pytest.fixture(scope="module")
def reference():
    assert os.path.exists(REFERENCE), "no reference; run this file to make it"
    return np.load(REFERENCE)


@pytest.fixture(scope="module")
def lib():
    lib = getlib()
    yield lib
    lib.cleanup()


@pytest.mark.parametrize("name", CASES)
def test_result_matches_reference(name, reference, lib):
    out = run(name, reference["img"], reference["psf"], lib)
    ref = reference[name]
    assert out.shape == ref.shape
    np.testing.assert_allclose(out, ref, rtol=1e-4, atol=1e-4 * float(ref.max()))


if __name__ == "__main__":
    img, psf = make_inputs()
    lib = getlib()
    outputs = {name: run(name, img, psf, lib) for name in CASES}
    lib.cleanup()
    os.makedirs(os.path.dirname(REFERENCE), exist_ok=True)
    np.savez_compressed(REFERENCE, img=img, psf=psf, **outputs)
    print("wrote", REFERENCE, {k: (v.shape, float(v.min()), float(v.max())) for k, v in outputs.items()})
