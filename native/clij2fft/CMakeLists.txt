cmake_minimum_required(VERSION 3.16)

project(cllij2fft)

find_package(OpenCL)

if (UNIX OR WIN32 AND NOT APPLE)
    FIND_PATH(OPENCL_INCLUDE_DIR ENV{OPENCL_INCLUDE_DIR} [DOC "Open CL include path" ])
    include_directories( ${OPENCL_INCLUDE_DIR} )
endif()

find_package(Python3 COMPONENTS Interpreter)

add_custom_target(
    generate_preamble ALL
    COMMAND ${Python3_EXECUTABLE} ${CMAKE_CURRENT_SOURCE_DIR}/stringify.py ${CMAKE_CURRENT_SOURCE_DIR}/totalvariationterm.cl ${CMAKE_CURRENT_SOURCE_DIR}/kernels/
    COMMENT "Converting totalvarationterm.cl to string."
)

SET(CMAKE_BUILD_RPATH_USE_ORIGIN TRUE)
SET(CMAKE_INSTALL_RPATH $ORIGIN)

# find clFFT path
FIND_PATH(CLFFT_LIBRARY_DIR $ENV{CLFFT_LIBRARY_DIR} [DOC "CLFFT library path" ])

link_directories(/Users/haase/code/ops-experiments/ops-experiments-opencl/native ${CLFFT_LIBRARY_DIR})
add_library(clij2fft SHARED clij2fft.cpp)
target_link_libraries(clij2fft clFFT ${OpenCL_LIBRARY})

install(TARGETS clij2fft DESTINATION .)
