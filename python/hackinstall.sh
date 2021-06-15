# currently unable to get setup.py to copy
# the c library properly using relative path
# so this script is a hack that copies the lib directory
# installs the clij2fft python package then
# deletes the lib directory

mkdir lib
cp -r ../lib/* lib

# note we are installing in editable mode, however it seems in this case
# we have to put the libraries somewhere where python can find them. For me
# that was anaconda3\envs\napari-env\Library\bin
pip install -e .
rm -rf lib
