package ch.ubiment.sensors.sensordemo.OpenGL;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.opengles.GL10;

public class Line {


    private FloatBuffer mVertexBuffer = null;
    private FloatBuffer mColorBuffer = null;
    private ShortBuffer mIndexBuffer = null;

    private float vertices[];
    private float colors[];

    private short indices[];



    public Line(float[] vert, short[] ind, float[] rgba) {
        vertices = vert.clone();
        indices = ind.clone();
        colors = rgba.clone();

        ByteBuffer byteBuf = ByteBuffer.allocateDirect(vertices.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mVertexBuffer = byteBuf.asFloatBuffer();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);

        byteBuf = ByteBuffer.allocateDirect(colors.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mColorBuffer = byteBuf.asFloatBuffer();
        mColorBuffer.put(colors);
        mColorBuffer.position(0);

        byteBuf = ByteBuffer.allocateDirect(indices.length * 2);
        byteBuf.order(ByteOrder.nativeOrder());
        mIndexBuffer = byteBuf.asShortBuffer();
        mIndexBuffer.put(indices);
        mIndexBuffer.position(0);
    }

    public void draw(GL10 gl) {


        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
        gl.glColorPointer(4, GL10.GL_FLOAT, 0, mColorBuffer);


        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_COLOR_ARRAY);
        // Draw all lines
        gl.glDrawElements(GL10.GL_LINES, indices.length,
                GL10.GL_UNSIGNED_SHORT, mIndexBuffer);

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_COLOR_ARRAY);

    }

    public void setVertices(float[] vertices){
        if (vertices.length != this.vertices.length) throw new AssertionError("Line/setVertices() ATTENTION, mauvais nombre de vertices!");
        this.vertices = vertices.clone();
        mVertexBuffer.put(vertices);
        mVertexBuffer.position(0);
    }

    public float[] getVertices(){
        return vertices.clone();
    }
}