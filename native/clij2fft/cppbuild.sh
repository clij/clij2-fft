#!/usr/bin/env bash
# Scripts to build and install native C++ libraries
# Adapted from https://github.com/bytedeco/javacpp-presets

# Please note this script requires clfft to be on the system
# The script passes the location of clfft to cmake.  We assume
# a "typical" location for clfft for each OS however it may be 
# different on your system.  Check -DCLFFT_LIBRARY_DIR variable
# and change this if clfft is in a different location on your 
# system.

set -eu

if [[ -z "$PLATFORM" ]]; then
    pushd ..
    bash cppbuild.sh "$@" MKLFFTW
    popd
    exit
fi

case $PLATFORM in
    linux-x86_64)
        ln -sf libclFFT.so.2 ../../../lib/linux-x86_64/libclFFT.so
        $CMAKE -DCMAKE_BUILD_TYPE=Release \
               -DCMAKE_INSTALL_PREFIX="../../../lib/linux-x86_64/" \
               -DCMAKE_CXX_COMPILER="/usr/bin/g++" \
               -DCMAKE_CUDA_HOST_COMPILER="/usr/bin/g++" \
               -DOPENCL_INCLUDE_DIR="/usr/local/cuda/include/" \
               -DCLFFT_LIBRARY_DIR="../../../lib/linux-x86_64" ..
        make
        make install
        ;;
    macosx-x86_64)
        CMAKE=(`which cmake`)

        $CMAKE -DCMAKE_BUILD_TYPE=Release \
               -DCMAKE_INSTALL_PREFIX="../../../lib/macosx-x86_64/" \
               -DCMAKE_CXX_COMPILER="g++" \
               -DCMAKE_CUDA_HOST_COMPILER="g++" \
               -DCLFFT_LIBRARY_DIR="../../../lib/macosx-x86_64" ..
        make
        make install
        install_name_tool -change libclFFT.2.dylib @rpath/libclFFT.2.dylib ../../../lib/macosx-x86_64/libclij2fft.dylib
        ;;
    macosx-arm64)
        ln -sf libclFFT.2.dylib ../../../lib/macosx-arm64/libclFFT.dylib

        CMAKE=(`which cmake`)

        $CMAKE -DCMAKE_BUILD_TYPE=Release \
               -DCMAKE_INSTALL_PREFIX="../../../lib/macosx-arm64/" \
               -DCMAKE_CXX_COMPILER="g++" \
               -DCMAKE_CUDA_HOST_COMPILER="g++" \
               -DCLFFT_LIBRARY_DIR="../../../lib/macosx-arm64" \
               -DCMAKE_BUILD_WITH_INSTALL_RPATH=TRUE \
               -DCMAKE_INSTALL_RPATH="@loader_path" ..
        make
        make install
        install_name_tool -change libclFFT.2.dylib @rpath/libclFFT.2.dylib ../../../lib/macosx-arm64/libclij2fft.dylib
        ;;
    windows-x86_64)
        $CMAKE -G"NMake Makefiles" \
               -DCMAKE_BUILD_TYPE=Release \
               -DCMAKE_INSTALL_PREFIX="../../../lib/windows-x86_64/" \
               -DOpenCL_INCLUDE_DIR="C:/OpenCL/include/" \
               -DOpenCL_LIBRARY="C:/OpenCL/lib/OpenCL.lib" \
               -DOPENCL_INCLUDE_DIR="C:/OpenCL/include/" \
               -DCLFFT_LIBRARY_DIR="C:/clFFT/lib64/import/" ..
        nmake
        nmake install
        ;;
    *)
        echo "Error: Platform \"$PLATFORM\" is not supported"
        ;;
esac