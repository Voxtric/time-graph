package com.voxtric.timegraph.opengl;

import android.opengl.GLES20;

public class Line extends Renderable
{
  public Line(float[] coords)
  {
    super(coords);
  }

  @Override
  public void draw()
  {
    int shaderHandle = Renderable.getShaderHandle();

    int vertexPositionHandle = GLES20.glGetAttribLocation(shaderHandle, "vertexPosition");
    GLES20.glEnableVertexAttribArray(vertexPositionHandle);
    GLES20.glVertexAttribPointer(vertexPositionHandle, COORDS_PER_VERTEX,
                                 GLES20.GL_FLOAT, false,
                                 COORDS_PER_VERTEX * (Float.SIZE / Byte.SIZE) , getVertexBuffer());

    int xOffsetHandle = GLES20.glGetUniformLocation(shaderHandle, "xOffset");
    GLES20.glUniform1f(xOffsetHandle, m_xOffset);

    GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, getVertexCount());
  }
}
