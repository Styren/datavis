/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volume;

import java.io.File;
import java.io.IOException;

/**
 *
 * @author michel
 * @Anna 
 * Volume object: This class contains the object and assumes that the distance between the voxels in x,y and z are 1 
 */
public class Volume {
    
    public Volume(int xd, int yd, int zd) {
        data = new short[xd*yd*zd];
        dimX = xd;
        dimY = yd;
        dimZ = zd;
    }
    
    public Volume(File file) {
        
        try {
            VolumeIO reader = new VolumeIO(file);
            dimX = reader.getXDim();
            dimY = reader.getYDim();
            dimZ = reader.getZDim();
            data = reader.getData().clone();
            computeHistogram();
        } catch (IOException ex) {
            System.out.println("IO exception");
        }
        
    }
    
    
    public short getVoxel(int x, int y, int z) {
        return data[x + dimX*(y + dimY * z)];
    }
    
    public void setVoxel(int x, int y, int z, short value) {
        data[x + dimX*(y + dimY*z)] = value;
    }

    public void setVoxel(int i, short value) {
        data[i] = value;
    }
    
    public short getVoxelInterpolate(double[] coord) {
    /* to be implemented: get the trilinear interpolated value. 
        The current implementation gets the Nearest Neightbour */
        
        if (coord[0] < 0 || coord[0] > (dimX-1) || coord[1] < 0 || coord[1] > (dimY-1)
                || coord[2] < 0 || coord[2] > (dimZ-1)) {
            return 0;
        }
        
       
        int xFloor =(int) Math.floor(coord[0]);
        int xCeil = (int) Math.ceil(coord[0]);
        int yFloor =(int) Math.floor(coord[1]);
        int yCeil = (int) Math.ceil(coord[1]);
        int zFloor =(int) Math.floor(coord[2]);
        int zCeil = (int) Math.ceil(coord[2]);
        /* set coordinates of points ordred to intepolate following z axis first then y axis then x axis*/
        int[] pointsX={xFloor, xFloor, xFloor,xFloor,xCeil ,xCeil,xCeil  ,xCeil };
        int[] pointsY={yFloor, yFloor, yCeil ,yCeil ,yFloor ,yFloor,yCeil ,yCeil};
        int[] pointsZ={zFloor, zCeil , zFloor,zCeil ,zFloor,zCeil,zFloor,zCeil };
        double[] interPolZ=new double[4];
        double[] interPolY=new double[2];
        //sart to interpolate folowing Z axis 
        // /!\ we interpolate between points[0] and points[1] then between points[2] and points[3] etc...
        for (int i=0;i<4;i++)
        {
            // is we are exactly on a known voxel we just get its value
            if (zCeil==zFloor){
                interPolZ[i]=getVoxel(pointsX[2*i],pointsY[2*i],pointsZ[2*i]);
            }
            else{
                // simple interpolation following z axis
                interPolZ[i] = (zCeil-coord[2])*getVoxel(pointsX[2*i],pointsY[2*i],pointsZ[2*i]) + (coord[2]-zFloor)*getVoxel(pointsX[2*i+1],pointsY[2*i+1],pointsZ[2*i+1]) ;
            }
        }
        for (int i=0;i<2;i++)
        {
            if(yCeil==yFloor)
            {
                interPolY[i]=interPolZ[2*i];
            }
            else{
                // simple interpolation following y axis
                interPolY[i] =(yCeil-coord[1])*interPolZ[2*i]+(coord[1]-yFloor)*interPolZ[2*i+1];
            }   
        }
        double interPolX;
         
         if(xCeil==xFloor)
            {
                interPolX=interPolY[0];
            }
         else{
             // simple interpolation following x axis
                interPolX=(xCeil-coord[0])*interPolY[0]+(coord[0]-xFloor)*interPolY[1];
            }
        
        return (short)interPolX;
    }
    
    public short getVoxel(int i) {
        return data[i];
    }
     public void setVoxel(short[] values) {
        data = values;
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

    public short getMinimum() {
        short minimum = data[0];
        for (int i=0; i<data.length; i++) {
            minimum = data[i] < minimum ? data[i] : minimum;
        }
        return minimum;
    }

    public short getMaximum() {
        short maximum = data[0];
        for (int i=0; i<data.length; i++) {
            maximum = data[i] > maximum ? data[i] : maximum;
        }
        return maximum;
    }
 
    public int[] getHistogram() {
        return histogram;
    }
    
    private void computeHistogram() {
        histogram = new int[getMaximum() + 1];
        for (int i=0; i<data.length; i++) {
            histogram[data[i]]++;
        }
    }
    
    private int dimX, dimY, dimZ;
    private short[] data;
    private int[] histogram;
}
