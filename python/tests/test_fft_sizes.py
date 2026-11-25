import os
import re

import pytest

from clij2fft.pad import FFT_SIZES, next_fft_size

# past the end of the table, so the fallback is covered too
LIMIT = 20000


def without_factors_2357(n):
    for p in (2, 3, 5, 7):
        while n % p == 0:
            n //= p
    return n


def test_next_is_at_least_n_and_only_has_factors_2357():
    for n in range(1, LIMIT + 1):
        m = next_fft_size(n)
        assert m >= n, (n, m)
        assert without_factors_2357(m) == 1, (n, m)
        assert next_fft_size(m) == m, "a padded length is kept"


def test_next_never_decreases():
    previous = next_fft_size(1)
    for n in range(2, LIMIT + 1):
        m = next_fft_size(n)
        assert m >= previous, n
        previous = m


JAVA = os.path.join(os.path.dirname(__file__), "..", "..", "src", "main", "java",
                    "net", "haesleinhuepf", "clijx", "plugins", "FFTSizes.java")


@pytest.mark.skipif(not os.path.exists(JAVA), reason="FFTSizes.java not found (not run from the repo)")
def test_same_table_as_java():
    java = open(JAVA).read()
    body = re.search(r"SIZES = \{(.*?)\};", java, re.S).group(1)
    assert tuple(int(x) for x in re.findall(r"\d+", body)) == FFT_SIZES
