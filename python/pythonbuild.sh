#!/bin/bash
# This script builds the native code and copies it to the python directory
# so it can be uploaded to TestPyPI or PyPI.
#
# NOTE: When uploading from one OS, you need to also build the native code on the other OSs,
# then move all libraries (via git) to the machine you are uploading from.

# First, build the native code
echo 'Building native code...'
cd ../native
bash cppbuild.sh
cd ../python

# Now copy the libraries into the python directory.
# (Note: Only libraries specific to this OS will have been built above.
#  You need to build on other platforms, commit, and pull those libraries from git.)
echo 'Copying libraries...'
mkdir lib
cp -r ../lib/* lib
