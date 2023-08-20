rm -rf macosx-universal2/*
lipo -create -output macosx-universal2/libclFFT.2.dylib macosx/libclFFT.2.dylib macosx-arm64/libclFFT.2.dylib
lipo -create -output macosx-universal2/libclij2fft.dylib macosx/libclij2fft.dylib macosx-arm64/libclij2fft.dylib
