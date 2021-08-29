# currently unable to get setup.py to copy
# the c library properly using relative path
# so this script is a hack that copies the lib directory
# installs the clij2fft python package then
# deletes the lib directory

mkdir lib
cp -r ../lib/* lib

# note if we are installing in editable mode 
# we have to put the libraries somewhere where python can find them. For me
# that was anaconda3\envs\napari-env\Library\bin on windows
# and /home/bnorthan/anaconda3/envs/tnia_deconware/lib/ on linux
KERNEL=(`uname -s | tr [A-Z] [a-z]`)

case $KERNEL in
    linux)
    cp -r ./lib/linux64/* /home/bnorthan/anaconda3/envs/tnia_deconware/lib/
    echo 'linux'
    ;;
    *)
    echo 'not linux'
    ;;
esac

pip install -e .
#rm -rf lib

