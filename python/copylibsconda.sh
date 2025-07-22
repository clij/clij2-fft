#!/bin/bash
# this script is used when installing in editable mode, it copies the native libraries to the 
# location they are needed NOTE this script needs to be customized for your system and environment 
# name

KERNEL=(`uname -s | tr [A-Z] [a-z]`)
ARCH=(`uname -m | tr [A-Z] [a-z]`)

# when we are installing in editable mode 
# we have to put the libraries somewhere where python can find them. For me
# (using anaconda) that was anaconda3\envs\(environment)\Library\bin on windows
# and /home/bnorthan/anaconda3/envs/(environment)/lib/ on linux
# YOU WILL HAVE TO CHANGE THE BELOW PATHS FOR YOUR SYSTEM
case $KERNEL in
    linux)
        echo 'linux'
        cp -r ./lib/linux64/* /home/bnorthan/mambaforge/envs/decon-dl-env2/lib/
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
        esac
        ;;
    *)
    echo 'not linux or mac (probably Windows)'
        cp -r ./lib/win64/* /c/Users/bnort/miniconda3/envs/decon_env_ts_python311/Library/bin/
    ;;
esac

