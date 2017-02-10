/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package volvis;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import gui.RaycastRendererPanel;
import gui.TransferFunction2DEditor;
import gui.TransferFunctionEditor;
import java.awt.image.BufferedImage;
import util.TFChangeListener;
import util.VectorMath;
import volume.GradientVolume;
import volume.Volume;
import volume.VoxelGradient;

/**
 *
 * @author michel
 * @Anna
 * This class has the main code that generates the raycasting result image. 
 * The connection with the interface is already given.  
 * The different modes mipMode, slicerMode, etc. are already correctly updated
 */
public class RaycastRenderer extends Renderer implements TFChangeListener {

    private Volume volume = null;
    private GradientVolume gradients = null;
    RaycastRendererPanel panel;
    TransferFunction tFunc;
    TransferFunctionEditor tfEditor;
    TransferFunction2DEditor tfEditor2D;
    private boolean mipMode = false;
    private boolean slicerMode = true;
    private boolean compositingMode = false;
    private boolean tf2dMode = false;
    private boolean shadingMode = false;
    
    public RaycastRenderer() {
        panel = new RaycastRendererPanel(this);
        panel.setSpeedLabel("0");
    }

    public void setVolume(Volume vol) {
        System.out.println("Assigning volume");
        volume = vol;

        System.out.println("Computing gradients");
        gradients = new GradientVolume(vol);

        // set up image for storing the resulting rendering
        // the image width and height are equal to the length of the volume diagonal
        int imageSize = (int) Math.floor(Math.sqrt(vol.getDimX() * vol.getDimX() + vol.getDimY() * vol.getDimY()
                + vol.getDimZ() * vol.getDimZ()));
        if (imageSize % 2 != 0) {
            imageSize = imageSize + 1;
        }
        image = new BufferedImage(imageSize, imageSize, BufferedImage.TYPE_INT_ARGB);
        tFunc = new TransferFunction(volume.getMinimum(), volume.getMaximum());
        tFunc.setTestFunc();
        tFunc.addTFChangeListener(this);
        tfEditor = new TransferFunctionEditor(tFunc, volume.getHistogram());
        
        tfEditor2D = new TransferFunction2DEditor(volume, gradients);
        tfEditor2D.addTFChangeListener(this);

        System.out.println("Finished initialization of RaycastRenderer");
    }

    public RaycastRendererPanel getPanel() {
        return panel;
    }

    public TransferFunction2DEditor getTF2DPanel() {
        return tfEditor2D;
    }
    
    public TransferFunctionEditor getTFPanel() {
        return tfEditor;
    }
     
    public void setShadingMode(boolean mode) {
        shadingMode = mode;
        changed();
    }
    
    public void setMIPMode() {
        setMode(false, true, false, false);
    }
    
    public void setSlicerMode() {
        setMode(true, false, false, false);
    }
    
    public void setCompositingMode() {
        setMode(false, false, true, false);
    }
    
    public void setTF2DMode() {
        setMode(false, false, false, true);
    }
    
