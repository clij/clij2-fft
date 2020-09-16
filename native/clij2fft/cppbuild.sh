#!/usr/bin/env bash
# Scripts to build and install native C++ libraries
# Adapted from https://github.com/bytedeco/javacpp-presets
set -eu

if [[ -z "$PLATFORM" ]]; then
    pushd ..
    bash cppbuild.sh "$@" MKLFFTW
    popd
    exit
fi

case $PLATFORM in
    linux-x86_64)
        $CMAKE -DCMAKE_BUILD_TYPE=Release \
               -DCMAKE_INSTALL_PREFIX="../../../lib/linux64/" \
               -DCMAKE_CXX_COMPILER="/usr/bin/g++" \
               -DCMAKE_CUDA_HOST_COMPILER="/usr/bin/g++" \
		-DCLFFT_LIBRARY_DIR="/opt/OpenCL/clFFT-2.12.2-Linux-x64/lib64/" .. 
        make
        make install
        ;;
    macosx-*)
        # the following line might not be necessary if make would be properly installed in the path
        CMAKE=/Applications/CMake.app/Contents/bin/cmake
        
        $CMAKE -DCMAKE_BUILD_TYPE=Release \
               -DCMAKE_INSTALL_PREFIX="../../../lib/macosx/" \
               -DCMAKE_CXX_COMPILER="g++" \
               -DCMAKE_CUDA_HOST_COMPILER="g++" \
		-DCLFFT_LIBRARY_DIR="../../../lib/macosx/" ..
        ;;
    windows-x86_64)
        $CMAKE -G"NMake Makefiles" \
		       -DCMAKE_BUILD_TYPE=Release \
               -DCMAKE_INSTALL_PREFIX="../../../lib/win64/" \
               -DOPENCL_INCLUDE_DIR="C:/Program Files/NVIDIA GPU Computing Toolkit/CUDA/v10.0/include/" \
		       -DCLFFT_LIBRARY_DIR="C:/OpenCL/clFFT-2.12.2-Windows-x64/lib64/import/" .. 
        nmake
        nmake install
        ;;
    *)
        echo "Error: Platform \"$PLATFORM\" is not supported"
        ;;
esac


