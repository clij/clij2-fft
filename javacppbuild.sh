#!/usr/bin/env bash

# this script builds the native part of clij2-fft

# call the script that builds the c libraries
# this script calls cmake and sets the install directory to the lib/{platform} directory
# so after the build the libraries should be in the correct location
cd native
source cppbuild.sh
cd ..

# now call mvn as part of the maven build the javacpp wrapper will be generated
# this builds both the java and native wrapper part of the library
# after the build the native wrapper will be in the target/classes/net/haesleinhuepf/clijx/plugins/{platform} directory
mvn install -Dgpg.skip

# now copy the native wrapper from the target directory to the lib/{platform} directory
case $PLATFORM in
    linux-x86_64)
      echo "copy jni wrapper"
	    cp target/classes/net/haesleinhuepf/clijx/plugins/$PLATFORM/libjniclij2fftWrapper.so lib/linux64/
      ;;
    macosx-x86_64)
      echo "copy jni wrapper"
	    cp target/classes/net/haesleinhuepf/clijx/plugins/$PLATFORM/libjniclij2fftWrapper.dylib lib/macosx/
      ;;
    macosx-arm64)
      echo "copy jni wrapper"
	    cp target/classes/net/haesleinhuepf/clijx/plugins/$PLATFORM/libjniclij2fftWrapper.dylib lib/macosx-arm64/
      ;;
    windows-x86_64)
       	echo "copy jni wriapper"
	      cp target/classes/net/haesleinhuepf/clijx/plugins/windows-x86_64/jniclij2fftWrapper.dll lib/win64/
      ;;
    *)
      echo "Error: Platform \"$PLATFORM\" is not supported"
      ;;
esac

