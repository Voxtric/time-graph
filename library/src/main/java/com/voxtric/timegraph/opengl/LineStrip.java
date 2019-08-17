package com.voxtric.timegraph.opengl;

import android.opengl.GLES20;

public class LineStrip extends TransformableRenderable
{
  LineStrip(float[] coords)
  {
    super(coords);
  }

  @Override
  public void draw()
  {
    int shaderHandle = TransformableRenderable.getShaderHandle();

    int vertexPositionHandle = GLES20.glGetAttribLocation(shaderHandle, "vertexPosition");
    GLES20.glEnableVertexAttribArray(vertexPositionHandle);
    GLES20.glVertexAttribPointer(vertexPositionHandle, COORDS_PER_VERTEX,
                                 GLES20.GL_FLOAT, false,
                                 COORDS_PER_VERTEX * (Float.SIZE / Byte.SIZE) , getVertexBuffer());

    int xOffsetHandle = GLES20.glGetUniformLocation(shaderHandle, "xOffset");
    GLES20.glUniform1f(xOffsetHandle, m_xOffset);

    int xScaleHandle = GLES20.glGetUniformLocation(shaderHandle, "xScale");
    GLES20.glUniform1f(xScaleHandle, m_xScale);

    GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, getVertexCount());
  }
}
