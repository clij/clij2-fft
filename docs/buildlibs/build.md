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
3.  If step 1 fails check [native/clij2fft/cppbuild.sh](https://github.com/clij/clij2-fft/blob/master/native/clij2fft/cppbuild.sh#L26) and verify that OpenCL and clFFT are installed in the correct locations.
4.  The updated library (clij2fft.so) and dependencies should now be in the ```clij2-fft/lib/linux64/``` directory.

### MacOSX and Mac Silicon Native M1

Some users have successfully built the code in a Mac environment.  Currently we recommend searching the [ImageSC Forum](https://forum.image.sc/search?q=apple%20M1%20clij%20deconvolution) for more information.  Please ask questions on the forum if previous discussions are unclear.    

## Build Java Wrapper and Plugin

1.  A 64 bit c++ compiler is needed to create the wrapper.  In windows from Start menu run ‘x64 Native Tools Command Prompt for VS 2019'.  In Linux/MacOsx make sure a 64 bit c++ compiler (ie gcc) is installed. 

2. Run 'mvn install -Dgpg.skip' from the command line or GUI.
   NOTE:  -Dgpg.skip is needed to skip signing if building for testing or local installation.  
   
4. The maven build is set up to build native wrappers using [javacpp](https://github.com/bytedeco/javacpp) and pack the wrapper into the jar. 

## Build Plugin Only and Install in Fiji

If you do not have a c compiler installed you may want to just build the java part of the plugin. 

1.  Run 'mvn -Djavacpp.skip=true'
2.  Copy the libraries in [lib/win64](https://github.com/clij/clij2-fft/tree/master/lib/win64) to your Fiji installation (Fiji.app/lib/win64).  (on linux copy to and from 'lib/linux64' on mac to and from 'lib/macosx').
3. Copy the output jar ('target/clij2-fft-x.x.jar') to Fiji.app/jars. 
