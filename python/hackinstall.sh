# currently unable to get setup.py to copy
# the c library properly using relative path
# so this script is a hack that copies the lib directory
# installs the clij2fft python package then
# deletes the lib directory

echo 'building native code'
cd ../native
bash cppbuild.sh
cd ../python
# currently

echo 'copying libs'

mkdir lib
cp -r ../lib/* lib

# note if we are installing in editable mode 
# we have to put the libraries somewhere where python can find them. For me
# that was anaconda3\envs\napari-env\Library\bin on windows
# and /home/bnorthan/anaconda3/envs/tnia_deconware/lib/ on linux
KERNEL=(`uname -s | tr [A-Z] [a-z]`)
ARCH=(`uname -m | tr [A-Z] [a-z]`)

case $KERNEL in
    linux)
        echo 'linux'
        cp -r ./lib/linux64/* /home/bnorthan/anaconda3/envs/tnia_deconware/lib/
        ;;
    darwin)
        echo 'mac'
        case $ARCH in
        x86_64)
            echo 'macosx-x86_84'
            cp -r ./lib/macosx/* /home/bnorthan/anaconda3/envs/tnia_deconware/lib/
            ;;
        arm84)
            echo 'macosx-arm64'
            cp -r ./lib/macosx-arm64/* /home/bnorthan/anaconda3/envs/tnia_deconware/lib/
            ;;
        ;;
    *)
    echo 'not linux'
    cp -r ./lib/win64/* /c/Users/bnort/miniconda3/envs/tnia_deconware/Library/bin/
    ;;
esac

pip install -e .
#rm -rf lib

