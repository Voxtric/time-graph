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
  private float m_previousPixelDistance = 0.0f;
  private boolean m_scaling = false;
  private boolean m_ignoreScroll = false;

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
  protected void onDetachedFromWindow()
  {
    super.onDetachedFromWindow();
    Renderable.releaseShader();
    TransformableRenderable.releaseShader();
    Mesh.releaseShader();
  }

  @Override
  public boolean onTouchEvent(MotionEvent motionEvent)
  {
    boolean handled;

    int pointerIndex = motionEvent.getActionIndex();
    int pointerId = motionEvent.getPointerId(pointerIndex);

    switch (motionEvent.getActionMasked())
    {
    case MotionEvent.ACTION_DOWN:
      m_previousPixelX = motionEvent.getX();
      m_ignoreScroll = false;
      handled = true;
      break;

    case MotionEvent.ACTION_UP:
      m_timeGraph.refresh(false);
      handled = true;
      break;

    case MotionEvent.ACTION_POINTER_DOWN:
      m_scaling = true;
      float startingXDifference = motionEvent.getX(0) - motionEvent.getX(1);
      float startingYDifference = motionEvent.getY(0) - motionEvent.getY(1);
      m_previousPixelDistance = (float)Math.sqrt((startingXDifference * startingXDifference) + (startingYDifference * startingYDifference));
      handled = true;
      break;

    case MotionEvent.ACTION_POINTER_UP:
      m_scaling = false;
      handled = true;
      break;

    case MotionEvent.ACTION_MOVE:
      if (!m_scaling)
      {
        if (!m_ignoreScroll)
        {
          float pixelX = motionEvent.getX();
          float pixelXDelta = pixelX - m_previousPixelX;
          float normalisedXDelta = pixelXDelta / getWidth();
          m_timeGraph.scrollData(normalisedXDelta);
          m_previousPixelX = pixelX;
        }
      }
      else if (motionEvent.getPointerCount() >= 2)
      {
        float pixelXDifference = motionEvent.getX(0) - motionEvent.getX(1);
        float pixelYDifference = motionEvent.getY(0) - motionEvent.getY(1);
        float pixelDistance = (float)Math.sqrt((pixelXDifference * pixelXDifference) + (pixelYDifference * pixelYDifference));
        float normalisedDistanceDelta = (pixelDistance - m_previousPixelDistance) / getWidth();

        float pixelXCentre = motionEvent.getX(0) - (pixelXDifference * 0.5f);
        float normalisedXCentre = pixelXCentre / getWidth();

        m_timeGraph.scaleData(normalisedDistanceDelta, normalisedXCentre);
        m_ignoreScroll = true;
        m_previousPixelDistance = pixelDistance;
      }
      handled = true;
      break;

    default:
      handled = false;
    }

    return handled;
  }

  public LineStrip addLineStrip(int drawOrder, float[] coords)
  {
    final LineStrip lineStrip = new LineStrip(drawOrder, coords);
    queueEvent(new Runnable()
    {
      @Override
      public void run()
      {
        m_renderer.addRenderable(lineStrip);
        requestRender();
      }
    });
    return lineStrip;
  }

  public Line addLine(int drawOrder, float[] coords)
  {
    final Line line = new Line(drawOrder, coords);
    queueEvent(new Runnable()
    {
      @Override
      public void run()
      {
        m_renderer.addRenderable(line);
        requestRender();
      }
    });
    return line;
  }

  public Mesh addMesh(int drawOrder, float[] coords, short[] indices, float[] colors)
  {
    final Mesh mesh = new Mesh(drawOrder, coords, indices, colors);
    queueEvent(new Runnable()
    {
      @Override
      public void run()
      {
        m_renderer.addRenderable(mesh);
        requestRender();
      }
    });
    return mesh;
  }

  public void removeRenderable(final Renderable renderable)
  {
    queueEvent(new Runnable()
    {
      @Override
      public void run()
      {
        m_renderer.removeRenderable(renderable);
        requestRender();
      }
    });
  }
}
