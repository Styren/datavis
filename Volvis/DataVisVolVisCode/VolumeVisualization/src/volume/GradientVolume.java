/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

/**
 *
 * @author michel
 * @ Anna
 * This class contains the pre-computes gradients of the volume. This means calculates the gradient
 * at all voxel positions, and provides functions
 * to get the gradient at any position in the volume also continuous..
*/
public class GradientVolume {

    public GradientVolume(Volume vol) {
        volume = vol;
        dimX = vol.getDimX();
        dimY = vol.getDimY();
        dimZ = vol.getDimZ();
        data = new VoxelGradient[dimX * dimY * dimZ];
        compute();
        maxmag = -1.0;
    }

    public VoxelGradient getGradient(int x, int y, int z) {
        return data[x + dimX * (y + dimY * z)];
    }

    private void interpolate(VoxelGradient g0, VoxelGradient g1, float factor, VoxelGradient result) {
        /* To be implemented: this function linearly interpolates gradient vector g0 and g1 given the factor (t) 
            the resut is given at result. You can use it to tri-linearly interpolate the gradient */
        /*Simple interpolation*/
        result.x=g0.x*(1-factor)+g0.x*factor;
        result.y=g0.y*(1-factor)+g0.y*factor;
        result.z=g0.z*(1-factor)+g0.z*factor;
        result.mag = (float) Math.sqrt(result.x*result.x + result.y*result.y + result.z*result.z);
    }
    
    public VoxelGradient getGradientNN(double[] coord) {
        /* Nearest neighbour interpolation applied to provide the gradient */
        if (coord[0] < 0 || coord[0] > (dimX-2) || coord[1] < 0 || coord[1] > (dimY-2)
                || coord[2] < 0 || coord[2] > (dimZ-2)) {
            return zero;
        }

        int x = (int) Math.round(coord[0]);
        int y = (int) Math.round(coord[1]);
        int z = (int) Math.round(coord[2]);
        
        return getGradient(x, y, z);
    }

    
    public VoxelGradient getGradient(double[] coord) {
    /* To be implemented: Returns trilinear interpolated gradient based on the precomputed gradients. 
     *   Use function interpolate. Use getGradientNN as bases */
        if (coord[0] < 0 || coord[0] > (dimX-2) || coord[1] < 0 || coord[1] > (dimY-2)
                || coord[2] < 0 || coord[2] > (dimZ-2)) {
            return zero;
        }
        // get coordinates for the 8 points around the coordinate
        int xF =(int) Math.floor(coord[0]);
        int xC = (int) Math.ceil(coord[0]);
        int yF =(int) Math.floor(coord[1]);
        int yC = (int) Math.ceil(coord[1]);
        int zF =(int) Math.floor(coord[2]);
        int zC = (int) Math.ceil(coord[2]);
        //Copute factors
        float factor_X=(float)((coord[0]-xF)/(xC-xF));
        float factor_Y=(float)((coord[0]-yF)/(yC-yF));
        float factor_Z=(float)((coord[0]-zF)/(zC-zF));
        if (xF==xC)
        {
            factor_X=0;
        }
         if (yF==yC)
        {
            factor_Y=0;
        }
        
          if (zF==zC)
        {
            factor_Z=0;
        }
        
        // set the 8 points of the cube
        VoxelGradient grad_fff=getGradient(xF,yF,zF);
        VoxelGradient grad_ffc=getGradient(xF,yF,zC);
        VoxelGradient grad_fcf=getGradient(xF,yC,zF);
        VoxelGradient grad_fcc=getGradient(xF,yC,zC);
        VoxelGradient grad_cff=getGradient(xC,yF,zF);
        VoxelGradient grad_cfc=getGradient(xC,yF,zC);
        VoxelGradient grad_ccf=getGradient(xC,yC,zF);
        VoxelGradient grad_ccc=getGradient(xC,yC,zC);
        
        VoxelGradient x_ff =new VoxelGradient();
        VoxelGradient x_fc =new VoxelGradient();
        VoxelGradient x_cf =new VoxelGradient();
        VoxelGradient x_cc =new VoxelGradient();
        VoxelGradient y_f =new VoxelGradient();
        VoxelGradient y_c =new VoxelGradient();
        VoxelGradient complete =new VoxelGradient();
        
        //interpolate following x
        interpolate(grad_fff,grad_cff,factor_X,x_ff);
        interpolate(grad_ffc,grad_cfc,factor_X,x_fc);
        interpolate(grad_fcf,grad_ccf,factor_X,x_cf);
        interpolate(grad_fcc,grad_ccc,factor_X,x_cc);
        
         //interpolate following y
        interpolate(x_ff,x_cf,factor_Y,y_f);
        interpolate(x_fc,x_cc,factor_Y,y_c);
        
         //interpolate following z
        interpolate(y_f,y_c,factor_Z,complete);
        return complete;
        
        
    }
    
  
    public void setGradient(int x, int y, int z, VoxelGradient value) {
        data[x + dimX * (y + dimY * z)] = value;
    }

