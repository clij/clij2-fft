# currently unable to get setup.py to copy
# the c library properly using relative path
# so this script is a hack that copies the lib directory
# installs the clij2fft python package then
# deletes the lib directory

mkdir lib
cp -r ../lib/* lib
pip install .
rm -rf lib
