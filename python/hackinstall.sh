# currently unable to get setup.py to copy
# the c library properly using relative path
# so this script is a hack that copies the lib directory
# installs the clij2fft python package then
# deletes the lib directory

# script may also be a useful template to follow if trying to install on your system

# first build native code
echo 'building native code'
cd ../native
bash cppbuild.sh
cd ../python

# now copy the libraries so that they are under the python directory 
echo 'copying libs'
mkdir lib
cp -r ../lib/* lib

bash editableinstall.sh