    public void setVoxel(int i, VoxelGradient value) {
        data[i] = value;
    }

    public VoxelGradient getVoxel(int i) {
        return data[i];
    }

    public int getDimX() {
        return dimX;
    }

    public int getDimY() {
        return dimY;
    }

    public int getDimZ() {
        return dimZ;
    }

    private void compute() {
        /* To be implemented: compute the gradient of contained in the volume attribute */
        for(int x=0;x<dimX;x++){
            for(int y=0;y<dimY;y++){
                for(int z=0;z<dimZ;z++){
                   // get voxels before and after current one
                   int xPlus=x+1;
                   int xMinus=x-1;
                   // set distance between these voxels
                   int deltaX=2;
                   
                   /*if voxels on the boundary estimate that the gradient to be equal to
                   the the gradient between the current point and the the other slected voxel*/
                   if(xPlus==dimX)
                   {xPlus=x;
                   deltaX=1;}
                   
                   if(xMinus==-1)
                   {xMinus=0;
                   deltaX=1;}
                   
                   //repeat for y and z
                   int yPlus=y+1;
                   int yMinus=y-1;
                   int deltaY=2;
                   if(yPlus==dimY)
                   {yPlus=y;
                   deltaY=1;}
                   
                   if(yMinus==-1)
                   {yMinus=0;
                   deltaY=1;}
                   
                   int zPlus=z+1;
                   int zMinus=z-1;
                   int deltaZ=2;
                   if(zPlus==dimZ)
                   {zPlus=z;
                   deltaZ=1;}
                   
                   if(zMinus==-1)
                   {zMinus=0;
                   deltaZ=1;}
                   
                   short x0,x1,y0,y1,z0,z1;
                   
                   //get values at each voxel
                   x0=volume.getVoxel(xMinus, y, z);
                   x1=volume.getVoxel(xPlus, y, z);
                   y0=volume.getVoxel(x, yMinus, z);
                   y1=volume.getVoxel(x, yPlus, z);
                   z0=volume.getVoxel(x, y, zMinus);
                   z1=volume.getVoxel(x, y, zPlus);
                   
                   VoxelGradient grad =new VoxelGradient((x1-x0)/deltaX,(y1-y0)/deltaY,(z1-z0)/deltaZ);
                   setGradient(x, y,z, grad);
                }
            }
            
        }
           
        
    }
    
    public double getMaxGradientMagnitude() {
        /* Simple  function to get the maximum gradient magnitude*/
        float max=-1;
        for(int i=0;i<data.length;i++)
        {
            float mag=data[i].mag;
            if (mag>max){
            max=mag;
            }
        }
        return max;
    }
    
    private int dimX, dimY, dimZ;
    private VoxelGradient zero = new VoxelGradient();
    VoxelGradient[] data;
    Volume volume;
    double maxmag;
}
