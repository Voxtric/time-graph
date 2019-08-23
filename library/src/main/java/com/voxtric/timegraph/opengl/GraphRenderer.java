package com.voxtric.timegraph.opengl;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GraphRenderer implements GLSurfaceView.Renderer
{
  private ArrayList<Renderable> m_renderables = new ArrayList<>();

  @Override
  public void onSurfaceCreated(GL10 unused, EGLConfig config)
  {
    GLES20.glClearColor(0.9f, 0.9f, 0.9f, 1.0f);
  }

  @Override
  public void onSurfaceChanged(GL10 unused, int width, int height)
  {
    GLES20.glViewport(0, 0, width, height);
  }

  @Override
  public void onDrawFrame(GL10 unused)
  {
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
    for (Renderable renderable : m_renderables)
    {
      renderable.draw();
    }
  }

  void addRenderable(Renderable renderable)
  {
    boolean added = false;
    for (int i = 0; i < m_renderables.size() && !added; i++)
    {
      if (renderable.getDrawOrder() < m_renderables.get(i).getDrawOrder())
      {
        m_renderables.add(null);
        for (int j = m_renderables.size() - 1; j > i; j--)
        {
          m_renderables.set(j, m_renderables.get(j - 1));
        }
        m_renderables.set(i, renderable);
        added = true;
      }
    }
    if (!added)
    {
      m_renderables.add(renderable);
    }
  }

  public void removeRenderable(Renderable renderable)
  {
    m_renderables.remove(renderable);
  }
}