    private void setMode(boolean slicer, boolean mip, boolean composite, boolean tf2d) {
        slicerMode = slicer;
        mipMode = mip;
        compositingMode = composite;
        tf2dMode = tf2d;        
        changed();
    }
    
        
    private void drawBoundingBox(GL2 gl) {
        gl.glPushAttrib(GL2.GL_CURRENT_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glColor4d(1.0, 1.0, 1.0, 1.0);
        gl.glLineWidth(1.5f);
        gl.glEnable(GL.GL_LINE_SMOOTH);
        gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glBegin(GL.GL_LINE_LOOP);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glVertex3d(-volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, volume.getDimZ() / 2.0);
        gl.glVertex3d(volume.getDimX() / 2.0, -volume.getDimY() / 2.0, -volume.getDimZ() / 2.0);
        gl.glEnd();

        gl.glDisable(GL.GL_LINE_SMOOTH);
        gl.glDisable(GL.GL_BLEND);
        gl.glEnable(GL2.GL_LIGHTING);
        gl.glPopAttrib();

    }
    
    private boolean intersectLinePlane(double[] plane_pos, double[] plane_normal,
            double[] line_pos, double[] line_dir, double[] intersection) {

        double[] tmp = new double[3];

        for (int i = 0; i < 3; i++) {
            tmp[i] = plane_pos[i] - line_pos[i];
        }

        double denom = VectorMath.dotproduct(line_dir, plane_normal);
        if (Math.abs(denom) < 1.0e-8) {
            return false;
        }

        double t = VectorMath.dotproduct(tmp, plane_normal) / denom;

        for (int i = 0; i < 3; i++) {
            intersection[i] = line_pos[i] + t * line_dir[i];
        }

        return true;
    }

    private boolean validIntersection(double[] intersection, double xb, double xe, double yb,
            double ye, double zb, double ze) {

        return (((xb - 0.5) <= intersection[0]) && (intersection[0] <= (xe + 0.5))
                && ((yb - 0.5) <= intersection[1]) && (intersection[1] <= (ye + 0.5))
                && ((zb - 0.5) <= intersection[2]) && (intersection[2] <= (ze + 0.5)));

    }

    private void intersectFace(double[] plane_pos, double[] plane_normal,
            double[] line_pos, double[] line_dir, double[] intersection,
            double[] entryPoint, double[] exitPoint) {

        boolean intersect = intersectLinePlane(plane_pos, plane_normal, line_pos, line_dir,
                intersection);
        if (intersect) {

            //System.out.println("Plane pos: " + plane_pos[0] + " " + plane_pos[1] + " " + plane_pos[2]);
            //System.out.println("Intersection: " + intersection[0] + " " + intersection[1] + " " + intersection[2]);
            //System.out.println("line_dir * intersection: " + VectorMath.dotproduct(line_dir, plane_normal));

            double xpos0 = 0;
            double xpos1 = volume.getDimX();
            double ypos0 = 0;
            double ypos1 = volume.getDimY();
            double zpos0 = 0;
            double zpos1 = volume.getDimZ();

            if (validIntersection(intersection, xpos0, xpos1, ypos0, ypos1,
                    zpos0, zpos1)) {
                if (VectorMath.dotproduct(line_dir, plane_normal) > 0) {
                    entryPoint[0] = intersection[0];
                    entryPoint[1] = intersection[1];
                    entryPoint[2] = intersection[2];
                } else {
                    exitPoint[0] = intersection[0];
                    exitPoint[1] = intersection[1];
                    exitPoint[2] = intersection[2];
                }
            }
        }
    }
    

      int traceRayMIP(double[] entryPoint, double[] exitPoint, double[] viewVec, double sampleStep) {
       
          /* to be implemented:  You need to sample the ray and implement the MIP
         * right now it just returns yellow as a color
        */
        double max=-10000.0;
        double length=Math.sqrt(Math.pow((entryPoint[0]-exitPoint[0]),2) + Math.pow((entryPoint[1]-exitPoint[1]),2)+Math.pow((entryPoint[2]-exitPoint[2]),2));
        double nbrSteps=length/sampleStep;
        for (int i=0;i<nbrSteps&&max<255;i++)
        {
            double[] pixelCoord = new double[3];
            pixelCoord[0]=entryPoint[0]-i*sampleStep*viewVec[0];
            pixelCoord[1]=entryPoint[1]-i*sampleStep*viewVec[1];
            pixelCoord[2]=entryPoint[2]-i*sampleStep*viewVec[2];
            int val = volume.getVoxelInterpolate(pixelCoord);
            if(val>max)
            {
                max=val;
            }
            
        }
        int max2=(int)max;
        int fill=255;
        if (max==0)
        {
            fill=0;
        }
        
        int color= (fill<< 24) | (max2<<16) | (max2<< 8) | (max2) ;
        return color;
    }
   
    
   
    void computeEntryAndExit(double[] p, double[] viewVec, double[] entryPoint, double[] exitPoint) {

        for (int i = 0; i < 3; i++) {
            entryPoint[i] = -1;
            exitPoint[i] = -1;
        }

        double[] plane_pos = new double[3];
        double[] plane_normal = new double[3];
        double[] intersection = new double[3];

        VectorMath.setVector(plane_pos, volume.getDimX(), 0, 0);
        VectorMath.setVector(plane_normal, 1, 0, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, -1, 0, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, volume.getDimY(), 0);
        VectorMath.setVector(plane_normal, 0, 1, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, 0, -1, 0);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, volume.getDimZ());
        VectorMath.setVector(plane_normal, 0, 0, 1);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

        VectorMath.setVector(plane_pos, 0, 0, 0);
        VectorMath.setVector(plane_normal, 0, 0, -1);
        intersectFace(plane_pos, plane_normal, p, viewVec, intersection, entryPoint, exitPoint);

    }

