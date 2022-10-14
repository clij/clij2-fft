#!/usr/bin/env bash
# Scripts to build and install native C++ libraries
# Adapted from https://github.com/bytedeco/javacpp-presets
set -eu

which cmake3 &> /dev/null && CMAKE3="cmake3" || CMAKE3="cmake"
[[ -z ${CMAKE:-} ]] && CMAKE=$CMAKE3
[[ -z ${MAKEJ:-} ]] && MAKEJ=4
[[ -z ${OLDCC:-} ]] && OLDCC="gcc"
[[ -z ${OLDCXX:-} ]] && OLDCXX="g++"
[[ -z ${OLDFC:-} ]] && OLDFC="gfortran"

KERNEL=(`uname -s | tr [A-Z] [a-z]`)
ARCH=(`uname -m | tr [A-Z] [a-z]`)
case $KERNEL in
    darwin)
        OS=macosx
        ;;
    mingw32*)
        OS=windows
        KERNEL=windows
        ARCH=x86
        ;;
    mingw64*)
        OS=windows
        KERNEL=windows
        ARCH=x86_64
        ;;
    *)
        OS=$KERNEL
        ;;
esac
case $ARCH in
    arm32)
        ARCH=arm32
        ;;
    arm64)
        ARCH=arm64
        ;;
    i386|i486|i586|i686)
        ARCH=x86
        ;;
    amd64|x86-64)
        ARCH=x86_64
        ;;
esac
PLATFORM=$OS-$ARCH
EXTENSION=
echo "Detected platform \"$PLATFORM\""

while [[ $# > 0 ]]; do
    case "$1" in
        -platform=*)
            PLATFORM="${1#-platform=}"
            ;;
        -platform)
            shift
            PLATFORM="$1"
            ;;
        -extension=*)
            EXTENSION="${1#-extension=}"
            ;;
        -extension)
            shift
            EXTENSION="$1"
            ;;
        *)
            PROJECTS+=("$1")
            ;;
    esac
    shift
done

echo -n "Building for platform \"$PLATFORM\""
if [[ -n "$EXTENSION" ]]; then
    echo -n " with extension \"$EXTENSION\""
fi
echo

TOP_PATH=`pwd`

if [[ -z ${PROJECTS:-} ]]; then
    PROJECTS=(clij2fft)
fi

for PROJECT in ${PROJECTS[@]}; do
    if [[ ! -d $PROJECT ]]; then
        echo "Warning: Project \"$PROJECT\" not found"
    else
        echo "Installing \"$PROJECT\""
        mkdir -p "$PROJECT/cppbuild"
        pushd "$PROJECT/cppbuild"
        source "../cppbuild.sh"
        popd
    fi

done

# we need to copy the libs to another location under the python directory
# in order for the setup script to find them
cp -r ../lib/* ../python/lib
