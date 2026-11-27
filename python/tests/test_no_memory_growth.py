"""Many calls in one process, with a lib passed in, must not pile up GPU memory."""
import os
import shutil
import subprocess

import numpy as np
import pytest

from clij2fft.libs import getlib
from clij2fft.richardson_lucy import richardson_lucy_nc

CALLS = 15
MAX_GROWTH_MIB = 100


def gpu_mib():
    """GPU memory this process uses, or the whole GPU's if nvidia-smi doesn't list it."""
    apps = subprocess.check_output(["nvidia-smi", "--query-compute-apps=pid,used_memory",
                                    "--format=csv,noheader,nounits"], text=True)
    for line in apps.splitlines():
        pid, mib = [x.strip() for x in line.split(",")]
        if int(pid) == os.getpid():
            return int(mib)
    used = subprocess.check_output(["nvidia-smi", "--query-gpu=memory.used",
                                    "--format=csv,noheader,nounits"], text=True)
    return int(used.split()[0])


@pytest.mark.skipif(shutil.which("nvidia-smi") is None, reason="needs nvidia-smi to read GPU memory")
def test_repeated_calls_with_lib_do_not_grow_gpu_memory():
    lib = getlib()
    img = np.random.default_rng(0).random((32, 64, 64), dtype=np.float32) * 100
    psf = np.ones((9, 9, 9), np.float32)
    richardson_lucy_nc(img, psf, 5, lib=lib)  # the first call sets things up
    before = gpu_mib()
    try:
        for _ in range(CALLS):
            richardson_lucy_nc(img, psf, 5, lib=lib)
        growth = gpu_mib() - before
    finally:
        lib.cleanup()
    assert growth < MAX_GROWTH_MIB, f"GPU memory grew {growth} MiB over {CALLS} calls"