    void raycast(double[] viewMatrix) {
        /* To be partially implemented:
            This function traces the rays through the volume. Have a look and check that you understand how it works.
            You need to introduce here the different modalities MIP/Compositing/TF2/ etc...*/

        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);


        int imageCenter = image.getWidth() / 2;

        double[] pixelCoord = new double[3];
        double[] entryPoint = new double[3];
        double[] exitPoint = new double[3];
        
        int increment=1;
        float sampleStep=0.2f;//0.2f;
        


        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                image.setRGB(i, j, 0);
            }
        }


        for (int j = 0; j < image.getHeight(); j += increment) {
            for (int i = 0; i < image.getWidth(); i += increment) {
                // compute starting points of rays in a plane shifted backwards to a position behind the data set
                pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter) - viewVec[0] * imageCenter
                        + volume.getDimX() / 2.0;
                pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter) - viewVec[1] * imageCenter
                        + volume.getDimY() / 2.0;
                pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter) - viewVec[2] * imageCenter
                        + volume.getDimZ() / 2.0;

                computeEntryAndExit(pixelCoord, viewVec, entryPoint, exitPoint);
                if ((entryPoint[0] > -1.0) && (exitPoint[0] > -1.0)) {
                    //System.out.println("Entry: " + entryPoint[0] + " " + entryPoint[1] + " " + entryPoint[2]);
                    //System.out.println("Exit: " + exitPoint[0] + " " + exitPoint[1] + " " + exitPoint[2]);
                    int pixelColor = 0;
                                   
                    /* set color to green if MipMode- see slicer function*/
                   if(mipMode) 
                        pixelColor= traceRayMIP(entryPoint,exitPoint,viewVec,sampleStep);
                         
                   if(compositingMode||tf2dMode) 
                        pixelColor= Compose(entryPoint,exitPoint,viewVec,sampleStep);
                   
                   
                    for (int ii = i; ii < i + increment; ii++) {
                        for (int jj = j; jj < j + increment; jj++) {
                            image.setRGB(ii, jj, pixelColor);
                        }
                    }
                }

            }
            System.out.print((int)(100*(double)j/(double)image.getHeight()));
            System.out.println(" %");
        }


    }
    TFColor Phong(double[] viewVec,TFColor color,double[] grad,float mag)
    {
        if(shadingMode){
        TFColor colorPhong= new TFColor();
        double ka,kd,ks,alpha,cosTheta,cosBeta,cosPhi,phi,theta,beta;
        double[] lightSource={1,0,0};
        /*System.out.print(grad[0]);
        System.out.print(',');
        System.out.print(grad[1]);
        System.out.print(',');
        System.out.print(grad[2]);
        System.out.print('+');
        System.out.println(mag);
        System.out.print(lightSource[0]);
        System.out.print(',');
        System.out.print(lightSource[1]);
        System.out.print(',');
        System.out.print(lightSource[2]);
         System.out.print('+');
        System.out.println(mag*Math.sqrt(Math.pow(lightSource[0],2)+ Math.pow(lightSource[1],2)+Math.pow(lightSource[2],2)));
        */if(mag==0)
        {cosTheta=0;
        cosBeta=0;
        cosPhi=0;}
        else{
            cosTheta=VectorMath.dotproduct(grad,lightSource)/(mag*VectorMath.length(lightSource));
            
       // cosTheta= (grad[0]*lightSource[0]+grad[1]*lightSource[1]+grad[2]*lightSource[2])/(mag*Math.sqrt(Math.pow(lightSource[0],2)+ Math.pow(lightSource[1],2)+Math.pow(lightSource[2],2))) ;
        /*cosBeta= (grad[0]*viewVec[0]+grad[1]*viewVec[1]+grad[2]*viewVec[2])/(mag*Math.sqrt(Math.pow(viewVec[0],2)+ Math.pow(viewVec[1],2)+Math.pow(viewVec[2],2))) ;
           beta=Math.acos(cosBeta);
        phi=beta-Math.acos(cosTheta);
        */
        theta=Math.acos(cosTheta);
        cosBeta=VectorMath.dotproduct(viewVec,lightSource)/(VectorMath.length(viewVec)*VectorMath.length(lightSource));
            
        //cosBeta= (lightSource[0]*viewVec[0]+lightSource[1]*viewVec[1]+lightSource[2]*viewVec[2])/(mag*Math.sqrt(Math.pow(viewVec[0],2)+ Math.pow(viewVec[1],2)+Math.pow(viewVec[2],2))) ;
        beta=Math.acos(cosBeta);
        phi=beta-2*theta;
        cosPhi=Math.cos(phi);}
        if(cosTheta*cosTheta>1)
        {System.out.print(cosTheta);
        System.out.print(' ');
        System.out.println(cosPhi);
        System.out.println(' ');
        System.out.println(' ');}
        ka=0.1;
        kd=0.7;
        ks=0.2;
        alpha=10;
        /*if(cosPhi>0.9)
        {
            System.out.print(ka*color.r);
            System.out.print(' ');
             System.out.print(cosTheta);
            System.out.print(' ');
            System.out.print(kd*color.r*cosTheta);
            System.out.print(' ');
            System.out.println(ks*Math.pow(cosPhi,alpha));
        }*/
        colorPhong.r=ka*color.r+kd*color.r*cosTheta+ks*Math.pow(cosPhi,alpha);
        colorPhong.g=ka*color.g+kd*color.g*cosTheta+ks*Math.pow(cosPhi,alpha);
        colorPhong.b=ka*color.b+kd*color.b*cosTheta+ks*Math.pow(cosPhi,alpha);
        colorPhong.a=color.a;
        return colorPhong;
        
        }
        else
        {return color;}
    }
    int Compose(double[] entryPoint, double[] exitPoint, double[] viewVec, double sampleStep) {
        double length=Math.sqrt(Math.pow((entryPoint[0]-exitPoint[0]),2) + Math.pow((entryPoint[1]-exitPoint[1]),2)+Math.pow((entryPoint[2]-exitPoint[2]),2));
        double nbrSteps=length/sampleStep;
        TFColor prevColor = new TFColor(); 
        int entryVal=volume.getVoxelInterpolate(entryPoint);
        prevColor.r=0;
        prevColor.g=0;
        prevColor.b=0;
        prevColor.a=0;
        
        for (int i=0;i<nbrSteps && prevColor.a < 0.999;i++ )
        {
            double[] pixelCoord = new double[3];
            pixelCoord[0]=entryPoint[0]-i*sampleStep*viewVec[0];
            pixelCoord[1]=entryPoint[1]-i*sampleStep*viewVec[1];
            pixelCoord[2]=entryPoint[2]-i*sampleStep*viewVec[2];
            int val = volume.getVoxelInterpolate(pixelCoord);
             TFColor newColor = new TFColor();
            TFColor voxelColor = new TFColor();
            voxelColor = tFunc.getColor(val);
            if (shadingMode || tf2dMode){
                VoxelGradient grad = new VoxelGradient();
                grad = gradients.getGradient(pixelCoord);
            
                
                if(tf2dMode){
                    TFColor tf2Color=tfEditor2D.triangleWidget.color;
                    double rad=tfEditor2D.triangleWidget.radius;
                    double fv=tfEditor2D.triangleWidget.baseIntensity;
                    voxelColor.r=tf2Color.r;
                    voxelColor.g=tf2Color.g;
                    voxelColor.b=tf2Color.b;
                    if(val==fv && grad.mag==0){
                        voxelColor.a=1;
                    }
                    else if(grad.mag>0 && fv >= val-rad*grad.mag && fv <= val+rad*grad.mag){
                
                        voxelColor.a=1-1/rad*Math.abs((fv-val)/(grad.mag));
                
                    }
                    else{
                        voxelColor.a=0;
                    } 
                }
                if(shadingMode){
                    double posX,posY,posZ;
                    posX=grad.x;
                    posY=grad.y;
                    posZ=grad.z;
                    double[] gradD={posX,posY,posZ};
                    voxelColor=Phong(viewVec,voxelColor,gradD,grad.mag);
                }
                
            }
          
            double alpha= (1-Math.pow(1-voxelColor.a, sampleStep));
            newColor.r=prevColor.r+(1-prevColor.a)*voxelColor.r*alpha;
            newColor.g=prevColor.g+(1-prevColor.a)*voxelColor.g*alpha;
            newColor.b=prevColor.b+(1-prevColor.a)*voxelColor.b*alpha;
            newColor.a=prevColor.a+(1-prevColor.a)*alpha;
           
            if (newColor.a>1)
            { 
                newColor.a=1;
            }
            prevColor=newColor;
            
        }
        
        int r= (int) (prevColor.r *255);
        if (r>255) r=255;
        int g= (int) (prevColor.g *255);
        if (g>255) g=255;
        int b= (int) (prevColor.b *255);
        if (b>255) b=255;
        int a= (int) (prevColor.a *255);
        if (a>255) a=255;
       /* System.out.print(r+",");
        System.out.print(g+",");
        System.out.print(b+",");
        System.out.println(a);*/
        int color= (a<< 24) | (r<<16) | (g<< 8) | (b) ;
        return color;
    
    }
    void slicer(double[] viewMatrix) {

        // clear image
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                image.setRGB(i, j, 0);
            }
        }

        // vector uVec and vVec define a plane through the origin, 
        // perpendicular to the view vector viewVec
        double[] viewVec = new double[3];
        double[] uVec = new double[3];
        double[] vVec = new double[3];
        VectorMath.setVector(viewVec, viewMatrix[2], viewMatrix[6], viewMatrix[10]);
        VectorMath.setVector(uVec, viewMatrix[0], viewMatrix[4], viewMatrix[8]);
        VectorMath.setVector(vVec, viewMatrix[1], viewMatrix[5], viewMatrix[9]);

        // image is square
        int imageCenter = image.getWidth() / 2;

        double[] pixelCoord = new double[3];
        double[] volumeCenter = new double[3];
        VectorMath.setVector(volumeCenter, volume.getDimX() / 2, volume.getDimY() / 2, volume.getDimZ() / 2);

        // sample on a plane through the origin of the volume data
        double max = volume.getMaximum();
        TFColor voxelColor = new TFColor();
        for (int j = 0; j < image.getHeight(); j++) {
            for (int i = 0; i < image.getWidth(); i++) {
                pixelCoord[0] = uVec[0] * (i - imageCenter) + vVec[0] * (j - imageCenter)
                        + volumeCenter[0];
                pixelCoord[1] = uVec[1] * (i - imageCenter) + vVec[1] * (j - imageCenter)
                        + volumeCenter[1];
                pixelCoord[2] = uVec[2] * (i - imageCenter) + vVec[2] * (j - imageCenter)
                        + volumeCenter[2];

                int val = volume.getVoxelInterpolate(pixelCoord);
                // Map the intensity to a grey value by linear scaling
               // voxelColor.r = val/max;
               // voxelColor.g = voxelColor.r;
                //voxelColor.b = voxelColor.r;
                //voxelColor.a = val > 0 ? 1.0 : 0.0;  // this makes intensity 0 completely transparent and the rest opaque
                
                // Alternatively, apply the transfer function to obtain a color
                TFColor auxColor = new TFColor(); 
                auxColor = tFunc.getColor(val);
                voxelColor.r=auxColor.r;
                voxelColor.g=auxColor.g;
                voxelColor.b=auxColor.b;
                voxelColor.a=auxColor.a;
                 
                
                // BufferedImage expects a pixel color packed as ARGB in an int
                int c_alpha = voxelColor.a <= 1.0 ? (int) Math.floor(voxelColor.a * 255) : 255;
                int c_red = voxelColor.r <= 1.0 ? (int) Math.floor(voxelColor.r * 255) : 255;
                int c_green = voxelColor.g <= 1.0 ? (int) Math.floor(voxelColor.g * 255) : 255;
                int c_blue = voxelColor.b <= 1.0 ? (int) Math.floor(voxelColor.b * 255) : 255;
                int pixelColor = (c_alpha << 24) | (c_red << 16) | (c_green << 8) | c_blue;
                image.setRGB(i, j, pixelColor);
            }
        }


    }


    @Override
    public void visualize(GL2 gl) {


        if (volume == null) {
            return;
        }

        drawBoundingBox(gl);

        gl.glGetDoublev(GL2.GL_MODELVIEW_MATRIX, viewMatrix, 0);

        long startTime = System.currentTimeMillis();
        if (slicerMode) {
            slicer(viewMatrix);    
        } else {
            raycast(viewMatrix);
        }
        
        long endTime = System.currentTimeMillis();
        double runningTime = (endTime - startTime);
        panel.setSpeedLabel(Double.toString(runningTime));

        Texture texture = AWTTextureIO.newTexture(gl.getGLProfile(), image, false);

        gl.glPushAttrib(GL2.GL_LIGHTING_BIT);
        gl.glDisable(GL2.GL_LIGHTING);
        gl.glEnable(GL.GL_BLEND);
        gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);

        // draw rendered image as a billboard texture
        texture.enable(gl);
        texture.bind(gl);
        double halfWidth = image.getWidth() / 2.0;
        gl.glPushMatrix();
        gl.glLoadIdentity();
        gl.glBegin(GL2.GL_QUADS);
        gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        gl.glTexCoord2d(0.0, 0.0);
        gl.glVertex3d(-halfWidth, -halfWidth, 0.0);
        gl.glTexCoord2d(0.0, 1.0);
        gl.glVertex3d(-halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 1.0);
        gl.glVertex3d(halfWidth, halfWidth, 0.0);
        gl.glTexCoord2d(1.0, 0.0);
        gl.glVertex3d(halfWidth, -halfWidth, 0.0);
        gl.glEnd();
        texture.disable(gl);
        texture.destroy(gl);
        gl.glPopMatrix();

        gl.glPopAttrib();


        if (gl.glGetError() > 0) {
            System.out.println("some OpenGL error: " + gl.glGetError());
        }

    }
    private BufferedImage image;
    private double[] viewMatrix = new double[4 * 4];

    @Override
    public void changed() {
        for (int i=0; i < listeners.size(); i++) {
            listeners.get(i).changed();
        }
    }
}
