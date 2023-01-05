# this script builds the native code then copies it to the python directory
# so we can then upload to testpypi or pypi.
# NOTE:  When uploading from one OS need to also build the native code on the other OSs then
# move all (can do this via git) to the machine that you are uploading from

# first build native code
echo 'building native code'
cd ../native
bash cppbuild.sh
cd ../python

# now copy the libraries so that they are under the python directory 
# (remember only ones specific to this OS will have been built above, need to build push, and pull the others from git)
echo 'copying libs'
mkdir lib
cp -r ../lib/* lib
