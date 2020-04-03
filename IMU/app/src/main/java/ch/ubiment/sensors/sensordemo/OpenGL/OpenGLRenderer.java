package ch.ubiment.sensors.sensordemo.OpenGL;

import android.opengl.GLSurfaceView;
import android.opengl.GLU;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by loic on 23.05.17.
 *
 * This class define some OpenGL stuff for rendering only. It will render what's in Scene.java
 */

public class OpenGLRenderer implements GLSurfaceView.Renderer {
    private Scene scene;

    private float[] rotationMatrix_Gyro = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};
    private float[] rotationMatrix_Magn = {1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1};

    public float[] getRotationMatrix_Gyro() {
        return rotationMatrix_Gyro;
    }

    public float[] getRotationMatrix_Magn() {
        return rotationMatrix_Magn;
    }

    public void setRotationMatrix_Gyro(float[] rotationMatrix) {
        this.rotationMatrix_Gyro = rotationMatrix;
    }

    public void setRotationMatrix_Magn(float[] rotationMatrix) {
        this.rotationMatrix_Magn = rotationMatrix;
    }

    public OpenGLRenderer(Scene scene){
        this.scene = scene;
    }


    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {

        gl.glClearColor(0.2f, 0.2f, 0.2f, 0.5f);

        gl.glClearDepthf(1.0f);
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);

        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT,
                GL10.GL_NICEST);

    }

    @Override
    public void onDrawFrame(GL10 gl) {
        gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
        gl.glLoadIdentity();

        gl.glTranslatef(0.0f, 0.0f, -10.0f);
        for (Line vector: scene.getVectorList()) {
            vector.draw(gl);
        }


        gl.glMultMatrixf(rotationMatrix_Gyro, 0);
        scene.getmCube_G().draw(gl);
        scene.getmAxis().draw(gl);

        gl.glLoadIdentity();
        gl.glTranslatef(0.0f, 0.0f, -10.0f);
        gl.glMultMatrixf(rotationMatrix_Magn, 0);
        scene.getmCube_M().draw(gl);
        scene.getmAxis().draw(gl);

        gl.glLoadIdentity();

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL10.GL_PROJECTION);
        gl.glLoadIdentity();
        GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, 0.1f, 100.0f);
        gl.glViewport(0, 0, width, height);

        gl.glMatrixMode(GL10.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

}