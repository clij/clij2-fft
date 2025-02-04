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
        ln -sf libclFFT.so.2 ../../../lib/linux64/libclFFT.so
        $CMAKE -DCMAKE_BUILD_TYPE=Release \
               -DCMAKE_INSTALL_PREFIX="../../../lib/linux64/" \
               -DCMAKE_CXX_COMPILER="/usr/bin/g++" \
               -DCMAKE_CUDA_HOST_COMPILER="/usr/bin/g++" \
               -DOPENCL_INCLUDE_DIR="/usr/local/cuda/include/" \
		-DCLFFT_LIBRARY_DIR="../../../lib/linux64" .. 
        make
        make install
        ;;
    macosx-x86_64)
        # the following line might not be necessary if make would be properly installed in the path
        CMAKE=(`which cmake`)
        
        $CMAKE -DCMAKE_BUILD_TYPE=Release \
               -DCMAKE_INSTALL_PREFIX="../../../lib/macosx/" \
               -DCMAKE_CXX_COMPILER="g++" \
               -DCMAKE_CUDA_HOST_COMPILER="g++" \
		       -DCLFFT_LIBRARY_DIR="../../../lib/macosx" ..
        make
        make install
	    install_name_tool -change libclFFT.2.dylib @rpath/libclFFT.2.dylib ../../../lib/macosx/libclij2fft.dylib
        ;;
    macosx-arm64)
        ln -sf libclFFT.2.dylib ../../../lib/macosx-arm64/libclFFT.dylib
        
        # the following line might not be necessary if make would be properly installed in the path
        CMAKE=(`which cmake`)
        
        $CMAKE -DCMAKE_BUILD_TYPE=Release \
               -DCMAKE_INSTALL_PREFIX="../../../lib/macosx-arm64/" \
               -DCMAKE_CXX_COMPILER="g++" \
               -DCMAKE_CUDA_HOST_COMPILER="g++" \
		       -DCLFFT_LIBRARY_DIR="../../../lib/macosx-arm64" ..
        make
        make install
	    install_name_tool -change libclFFT.2.dylib @rpath/libclFFT.2.dylib ../../../lib/macosx-arm64/libclij2fft.dylib
        ;;
    windows-x86_64)
        $CMAKE -G"NMake Makefiles" \
		       -DCMAKE_BUILD_TYPE=Release \
               -DCMAKE_INSTALL_PREFIX="../../../lib/win64/" \
               -DOPENCL_INCLUDE_DIR="C:/Program Files/NVIDIA GPU Computing Toolkit/CUDA/v11.2/include/" \
		       -DCLFFT_LIBRARY_DIR="C:/OpenCL/clFFT-2.12.2-Windows-x64/lib64/import/" .. 
        nmake
        nmake install
        ;;
    *)
        echo "Error: Platform \"$PLATFORM\" is not supported"
        ;;
esac


