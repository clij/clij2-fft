# Building the code

## Overview
The C++ library **clij2fft** implements several FFT-based algorithms built on top of [clFFT](https://github.com/clMathLibraries/clFFT). This library is independent of Java.

**Note:** For Java, native libraries are now included in the JAR, so there is no need to manually copy them to an update site or Fiji installation.

---

## Building Native Libraries

### **Using GitHub Actions (Recommended)**
- **No local native builds are required or recommended.**
- If you modify the native code, push your changes to GitHub with the commit message containing `[build natives]`.
- GitHub Actions will build the native libraries for all platforms and commit the updated libraries back to the repo.
- Pull the latest changes to get the updated native libraries.

## Java release cycle

### Building the Java Plugin

If you modified the native, code, **build the new natives following the new workflow**. Then run the following Maven command to build the jar:
   ```sh
   mvn clean package "-Dgpg.skip=true" "-Djavacpp.skip=true"
   ```

### Installation of the Java plugin in your local Fiji

Once the Java plugin is built, copy the JAR (`./target/clij2-fft-{version}.jar`) to your Fiji installation (`Fiji.app/plugins/`).

### Java releases to scijava maven
To create a release:
1. Ensure all native libraries are up-to-date (built via GitHub Actions).
2. Use the `release-version.sh` script from [scijava-scripts](https://github.com/scijava/scijava-scripts) with the following arguments to skip native builds and GPG signing:
   ```sh
   ./release-version.sh -Darguments="-Dgpg.skip=true -Djavacpp.skip=true"
   ```

## Troubleshooting
- Search the [Image.sc Forum](https://forum.image.sc/search?q=apple%20M1%20clij%20deconvolution) for more information.
- Ask questions on the forum if previous discussions are unclear.

---
<details>
<summary>Building Native Libraries Locally - Legacy (Click to expand)</summary>
## Building Native Libraries - Legacy

### **Old Workflow (Local Builds)**
If you still need to build natives locally (e.g., for testing or development), follow the instructions below.

#### **Prerequisites for All Operating Systems**
- [clFFT](https://github.com/clMathLibraries/clFFT/releases)

---

#### **Windows**
##### **Prerequisites**
- [Visual Studio Community C++ Compiler](https://visualstudio.microsoft.com/vs/community/)
- [Git for Windows with Bash Terminal](https://gitforwindows.org/)

##### **Build Instructions**
1. From the Start menu, run **'x64 Native Tools Command Prompt for VS'**.
2. From the Command Prompt, start a bash shell: `"C:\Program Files\Git\bin\sh.exe"`.
3. Run [`./native/cppbuild.sh`](./native/cppbuild.sh).
4. If step 3 fails, check [`./native/clij2fft/cppbuild.sh`](./native/clij2fft/cppbuild.sh) and verify that OpenCL and clFFT are installed in the correct locations.
5. The updated library (`clij2fft.dll`) and dependencies will be in the `./lib/windows-x86_64/` directory.

#### **Linux**
##### **Prerequisites**
- [gcc](https://gcc.gnu.org/)

##### **Build Instructions**
1. From a bash terminal, run [`./native/cppbuild.sh`](./native/cppbuild.sh).
2. If step 1 fails, check [`./native/clij2fft/cppbuild.sh`](./native/clij2fft/cppbuild.sh) and verify that OpenCL and clFFT are installed in the correct locations.
3. The updated library (`clij2fft.so`) and dependencies will be in the `./lib/linux-x86_64/` directory.

#### **macOS (Intel and Apple Silicon M1/M2/M3)**
##### **Prerequisites**
- `clFFT` needs to be built from source
  ```sh
   brew install cmake || true
   git clone https://github.com/clMathLibraries/clFFT.git
   cd clFFT/src
   sed -i.bak 's/cmake_minimum_required( VERSION 3.1 )/cmake_minimum_required( VERSION 3.6 )/' CMakeLists.txt
   mkdir build && cd build
   cmake .. -DCMAKE_BUILD_TYPE=Release -DBUILD_SHARED_LIBS=ON -DCMAKE_INSTALL_PREFIX=${{ matrix.lib_prefix }}
   make -j$(sysctl -n hw.ncpu) && sudo make install
  ```

##### **Build Instructions**
1. From a bash terminal, run [`./native/cppbuild.sh`](./native/cppbuild.sh).
2. If step 1 fails, check [`./native/clij2fft/cppbuild.sh`](./native/clij2fft/cppbuild.sh) and verify that OpenCL and clFFT are installed in the correct locations.
3. For macOS, the native library `libclij2fft` needs to be modified with `install_name_tool` to find `clFFT` in its current directory:
   ```sh
   install_name_tool -change libclFFT.2.dylib @rpath/libclFFT.2.dylib ../../../lib/macosx-arm64/libclij2fft.dylib
   ```
4. If targeting both Intel and Apple Silicon, you can build a universal binary using `lipo`:
   ```sh
   lipo -create -output lib/macosx-universal2/libclFFT.dylib lib/macosx-x86_64/libclFFT.dylib lib/macosx-arm64/libclFFT.dylib
   ```
5. The updated library (`clij2fft.dylib`) and dependencies will be in the `./lib/macosx-x86_64/` or `./lib/macosx-arm64/` directory.

</details>
