#!/usr/bin/python

import sys, os

for i in range(100):
    print('we called python')

# stringify function for clij kernel
def stringify_clij_code(fname, fout, prefix="cle_"):
    # get kernel name
    name = os.path.split(fname)[-1].split('_x.cl')[0].split('.cl')[0]
    # generate header name
    new_fname = os.path.join(fout, "{0}{1}.h".format(prefix,name))
    # I/O kernel into header
    with open(new_fname, 'w') as output_file, open(fname, 'r') as input_file:
        output_file.write("#ifndef __{0}{1}_h\n".format(prefix, name))
        output_file.write("#define __{0}{1}_h ".format(prefix, name))
        for line in input_file:
            clean_line = list(filter(None, line.split('\n')))
            if len(clean_line) > 0:
                clean_line = clean_line[0].replace(r'"', r'\"')
                # BN: To get this working I had to add an extra space and backslash to the end of the line
                clean_line = "\"{0}\\n\" \\\n".format(clean_line)
                #clean_line = clean_line
                print('line is: ',clean_line)
                output_file.write(clean_line)
            else:
                output_file.write("\"\\n\" \\\n")
        output_file.write("\n")
        output_file.write("#endif //__{0}{1}_h\n".format(prefix, name))

# check output folder existance
if not os.path.isdir(sys.argv[2]):
    os.makedirs(sys.argv[2])

# process folder/file
if os.path.isdir(sys.argv[1]):
    for filename in os.listdir(sys.argv[1]):
        if filename.endswith(".cl"):
            stringify_clij_code("{0}/{1}".format(sys.argv[1],filename), sys.argv[2])
else:
    stringify_clij_code(sys.argv[1], sys.argv[2])
    