package com.voxtric.timegraph;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import java.util.ArrayList;

public class TimeGraph extends ConstraintLayout
{
  private static final boolean DEFAULT_SHOW_Y_MARGIN = true;
  private static final float DEFAULT_MIN_VALUE = 0.0f;
  private static final float DEFAULT_MAX_VALUE = 100.0f;

  private static final float Y_AXIS_MARGIN_DP = 4.0f;

  private boolean m_showYMargin = DEFAULT_SHOW_Y_MARGIN;
  private float m_minValue = DEFAULT_MIN_VALUE;
  private float m_maxValue = DEFAULT_MAX_VALUE;
  private float[] m_midValues = null;

  private SurfaceView m_surfaceView = null;
  private TextView m_minValueView = null;
  private TextView m_maxValueView = null;
  private ArrayList<TextView> m_midValueViews = null;

  public TimeGraph(Context context)
  {
    super(context);
    initialise(context);
  }

  public TimeGraph(Context context, AttributeSet attrs)
  {
    super(context, attrs);
    applyAttributes(context, attrs);
    initialise(context);
  }

  public TimeGraph(Context context, AttributeSet attrs, int defStyleAttr)
  {
    super(context, attrs, defStyleAttr);
    applyAttributes(context, attrs);
    initialise(context);
  }

  public void setShowYMargin(boolean value)
  {
    m_showYMargin = value;

    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(this);
    if (value)
    {
      m_minValueView.setVisibility(View.VISIBLE);
      m_maxValueView.setVisibility(View.VISIBLE);
      constraintSet.connect(m_surfaceView.getId(), ConstraintSet.LEFT, m_maxValueView.getId(), ConstraintSet.RIGHT, (int)dpToPx(getContext(), Y_AXIS_MARGIN_DP));
    }
    else
    {
      m_minValueView.setVisibility(View.GONE);
      m_maxValueView.setVisibility(View.GONE);
      constraintSet.connect(m_surfaceView.getId(), ConstraintSet.LEFT, m_maxValueView.getId(), ConstraintSet.RIGHT, 0);
    }
    constraintSet.applyTo(this);
  }

  public boolean getShowYMargin()
  {
    return m_showYMargin;
  }

  public void setMinValue(float value)
  {
    m_minValue = value;
    m_minValueView.setText(String.valueOf(value));
  }

  public float getMinValue()
  {
    return m_minValue;
  }

  public void setMaxValue(float value)
  {
    m_maxValue = value;
    m_maxValueView.setText(String.valueOf(value));
  }

  public float getMaxValue()
  {
    return m_maxValue;
  }

  public void setMidValues(float[] midValues)
  {
    if (midValues != null)
    {
      if (m_midValueViews == null)
      {
        m_midValueViews = new ArrayList<>(midValues.length);
      }

      int midValueViewCount = m_midValueViews.size();
      int index = 0;
      for (; index < midValues.length; index++)
      {
        if (midValues[index] < m_minValue)
        {
          throw new IllegalArgumentException("'midValues' array must not contain values lower than the minimum.");
        }
        if (midValues[index] > m_maxValue)
        {
          throw new IllegalArgumentException("'midValues' array must not contain values greater than the maximum.");
        }

        TextView textView;
        if (index >= m_midValueViews.size())
        {
          textView = new TextView(getContext());
          textView.setId(View.generateViewId());
          addView(textView);
          m_midValueViews.add(textView);
        }
        else
        {
          textView = m_midValueViews.get(index);
          if (textView == null)
          {
            textView = new TextView(getContext());
            textView.setId(View.generateViewId());
            addView(textView);
            m_midValueViews.set(index, textView);
          }
        }

        textView.setText(String.valueOf(midValues[index]));
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(this);
        constraintSet.connect(textView.getId(), ConstraintSet.RIGHT, m_maxValueView.getId(), ConstraintSet.RIGHT);
        constraintSet.connect(textView.getId(), ConstraintSet.TOP, m_surfaceView.getId(), ConstraintSet.TOP);
        constraintSet.connect(textView.getId(), ConstraintSet.BOTTOM, m_surfaceView.getId(), ConstraintSet.BOTTOM);
        constraintSet.setHorizontalBias(textView.getId(), 1.0f);
        constraintSet.setVerticalBias(textView.getId(), (m_maxValue - midValues[index]) / (m_maxValue - m_minValue));
        constraintSet.applyTo(this);
      }

      for (int i = m_midValueViews.size() - 1; i >= index; i--)
      {
        removeView(m_midValueViews.get(i));
        m_midValueViews.remove(i);
      }
    }
  }

