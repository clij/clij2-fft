#!/bin/bash
# This script automatically detects the active conda/mamba environment and copies 
# native libraries to the appropriate location for editable mode installation.

KERNEL=(`uname -s | tr [A-Z] [a-z]`)
ARCH=(`uname -m | tr [A-Z] [a-z]`)

# Detect active conda environment
if [ -z "$CONDA_PREFIX" ]; then
    echo "Error: No conda/mamba environment is active. Please activate an environment first."
    exit 1
fi

echo "Detected active environment: $CONDA_PREFIX"

# Determine target directory based on OS
case $KERNEL in
    linux)
        echo "OS: Linux"
        TARGET_DIR="$CONDA_PREFIX/lib"
        SOURCE_DIR="./lib/linux64"
        ;;
    darwin)
        echo "OS: macOS ($ARCH)"
        TARGET_DIR="$CONDA_PREFIX/lib"
        case $ARCH in
            x86_64)
                SOURCE_DIR="./lib/macosx"
                ;;
            arm64)
                SOURCE_DIR="./lib/macosx-arm64"
                ;;
            *)
                echo "Error: Unsupported macOS architecture: $ARCH"
                exit 1
                ;;
        esac
        ;;
    mingw* | msys* | cygwin*)
        echo "OS: Windows"
        TARGET_DIR="$CONDA_PREFIX/Library/bin"
        SOURCE_DIR="./lib/windows-x86_64"
        ;;
    *)
        echo "Error: Unsupported OS: $KERNEL"
        exit 1
        ;;
esac

# Verify source directory exists
if [ ! -d "$SOURCE_DIR" ]; then
    echo "Error: Source directory not found: $SOURCE_DIR"
    exit 1
fi

# Create target directory if it doesn't exist
mkdir -p "$TARGET_DIR"

# Copy libraries
echo "Copying libraries from $SOURCE_DIR to $TARGET_DIR"
cp -r "$SOURCE_DIR"/* "$TARGET_DIR/"

if [ $? -eq 0 ]; then
    echo "Libraries copied successfully!"
else
    echo "Error: Failed to copy libraries"
    exit 1
fi

