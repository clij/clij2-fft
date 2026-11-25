import numpy as np
import math
import bisect

def handle_prime(p,x,a):
    log = math.log(p)
    power=p

    while power <= x + a.shape[0]:
        j=x%power
        if j>0:
            j=power-j

        while j < a.shape[0]:
            a[j]+=log
            j+=power

        power*=p
    

def next_smooth(x):
    """[summary]
    author Johannes Schindelin
    author Brian Northan

    A class to determine the next smooth number (a number divisable only)
    by prime numbers up to k (in this case we fix k at 7).
    
    Based on A. Granville, Finding smooth numbers computationally.
    
    Args:
        x ([type]): number to test

    Returns:
        [type]: next smooth number larger than x
    """
    if x == 0:
        return 0
    
    z = int(16*math.log2(x))
    delta = 0.000001

    a = np.zeros(z)

    handle_prime(2,x,a)
    handle_prime(3,x,a)
    handle_prime(5,x,a)
    handle_prime(7,x,a)

    log = math.log(x)
    for i in range(a.shape[0]):
        if a[i] >=log-delta:
            return x+i

    # none in the window (always for x=1, and for some large x): count up from its end
    m = x + a.shape[0]
    while True:
        r = m
        for p in (2, 3, 5, 7):
            while r % p == 0:
                r //= p
        if r == 1:
            return m
        m += 1

def get_next_smooth(size):
    """ for an nd tuble compute the next smooth size for each element

    Args:
        size ([type]): nd input tuple 

    Returns:
        [type]: tuple containing the next smooth size for each input element
    """
    return tuple(map(lambda i: next_smooth(i), size))


# FFT lengths, ascending; the same table as FFTSizes.java on the Java side
FFT_SIZES = (
    1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 14, 15, 16, 18, 20,
    21, 24, 25, 27, 28, 30, 32, 35, 36, 40, 42, 45, 48, 49, 50, 54,
    56, 60, 63, 64, 70, 72, 75, 80, 81, 84, 90, 96, 98, 100, 108, 112,
    120, 125, 126, 128, 135, 140, 144, 147, 150, 160, 162, 168, 175, 180, 189, 192,
    196, 200, 216, 224, 225, 240, 243, 245, 250, 252, 256, 270, 280, 288, 294, 300,
    320, 324, 336, 343, 350, 360, 375, 378, 384, 392, 400, 405, 432, 441, 448, 450,
    480, 486, 490, 500, 504, 512, 540, 560, 567, 576, 588, 600, 625, 640, 648, 672,
    675, 686, 700, 720, 729, 750, 756, 768, 784, 800, 810, 864, 875, 882, 896, 900,
    960, 972, 980, 1000, 1008, 1024, 1029, 1080, 1120, 1125, 1134, 1152, 1176, 1200, 1215, 1225,
    1250, 1280, 1296, 1323, 1344, 1350, 1372, 1400, 1440, 1458, 1500, 1512, 1536, 1568, 1600, 1620,
    1701, 1715, 1728, 1750, 1764, 1792, 1800, 1875, 1920, 1944, 1960, 2000, 2016, 2025, 2048, 2058,
    2160, 2187, 2240, 2250, 2268, 2304, 2352, 2400, 2401, 2430, 2450, 2500, 2560, 2592, 2646, 2688,
    2700, 2744, 2800, 2880, 2916, 3000, 3024, 3072, 3087, 3125, 3136, 3200, 3240, 3375, 3402, 3430,
    3456, 3500, 3528, 3584, 3600, 3645, 3750, 3840, 3888, 3920, 3969, 4000, 4032, 4050, 4096, 4116,
    4320, 4374, 4375, 4480, 4500, 4536, 4608, 4704, 4800, 4802, 4860, 4900, 5000, 5103, 5120, 5184,
    5292, 5376, 5400, 5488, 5600, 5625, 5760, 5832, 6000, 6048, 6075, 6125, 6144, 6174, 6250, 6272,
    6400, 6480, 6561, 6750, 6804, 6860, 6912, 7000, 7056, 7168, 7200, 7203, 7290, 7500, 7680, 7776,
    7840, 7938, 8000, 8064, 8100, 8192, 8232, 8575, 8640, 8748, 8750, 8960, 9000, 9072, 9216, 9261,
    9375, 9408, 9600, 9604, 9720, 9800, 10000, 10125, 10206, 10240, 10368, 10584, 10752, 10800, 10935, 10976,
    11200, 11250, 11520, 11664, 11907, 12000, 12005, 12096, 12150, 12250, 12288, 12348, 12500, 12544, 12800, 12960,
    13122, 13500, 13608, 13720, 13824, 14000, 14112, 14336, 14400, 14406, 14580, 15000, 15309, 15360, 15552, 15625,
    15680, 15876, 16000, 16128, 16200, 16384,
)

def next_fft_size(x):
    """ the smallest length from FFT_SIZES that is at least x; beyond the table, next_smooth(x)

    Args:
        x (int): length to pad

    Returns:
        int: the padded length
    """
    if x <= 1:
        return 1
    i = bisect.bisect_left(FFT_SIZES, x)
    if i < len(FFT_SIZES):
        return FFT_SIZES[i]
    return next_smooth(x)

def get_next_fft_size(size):
    """ for an nd tuple compute next_fft_size for each element

    Args:
        size ([type]): nd input tuple

    Returns:
        [type]: tuple containing next_fft_size of each input element
    """
    return tuple(map(lambda i: next_fft_size(i), size))

def get_pad_size(img, psf):
    """ given an image and psf return the extended size needed to avoid circular calculations
    during convolution and/or deconvolution 

    Args:
        img ([type]): nd image
        psf ([type]): nd psf 

    Returns:
        [tuple]: extended size to use to avoid circular calculations
    """
    return tuple(map(lambda i,j: i+2*math.floor(j/2),img.shape,psf.shape))


def pad(img, paddedsize, mode, constant_values=0):
    """ pad image to paddedsize

    Args:
        img ([type]): image to pad 
        paddedsize ([type]): size to pad to 
        mode ([type]): one of the np.pad modes

    Returns:
        padded [nd array]: padded image
        padding [tuple]: tuple containing the padding used
    """
    padding = tuple(map(lambda i,j: ( math.ceil((i-j)/2), math.floor((i-j)/2) ),paddedsize,img.shape))

    if mode == 'constant':
        return np.pad(img, padding,mode, constant_values=constant_values), padding
    else:
        return np.pad(img, padding,mode), padding
    
def unpad(padded, imgsize):
    """ crop padded back to imgsize

    Args:
        padded ([type]): [description]
        imgsize ([type]): [description]

    Returns:
        [type]: [description]
    """
    padding = tuple(map(lambda i,j: ( math.ceil((i-j)/2), math.floor((i-j)/2) ),padded.shape, imgsize))
    return padded[padding[0][0]:padding[0][0]+imgsize[0], padding[1][0]:padding[1][0]+imgsize[1], padding[2][0]:padding[2][0]+imgsize[2]]