  private void applyAttributes(Context context, AttributeSet attrs)
  {
    TypedArray attributes = context.getTheme().obtainStyledAttributes(
        attrs,
        R.styleable.TimeGraph,
        0, 0);

    try
    {
      m_showYMargin = attributes.getBoolean(R.styleable.TimeGraph_showYMargin, DEFAULT_SHOW_Y_MARGIN);
      m_minValue = attributes.getFloat(R.styleable.TimeGraph_minValue, DEFAULT_MIN_VALUE);
      m_maxValue = attributes.getFloat(R.styleable.TimeGraph_maxValue, DEFAULT_MAX_VALUE);

      if (m_minValue > m_maxValue)
      {
        throw new IllegalArgumentException("Minimum value cannot be greater than maximum value.");
      }
    }
    finally
    {
      attributes.recycle();
    }
  }

  private void initialise(Context context)
  {
    m_surfaceView = new SurfaceView(context);
    m_surfaceView.setId(View.generateViewId());
    addView(m_surfaceView);

    m_minValueView = new TextView(context);
    m_minValueView.setId(View.generateViewId());
    m_minValueView.setText(String.valueOf(m_minValue));
    addView(m_minValueView);

    m_maxValueView = new TextView(context);
    m_maxValueView.setId(View.generateViewId());
    m_maxValueView.setText(String.valueOf(m_maxValue));
    addView(m_maxValueView);

    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(this);

    constraintSet.connect(m_surfaceView.getId(), ConstraintSet.TOP, getId(), ConstraintSet.TOP);
    constraintSet.connect(m_surfaceView.getId(), ConstraintSet.BOTTOM, getId(), ConstraintSet.BOTTOM);
    constraintSet.connect(m_surfaceView.getId(), ConstraintSet.RIGHT, getId(), ConstraintSet.RIGHT);
    constraintSet.connect(m_surfaceView.getId(), ConstraintSet.LEFT, m_maxValueView.getId(), ConstraintSet.RIGHT, m_showYMargin ? (int)dpToPx(context, Y_AXIS_MARGIN_DP) : 0);
    constraintSet.setHorizontalBias(m_surfaceView.getId(), 0.0f);

    constraintSet.connect(m_minValueView.getId(), ConstraintSet.BOTTOM, m_surfaceView.getId(), ConstraintSet.BOTTOM);
    constraintSet.connect(m_minValueView.getId(), ConstraintSet.RIGHT, m_maxValueView.getId(), ConstraintSet.RIGHT);
    constraintSet.setHorizontalBias(m_minValueView.getId(), 1.0f);
    constraintSet.setVerticalBias(m_minValueView.getId(), 1.0f);

    constraintSet.connect(m_minValueView.getId(), ConstraintSet.TOP, m_surfaceView.getId(), ConstraintSet.TOP);
    constraintSet.connect(m_minValueView.getId(), ConstraintSet.LEFT, getId(), ConstraintSet.LEFT);
    constraintSet.setVerticalBias(m_maxValueView.getId(), 0.0f);

    constraintSet.applyTo(this);

    if (!m_showYMargin)
    {
      m_minValueView.setVisibility(View.GONE);
      m_maxValueView.setVisibility(View.GONE);
    }
  }

  private static float dpToPx(Context context, float dp)
  {
    return dp * context.getResources().getDisplayMetrics().density;
  }

  private static float pxToDp(Context context, float px)
  {
    return px / context.getResources().getDisplayMetrics().density;
  }
}
