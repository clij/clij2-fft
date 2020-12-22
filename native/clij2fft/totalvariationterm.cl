
float hypot3(float a, float b, float c) {
    return sqrt(a*a+b*b+c*c);
}

double m(double a, double b) {
    if (a < 0 && b < 0) {
        if (a >= b) return a;
        return b;
    }
    if (a > 0 && b > 0) {
        if (a < b) return a;
        return b;
    }
    return 0.0;
}
/**
 * This kernel implements the correction term for Richardson-Lucy algorithm with total variation
 * regularization for 3D confocal microscope deconvolution Microsc Res Rech 2006
 * Apr; 69(4)- 260-6
 *
 * estimate - current estimate of RL algorithm
 * correction - current correction of RL algorithm
 * variation - the correction will be modified with the total variation constraint and written to 'variation'
 *
 **/
#pragma OPENCL EXTENSION cl_khr_fp64 : enable 
__kernel void totalVariationTerm(  __global float *estimate, global float * correction, __global float * variation,
                    const unsigned int Nx, const unsigned int Ny, const unsigned int Nz, float hx, float hy, float hz, float regularizationFactor)  
{                                             
    
    int NxNy=Nx*Ny;
    
    double FLOAT32_EPS = 0.0;
    
    //Get 3D global thread IDs               
    int i = get_global_id(0);              
    int j = get_global_id(1);              
    int k = get_global_id(2);     

    int im1, ip1, jm1, jp1, km1, kp1;

    float fip, fim, fjp, fjm, fkp, fkm, fijk;
    float fimkm, fipkm, fjmkm, fjpkm, fimjm, fipjm, fimkp, fimjp;
    float aim, bjm, ckm, aijk, bijk, cijk;
    float Dxpf, Dxmf, Dypf, Dymf, Dzpf, Dzmf;
    float Dxma, Dymb, Dzmc;
 
    //Make sure we do not go out of bounds
    i = (i>Nx-1 ? Nx-1 : i);
    j = (j>Ny-1 ? Ny-1 : j);
    k = (k>Nz-1 ? Nz-1 : k);
    
    im1 = (i > 0 ? i - 1 : 0);
	ip1 = (i + 1 == Nx ? i : i + 1);

    jm1 = (j > 0 ? j - 1 : 0);
	jp1 = (j + 1 == Ny ? j : j + 1);
    
    km1 = (k > 0 ? k - 1 : 0);
	kp1 = (k + 1 == Nz ? k : k + 1);
    
    fimjm = estimate[ im1 + jm1*Nx + k*NxNy];
    fim = estimate[ im1+ j*Nx+ k*NxNy];
    fimkm =  estimate[im1+ j*Nx+ km1*NxNy];
    fimkp = estimate[ im1+ j*Nx+ kp1*NxNy];
    fimjp = estimate[ im1+ jp1*Nx+ k*NxNy];

    fjmkm = estimate[ i+ jm1*Nx+ km1*NxNy];
    fjm = estimate[ i+ jm1*Nx+ k*NxNy];
    // fjmkp = estimate[ i+ jm1*Nx+ kp1*NxNy];

    fkm = estimate[ i+ j*Nx+ km1*NxNy];
    fijk = estimate[ i+ j*Nx+ k*NxNy];
    fkp = estimate[ i+ j*Nx+ kp1*NxNy];

    fjpkm = estimate[ i+ jp1*Nx+ km1*NxNy];
    fjp = estimate[ i+ jp1*Nx+ k*NxNy];

    fipjm = estimate[ ip1+ jm1*Nx+ k*NxNy];
    fipkm = estimate[ ip1+ j*Nx+ km1*NxNy];
    fip = estimate[ ip1+ j*Nx+ k*NxNy];

    Dxpf = (fip - fijk) / hx;
    Dxmf = (fijk - fim) / hx;
    Dypf = (fjp - fijk) / hy;
    Dymf = (fijk - fjm) / hy;
    Dzpf = (fkp - fijk) / hz;
    Dzmf = (fijk - fkm) / hz;
    aijk = hypot3(Dxpf, m(Dypf, Dymf), m(Dzpf, Dzmf));
    bijk = hypot3(Dypf, m(Dxpf, Dxmf), m(Dzpf, Dzmf));
    cijk = hypot3(Dzpf, m(Dypf, Dymf), m(Dxpf, Dxmf));

    aijk = (aijk > FLOAT32_EPS ? Dxpf / aijk : 0.0);
    bijk = (bijk > FLOAT32_EPS ? Dypf / bijk : 0.0);
    cijk = (cijk > FLOAT32_EPS ? Dzpf / cijk : 0.0);

    Dxpf = (fijk - fim) / hx;
    Dypf = (fimjp - fim) / hy;
    Dymf = (fim - fimjm) / hy;
    Dzpf = (fimkp - fim) / hz;
    Dzmf = (fim - fimkm) / hz;
    aim = hypot3(Dxpf, m(Dypf, Dymf), m(Dzpf, Dzmf));

    aim = (aim > FLOAT32_EPS ? Dxpf / aim : 0.0);

    Dxpf = (fipjm - fjm) / hx;
    Dxmf = (fjm - fimjm) / hx;
    Dypf = (fijk - fjm) / hy;
    Dzmf = (fjm - fjmkm) / hz;
    bjm = hypot3(Dypf, m(Dxpf, Dxmf), m(Dzpf, Dzmf));

    bjm = (bjm > FLOAT32_EPS ? Dypf / bjm : 0.0);

    Dxpf = (fipkm - fkm) / hx;
    Dxmf = (fjm - fimkm) / hx;
    Dypf = (fjpkm - fkm) / hy;
    Dymf = (fkm - fjmkm) / hy;
    Dzpf = (fijk - fkm) / hz;
    ckm = hypot3(Dzpf, m(Dypf, Dymf), m(Dxpf, Dxmf));

    ckm = (ckm > FLOAT32_EPS ? Dzpf / ckm : 0.0);

    Dxma = (aijk - aim) / hx;
    Dymb = (bijk - bjm) / hy;
    Dzmc = (cijk - ckm) / hz;
    
    int index=i+j*Nx+k*NxNy;
    variation[index]=correction[index]/(1.-regularizationFactor*(Dxma+Dymb+Dzmc));
}                                
 