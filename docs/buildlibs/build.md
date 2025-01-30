# Building the code

## Build and install native libraries

The c++ library 'clij2fft' implements several FFT based algorithms built on top of clFFT.  This library is indepentent of java. 

### Pre-requisites for all operating systems

[clfft](https://github.com/clMathLibraries/clFFT/releases)

### Windows

#### Windows pre-requisites

[Visual Studio Community 2019 c++ compiler](https://visualstudio.microsoft.com/vs/community/)    
[Git for Windows with Bash Terminal](https://gitforwindows.org/)  

#### Windows build Instructions 

1.  From Start menu run 'x64 Native Tools Command Prompt for VS 2019'
2.  From the Command Prompt start a bash shell “C:\Program Files\Git\bin\sh.exe”  
3.  Run [native/cppbuild.sh](https://github.com/clij/clij2-fft/blob/master/native/cppbuild.sh)  
4.  If step 3 fails check [native/clij2fft/cppbuild.sh](https://github.com/clij/clij2-fft/blob/master/native/clij2fft/cppbuild.sh#L26) and verify that OpenCL and clFFT are installed in the correct locations.  
5.  The updated library (clij2fft.dll) and dependencies should now be in the ```clij2-fft/lib/win64/``` directory. 

### Linux

#### Linux Pre-requisites  

[gcc](https://gcc.gnu.org/)

#### Linux/MacOSX Build Instructions

2.  From a bash terminal run [native/cppbuild.sh](https://github.com/clij/clij2-fft/blob/master/native/cppbuild.sh)  
3.  If step 1 fails check [native/clij2fft/cppbuild.sh](https://github.com/clij/clij2-fft/blob/master/native/clij2fft/cppbuild.sh#L28) and verify that OpenCL and clFFT are installed in the correct locations.
4.  The updated library (clij2fft.so) and dependencies should now be in the ```clij2-fft/lib/linux64/``` directory.

### MacOSX and Mac Silicon Native M1

You will need to install `clFFT` from [here](https://formulae.brew.sh/formula/clfft) using [homebrew](https://brew.sh/).  

To build on macosx or mac m1/m2 (arm64) perform the following

1.  From a bash terminal run [native/cppbuild.sh](https://github.com/clij/clij2-fft/blob/master/native/cppbuild.sh)  
2.  If step 1 fails check [native/clij2fft/cppbuild.sh](https://github.com/clij/clij2-fft/blob/master/native/clij2fft/cppbuild.sh#L56) and verify that OpenCL and clFFT are installed in the correct locations.

The macosx the native library libclij2fft needs to be modified with ```install_name_tool``` in order for it to find ```clFFT``` in it’s current directory (ie when both are installed in a conda environment and are in /mambaforge/envs/current_environment/lib)

```
install_name_tool -change libclFFT.2.dylib @rpath/libclFFT.2.dylib ../../../lib/macosx-arm64/libclij2fft.dylib
```

If targeting both macosx and macosx-arm64 we need to build a universal binary using 'lipo'

```
lipo -create -output lib/macosx-universal2/libclFFT.dylib lib/macosx/libclFFT.dylib lib/macosx-arm64/libclFFT.dylib`
```
3.  The updated library (clij2fft.dll) and dependencies should now be in the ```clij2-fft/lib/macosx/``` directory. 

We recommend searching the [ImageSC Forum](https://forum.image.sc/search?q=apple%20M1%20clij%20deconvolution) for more information.  Please ask questions on the forum if previous discussions are unclear.  

## Build Java Wrapper and Plugin

1.  A 64 bit c++ compiler is needed to create the wrapper.  In windows from Start menu run ‘x64 Native Tools Command Prompt for VS 2019'.  In Linux/MacOsx make sure a 64 bit c++ compiler (ie gcc) is installed. 

2. Run 'mvn install -Dgpg.skip' from the command line or GUI.
   NOTE:  -Dgpg.skip is needed to skip signing if building for testing or local installation.  
   
3. If the javacpp part of the native build is successful a wrapper libary called ```jniclij2fftWrapper``` will be created and put into the ```target/classes/net/haesleinhuepf/clijx/plugins/$PLATFORM/``` directory.  We want to copy the library into the top level lib folder (currently we store the libraries in github).  That can be done with the following bash snippet....

```
case $PLATFORM in
  linux-x86_64
        cp target/classes/net/haesleinhuepf/clijx/plugins/$PLATFORM/libjniclij2fftWrapper.so lib/linux64/
      ;;
    macosx-*)
      echo "copy jni wrapper"
        cp target/classes/net/haesinhuepf/clijx/plugins/$PLATFORM/libjniclij2fftWrapper.dylib lib/^Smacosx/
      ;;
    windows-x86_64)
        echo "copy jni wriapper"
        cp target/classes/net/haesleinhuepf/clijx/plugins/windows-x86_64/jniclij2fftWrapper.dll lib/win64/
      ;;
    *)
      echo "Error: Platform \"$PLATFORM\" is not supported"
      ;;
```

### Push to Fiji update site 

There is a fiji update site at ```https://sites.imagej.net/clijx-deconvolution``` where we push the artifacts.  After a build you need to 

1.  Copy ```clij2-fft_{version}.jar``` to the ```Fiji.app/plugins``` directory.
2.  Copy ```lib/{platform}/{lib}clij2fft.{dll/so/dylib}``` to the corresponding location in ```Fiji.app/lib```.
3.  Copy ```lib/{platform}/{lib}jniclij2fftWrapper.{dll/so/dylib}``` to the corresponding location in ```Fiji.app/lib```.

Then perform Fiji update site steps.  A description of how to work with update sites can be found [here](https://imagej.net/update-sites/setup).

## Build Plugin Only and Install in Fiji

If you do not have a c compiler installed you may want to just build the java part of the plugin. 

1.  Run 'mvn -Djavacpp.skip=true'
2.  Copy the libraries in [lib/win64](https://github.com/clij/clij2-fft/tree/master/lib/win64) to your Fiji installation (Fiji.app/lib/win64).  (on linux copy to and from 'lib/linux64' on mac to and from 'lib/macosx').
3. Copy the output jar ('target/clij2-fft-x.x.jar') to Fiji.app/jars. 
