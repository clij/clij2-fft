#!/usr/bin/env bash

# this script builds the native part of clij2-fft

# call the script that builds the c libraries
cd native
source cppbuild.sh
cd ..

# now call mvn as part of the maven build the javacpp wrapper will be generated
mvn

# copy the javacpp wrapper to the resource folder
case $PLATFORM in
    linux-x86_64)
			cp target/classes/net/haesleinhuepf/clijx/plugins/$PLATFORM/libjniclij2fftWrapper.so lib/linux64/
      ;;
    macosx-*)
      echo "TODO"
      ;;
    windows-x86_64)
       	echo "copy jni wriapper"
	cp target/classes/net/haesleinhuepf/clijx/plugins/windows-x86_64/jniclij2fftWrapper.dll lib/win64/
      ;;
    *)
      echo "Error: Platform \"$PLATFORM\" is not supported"
      ;;
esac

