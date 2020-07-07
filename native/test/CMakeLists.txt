project(test)

find_package(TIFF)
find_package(OpenCL)

# find Open CV
FIND_PACKAGE( OpenCV REQUIRED PATHS ${OPENCV_INSTALL_DIR})
INCLUDE_DIRECTORIES( ${OpenCV_INCLUDE_DIRS} )

# find clFFT path
FIND_PATH(CLFFT_LIBRARY_DIR $ENV{CLFFT_LIBRARY_DIR} [DOC "CLFFT library path"])

link_directories(${CLFFT_LIBRARY_DIR})

add_executable(test test.cpp tifstack.cpp ../clij2fft/clij2fft.cpp)

target_link_libraries(test ${TIFF_LIBRARIES} ${OpenCV_LIBS} clFFT ${OpenCL_LIBRARY})