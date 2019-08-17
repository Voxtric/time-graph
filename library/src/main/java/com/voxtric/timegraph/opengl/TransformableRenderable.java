package com.voxtric.timegraph.opengl;

import android.opengl.GLES20;

abstract class TransformableRenderable extends Renderable
{
  private static final String VERTEX_SHADER_CODE =
      "uniform float xOffset;" +
      "uniform float xScale;" +
      "attribute vec4 vertexPosition;" +
      "void main() {" +
      "  gl_Position = vec4((vertexPosition.x * xScale) + xOffset, vertexPosition.yzw);" +
      "}";
  private static final String FRAGMENT_SHADER_CODE =
      "precision mediump float;" +
      "void main() {" +
      "  gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);" +
      "}";
  private static int s_shaderHandle = -1;

  float m_xOffset = 0.0f;
  float m_xScale = 1.0f;

  TransformableRenderable(float[] coords)
  {
    super(coords);
  }

  public void setXOffset(float xOffset)
  {
    m_xOffset = xOffset;
  }

  public void setXScale(float xScale)
  {
    m_xScale = xScale;
  }

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
}
