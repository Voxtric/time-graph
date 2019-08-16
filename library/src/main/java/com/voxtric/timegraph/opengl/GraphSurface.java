package com.voxtric.timegraph.opengl;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.voxtric.timegraph.TimeGraph;

public class GraphSurface extends GLSurfaceView
{
  private TimeGraph m_timeGraph = null;
  private GraphRenderer m_renderer = null;

  private float m_previousPixelX = 0.0f;

  public GraphSurface(Context context)
  {
    super(context);
  }

  public GraphSurface(Context context, AttributeSet attrs)
  {
    super(context, attrs);
  }

  public void initialise(TimeGraph timeGraph)
  {
    m_timeGraph = timeGraph;

    setEGLContextClientVersion(2);
    m_renderer = new GraphRenderer();
    setRenderer(m_renderer);
    setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
  }

  @Override
  public boolean onTouchEvent(MotionEvent motionEvent)
  {
    boolean handled = false;
    float pixelX = motionEvent.getX();

    switch (motionEvent.getAction())
    {
    case MotionEvent.ACTION_DOWN:
      handled = true;
      break;
    case MotionEvent.ACTION_MOVE:
      float pixelDx = pixelX - m_previousPixelX;
      float normalisedDx = pixelDx / getWidth();
      m_timeGraph.scrollAlong(pixelDx, normalisedDx);
      handled = true;
      break;
    case MotionEvent.ACTION_UP:
      m_timeGraph.refresh();
      handled = true;
    }

    m_previousPixelX = pixelX;
    return handled;
  }

  public Line addLine(float[] coords)
  {
    Line line = new Line(coords);
    m_renderer.addRenderable(line);
    requestRender();
    return line;
  }

  public void removeRenderable(Renderable renderable)
  {
    m_renderer.removeRenderable(renderable);
  }
}
