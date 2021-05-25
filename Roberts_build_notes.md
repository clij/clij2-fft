On MacOS:
```
cd native
chmod u+x cppbuild.sh
./cppbuild.sh
cd clij2fft
cd ccpbuild
make
```

From within IntellIJ: run `mvn clean install -DskipTests`. 

Copy over `libclFFT.dylib`, `libjniclij2fftWrapper.dylib` from `target/classes/net/..../` to `Fiji.app/native/macosx`.

Copy over `clij2fft.dylib` from `/native/clij2fft/cppbuild` to `Fiji.app/native/macosx`.