#!/bin/bash
# This script performs an editable install of the python package then
# copies the required libraries into the conda environment.

pip install -e .
bash copylibsconda.sh
