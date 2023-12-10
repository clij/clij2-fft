project(test)

find_package(OpenCL)

# find Open CV
FIND_PACKAGE( OpenCV REQUIRED PATHS ${OPENCV_INSTALL_DIR})
INCLUDE_DIRECTORIES( ${OpenCV_INCLUDE_DIRS} ${OPENCL_INCLUDE_DIR})

# find clFFT path
FIND_PATH(CLFFT_LIBRARY_DIR $ENV{CLFFT_LIBRARY_DIR} [DOC "CLFFT library path"])

link_directories(${CLFFT_LIBRARY_DIR})

add_executable(ffttest ffttest.cpp tifstack.cpp ../clij2fft/clij2fft.cpp)
add_executable(deconvolutiontest deconvolutiontest.cpp tifstack.cpp ../clij2fft/clij2fft.cpp)
add_executable(convolvetest convolvetest.cpp tifstack.cpp ../clij2fft/clij2fft.cpp)
add_executable(makevariationkerneltest makevariationkerneltest.cpp ../clij2fft/clij2fft.cpp)

target_link_libraries(ffttest ${OpenCV_LIBS} clFFT ${OpenCL_LIBRARY})
target_link_libraries(deconvolutiontest ${OpenCV_LIBS} clFFT ${OpenCL_LIBRARY})
target_link_libraries(convolvetest ${OpenCV_LIBS} clFFT ${OpenCL_LIBRARY})
target_link_libraries(makevariationkerneltest ${OpenCL_LIBRARY} clFFT)