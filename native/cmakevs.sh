mkdir Visual_Studio

cd Visual_Studio

cmake ../clij2fft -G"Visual Studio 16 2019" \
	-DCMAKE_BUILD_TYPE=Debug \
	-DCMAKE_INSTALL_PREFIX="../../lib/win64/" \
	-DOPENCL_INCLUDE_DIR="C:/Program Files/NVIDIA GPU Computing Toolkit/CUDA/v11.2/include/" \
	-DCLFFT_LIBRARY_DIR="C:/OpenCL/clFFT-2.12.2-Windows-x64/lib64/import/" \
	-DOPENCV_INSTALL_DIR="C:/Program Files/opencv/build"
   