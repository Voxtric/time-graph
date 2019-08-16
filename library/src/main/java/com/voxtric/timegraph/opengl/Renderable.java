package com.voxtric.timegraph.opengl;

import android.opengl.GLES20;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public abstract class Renderable
{
  public static final int COORDS_PER_VERTEX = 2;

  private static final String VERTEX_SHADER_CODE =
      "uniform float xOffset;" +
      "attribute vec4 vertexPosition;" +
      "void main() {" +
      "  gl_Position = vec4(vertexPosition.x + xOffset, vertexPosition.yzw);" +
      "}";
  private static final String FRAGMENT_SHADER_CODE =
      "precision mediump float;" +
      "void main() {" +
      "  gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);" +
      "}";
  private static int s_shaderHandle = -1;

  private FloatBuffer m_vertexBuffer;
  private int m_vertexCount;
  protected float m_xOffset = 0.0f;

  Renderable(float[] coords)
  {
    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(coords.length * (Float.SIZE / Byte.SIZE));
    byteBuffer.order(ByteOrder.nativeOrder());

    m_vertexBuffer = byteBuffer.asFloatBuffer();
    m_vertexBuffer.put(coords);
    m_vertexBuffer.position(0);

    m_vertexCount = coords.length / COORDS_PER_VERTEX;
  }

  FloatBuffer getVertexBuffer()
  {
    return m_vertexBuffer;
  }

  int getVertexCount()
  {
    return m_vertexCount;
  }

  public abstract void draw();

  static int getShaderHandle()
  {
    if (s_shaderHandle == -1)
    {
      int vertexShaderHandle = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE);
      int fragmentShaderHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE);
      s_shaderHandle = GLES20.glCreateProgram();
      GLES20.glAttachShader(s_shaderHandle, vertexShaderHandle);
      GLES20.glAttachShader(s_shaderHandle, fragmentShaderHandle);
      GLES20.glLinkProgram(s_shaderHandle);

      GLES20.glUseProgram(s_shaderHandle);
    }
    return s_shaderHandle;
  }

  private static int loadShader(int type, String shaderCode)
  {
    int shaderHandle = GLES20.glCreateShader(type);
    GLES20.glShaderSource(shaderHandle, shaderCode);
    GLES20.glCompileShader(shaderHandle);
    return shaderHandle;
  }

  public void setXOffset(float xOffset)
  {
    m_xOffset = xOffset;
  }
}
