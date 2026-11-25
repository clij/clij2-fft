import os
import subprocess
import sys

import pytest

# runs in a fresh Python with no OpenCL drivers visible: OCL_ICD_VENDORS points the OpenCL
# loader at an empty folder. It must be set before OpenCL loads, hence the subprocess.
SCRIPT = """
import numpy as np
from clij2fft.richardson_lucy import richardson_lucy_nc
img = np.ones((32, 64, 64), np.float32)
psf = np.ones((9, 9, 9), np.float32)
try:
    richardson_lucy_nc(img, psf, 5)
except RuntimeError as e:
    print("RAISED", e)
else:
    print("RETURNED")
"""


@pytest.mark.skipif(sys.platform != "linux", reason="OCL_ICD_VENDORS is how the Linux OpenCL loader finds drivers")
def test_raises_without_opencl(tmp_path):
    env = dict(os.environ, OCL_ICD_VENDORS=str(tmp_path))
    out = subprocess.run([sys.executable, "-c", SCRIPT], env=env, capture_output=True, text=True,
                         timeout=120).stdout
    assert "RAISED" in out, out
    assert "-1001" in out, out
