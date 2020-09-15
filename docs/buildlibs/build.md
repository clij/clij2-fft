# Building the code

## Build and install native libraries

The c++ library 'clij2fft' implements several FFT based algorithms built on clFFT.  This library is indepentent of java. 

### Windows

#### Pre-requisites

[Visual Studio Community 2019 c++ compiler](https://visualstudio.microsoft.com/vs/community/)    
[Git for Windows with Bash Terminal](https://gitforwindows.org/)  

#### Build Instructions 

1.  From Start menu run 'x64 Native Tools Command Prompt for VS 2019'
2.  From the Command Prompt start a bash shell “C:\Program Files\Git\bin\sh.exe”  
3.  Run [native/cppbuild.sh](https://github.com/clij/clij2-fft/blob/master/native/cppbuild.sh)  
4.  If step 3 fails check [native/clij2fft/cppbuild.sh](https://github.com/clij/clij2-fft/blob/master/native/clij2fft/cppbuild.sh#L26) and verify that OpenCL and clFFT are installed in the correction location.  

### Linux

#### Linux Pre-requisites  

[gcc](https://gcc.gnu.org/)

#### Linux Build Instructions

2.  Run [native/cppbuild.sh](https://github.com/clij/clij2-fft/blob/master/native/cppbuild.sh)  
3.  If step 3 fails check [native/clij2fft/cppbuild.sh](https://github.com/clij/clij2-fft/blob/master/native/clij2fft/cppbuild.sh#L26) and verify that OpenCL and clFFT are installed in the correction location.  

## Build Java Wrapper and Plugin

1.  A 64 bit c++ compiler is needed to create the wrapper.  In windows from Start menu run ‘x64 Native Tools Command Prompt for VS 2019'.  In Linux/MacOsx make sure a 64 bit c++ compiler (ie gcc) is installed. 

2. Simply run 'mvn' from the command line or GUI.   The maven build is set up to build native wrappers using [javacpp](https://github.com/bytedeco/javacpp) and pack the wrapper in the jar. 

## Build Plugin Only and Install in Fiji

If you do not have a c compiler installed you may want to just build the java part of the plugin. 

1.  Run 'mvn -Djavacpp.skip=true'
2.  Copy the libraries in [lib/win64](https://github.com/clij/clij2-fft/tree/master/lib/win64) to your Fiji installation (Fiji.app/lib/win64).  (on linux copy to and from 'lib/linux64' on mac to and from 'lib/macosx').
3. Copy the output jar ('target/clij2-fft-x.x.jar') to Fiji.app/jars. 