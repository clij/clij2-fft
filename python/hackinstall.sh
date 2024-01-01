#!/bin/bash
# currently unable to get setup.py to copy
# the c library properly using relative path
# so this script is a hack that copies the lib directory
# then installs the clij2fft python package in editable mode

# script may be a useful template to follow if trying to install in editable on your system
# but remember to edit editableinstall.sh to point to the correct location of the lib directory 
# (i.e. the conda/mamba location on your system)

bash pythonbuild.sh

bash editableinstall.sh