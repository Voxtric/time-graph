package com.voxtric.timegraph.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;

public class GraphSurface extends GLSurfaceView
{
  private GraphRenderer m_renderer;

  public GraphSurface(Context context)
  {
    super(context);
    initialise();
  }

  public GraphSurface(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    initialise();
  }

  private void initialise()
  {
    setEGLContextClientVersion(2);

    m_renderer = new GraphRenderer();
    setRenderer(m_renderer);
    setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
  }

  public Line addLine(final float[] coords)
  {
    final Line line = new Line(coords);
    postDelayed(new Runnable()
    {
      @Override
      public void run()
      {
        m_renderer.addRenderable(line);
        requestRender();
      }
    }, 1000);
    return line;
  }
}
