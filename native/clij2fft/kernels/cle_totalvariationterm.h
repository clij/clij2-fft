#ifndef __cle_totalvariationterm_h
#define __cle_totalvariationterm_h "\n" \
"float hypot3(float a, float b, float c) {\n" \
"    return sqrt(a*a+b*b+c*c);\n" \
"}\n" \
"\n" \
"float m(float a, float b) {\n" \
"    if (a < 0 && b < 0) {\n" \
"        if (a >= b) return a;\n" \
"        return b;\n" \
"    }\n" \
"    if (a > 0 && b > 0) {\n" \
"        if (a < b) return a;\n" \
"        return b;\n" \
"    }\n" \
"    return 0.0;\n" \
"}\n" \
"\n" \
"/**\n" \
" * This kernel implements the correction term for Richardson-Lucy algorithm with total variation\n" \
" * regularization for 3D confocal microscope deconvolution Microsc Res Rech 2006\n" \
" * Apr; 69(4)- 260-6\n" \
" *\n" \
" * estimate - current estimate of RL algorithm\n" \
" * correction - current correction of RL algorithm\n" \
" * variation - the correction will be modified with the total variation constraint and written to 'variation'\n" \
" *\n" \
" **/\n" \
"#pragma OPENCL EXTENSION cl_khr_fp64 : enable \n" \
"__kernel void totalVariationTerm(  __global float *estimate, global float * correction, __global float * variation,\n" \
"                    const unsigned int Nx, const unsigned int Ny, const unsigned int Nz, float hx, float hy, float hz, float regularizationFactor)  \n" \
"{                                             \n" \
"    \n" \
"    int NxNy=Nx*Ny;\n" \
"    \n" \
"    float FLOAT32_EPS = 0.0;\n" \
"    \n" \
"    //Get 3D global thread IDs               \n" \
"    int i = get_global_id(0);              \n" \
"    int j = get_global_id(1);              \n" \
"    int k = get_global_id(2);     \n" \
"\n" \
"    int im1, ip1, jm1, jp1, km1, kp1;\n" \
"\n" \
"    float fip, fim, fjp, fjm, fkp, fkm, fijk;\n" \
"    float fimkm, fipkm, fjmkm, fjpkm, fimjm, fipjm, fimkp, fimjp;\n" \
"    float aim, bjm, ckm, aijk, bijk, cijk;\n" \
"    float Dxpf, Dxmf, Dypf, Dymf, Dzpf, Dzmf;\n" \
"    float Dxma, Dymb, Dzmc;\n" \
" \n" \
"    //Make sure we do not go out of bounds\n" \
"    i = (i>Nx-1 ? Nx-1 : i);\n" \
"    j = (j>Ny-1 ? Ny-1 : j);\n" \
"    k = (k>Nz-1 ? Nz-1 : k);\n" \
"    \n" \
"    im1 = (i > 0 ? i - 1 : 0);\n" \
"	ip1 = (i + 1 == Nx ? i : i + 1);\n" \
"\n" \
"    jm1 = (j > 0 ? j - 1 : 0);\n" \
"	jp1 = (j + 1 == Ny ? j : j + 1);\n" \
"    \n" \
"    km1 = (k > 0 ? k - 1 : 0);\n" \
"	kp1 = (k + 1 == Nz ? k : k + 1);\n" \
"    \n" \
"    fimjm = estimate[ im1 + jm1*Nx + k*NxNy];\n" \
"    fim = estimate[ im1+ j*Nx+ k*NxNy];\n" \
"    fimkm =  estimate[im1+ j*Nx+ km1*NxNy];\n" \
"    fimkp = estimate[ im1+ j*Nx+ kp1*NxNy];\n" \
"    fimjp = estimate[ im1+ jp1*Nx+ k*NxNy];\n" \
"\n" \
"    fjmkm = estimate[ i+ jm1*Nx+ km1*NxNy];\n" \
"    fjm = estimate[ i+ jm1*Nx+ k*NxNy];\n" \
"    // fjmkp = estimate[ i+ jm1*Nx+ kp1*NxNy];\n" \
"\n" \
"    fkm = estimate[ i+ j*Nx+ km1*NxNy];\n" \
"    fijk = estimate[ i+ j*Nx+ k*NxNy];\n" \
"    fkp = estimate[ i+ j*Nx+ kp1*NxNy];\n" \
"\n" \
"    fjpkm = estimate[ i+ jp1*Nx+ km1*NxNy];\n" \
"    fjp = estimate[ i+ jp1*Nx+ k*NxNy];\n" \
"\n" \
"    fipjm = estimate[ ip1+ jm1*Nx+ k*NxNy];\n" \
"    fipkm = estimate[ ip1+ j*Nx+ km1*NxNy];\n" \
"    fip = estimate[ ip1+ j*Nx+ k*NxNy];\n" \
"\n" \
"    Dxpf = (fip - fijk) / hx;\n" \
"    Dxmf = (fijk - fim) / hx;\n" \
"    Dypf = (fjp - fijk) / hy;\n" \
"    Dymf = (fijk - fjm) / hy;\n" \
"    Dzpf = (fkp - fijk) / hz;\n" \
"    Dzmf = (fijk - fkm) / hz;\n" \
"    aijk = hypot3(Dxpf, m(Dypf, Dymf), m(Dzpf, Dzmf));\n" \
"    bijk = hypot3(Dypf, m(Dxpf, Dxmf), m(Dzpf, Dzmf));\n" \
"    cijk = hypot3(Dzpf, m(Dypf, Dymf), m(Dxpf, Dxmf));\n" \
"\n" \
"    aijk = (aijk > FLOAT32_EPS ? Dxpf / aijk : 0.0);\n" \
"    bijk = (bijk > FLOAT32_EPS ? Dypf / bijk : 0.0);\n" \
"    cijk = (cijk > FLOAT32_EPS ? Dzpf / cijk : 0.0);\n" \
"\n" \
"    Dxpf = (fijk - fim) / hx;\n" \
"    Dypf = (fimjp - fim) / hy;\n" \
"    Dymf = (fim - fimjm) / hy;\n" \
"    Dzpf = (fimkp - fim) / hz;\n" \
"    Dzmf = (fim - fimkm) / hz;\n" \
"    aim = hypot3(Dxpf, m(Dypf, Dymf), m(Dzpf, Dzmf));\n" \
"\n" \
"    aim = (aim > FLOAT32_EPS ? Dxpf / aim : 0.0);\n" \
"\n" \
"    Dxpf = (fipjm - fjm) / hx;\n" \
"    Dxmf = (fjm - fimjm) / hx;\n" \
"    Dypf = (fijk - fjm) / hy;\n" \
"    Dzmf = (fjm - fjmkm) / hz;\n" \
"    bjm = hypot3(Dypf, m(Dxpf, Dxmf), m(Dzpf, Dzmf));\n" \
"\n" \
"    bjm = (bjm > FLOAT32_EPS ? Dypf / bjm : 0.0);\n" \
"\n" \
"    Dxpf = (fipkm - fkm) / hx;\n" \
"    Dxmf = (fjm - fimkm) / hx;\n" \
"    Dypf = (fjpkm - fkm) / hy;\n" \
"    Dymf = (fkm - fjmkm) / hy;\n" \
"    Dzpf = (fijk - fkm) / hz;\n" \
"    ckm = hypot3(Dzpf, m(Dypf, Dymf), m(Dxpf, Dxmf));\n" \
"\n" \
"    ckm = (ckm > FLOAT32_EPS ? Dzpf / ckm : 0.0);\n" \
"\n" \
"    Dxma = (aijk - aim) / hx;\n" \
"    Dymb = (bijk - bjm) / hy;\n" \
"    Dzmc = (cijk - ckm) / hz;\n" \
"    \n" \
"    int index=i+j*Nx+k*NxNy;\n" \
"    variation[index]=correction[index]/(1.-regularizationFactor*(Dxma+Dymb+Dzmc));\n" \
"}                                \n" \
" \n" \

#endif //__cle_totalvariationterm_h
