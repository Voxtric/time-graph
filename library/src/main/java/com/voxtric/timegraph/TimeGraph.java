package com.voxtric.timegraph;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.SurfaceView;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import java.util.ArrayList;

public class TimeGraph extends ConstraintLayout
{
  private static final boolean DEFAULT_SHOW_TIME_AXIS = true;
  private static final boolean DEFAULT_SHOW_VALUE_AXIS = true;
  private static final float DEFAULT_MIN_VALUE = 0.0f;
  private static final float DEFAULT_MAX_VALUE = 100.0f;

  private static final float VALUE_AXIS_MARGIN_DP = 4.0f;

  private boolean m_showTimeAxis = DEFAULT_SHOW_TIME_AXIS;
  private boolean m_showValueAxis = DEFAULT_SHOW_VALUE_AXIS;
  private float m_minValue = DEFAULT_MIN_VALUE;
  private float m_maxValue = DEFAULT_MAX_VALUE;

  private long m_startTimestamp = 0;
  private long m_endTimestamp = 1;

  private SurfaceView m_graphSurfaceView = null;

  private RelativeLayout m_timeLabelsLayoutView = null;
  private ArrayList<TextView> m_midValueViews = null;

  private TextView m_minValueView = null;
  private TextView m_maxValueView = null;
  private ArrayList<TextView> m_timeLabelViews = null;

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

  public void setShowXAxis(boolean value)
  {
    m_showTimeAxis = value;
    m_timeLabelsLayoutView.setVisibility(value ? View.VISIBLE : View.GONE);
  }

  public void setShowYAxis(boolean value)
  {
    m_showValueAxis = value;

    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(this);
    if (value)
    {
      m_minValueView.setVisibility(View.VISIBLE);
      m_maxValueView.setVisibility(View.VISIBLE);
      constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.LEFT, m_maxValueView.getId(), ConstraintSet.RIGHT, (int)dpToPx(getContext(),
                                                                                                                                     VALUE_AXIS_MARGIN_DP));
    }
    else
    {
      m_minValueView.setVisibility(View.GONE);
      m_maxValueView.setVisibility(View.GONE);
      constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.LEFT, m_maxValueView.getId(), ConstraintSet.RIGHT, 0);
    }
    constraintSet.applyTo(this);
  }

  public boolean getShowYMargin()
  {
    return m_showValueAxis;
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

  public void setFreshData(Data[] data, long startTimestamp, long endTimestamp)
  {
    m_startTimestamp = startTimestamp;
    m_endTimestamp = endTimestamp;
  }

  public void setMidValueAxisLabels(final float[] midValues)
  {
    if (midValues != null)
    {
      m_maxValueView.post(new Runnable()
      {
        @Override
        public void run()
        {
          if (m_midValueViews == null)
          {
            m_midValueViews = new ArrayList<>(midValues.length);
          }

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
              textView = createTextView(getContext());
              textView.animate().translationYBy(m_maxValueView.getHeight() * 0.5f);
              addView(textView);
              m_midValueViews.add(textView);
            }
            else
            {
              textView = m_midValueViews.get(index);
              if (textView == null)
              {
                textView = createTextView(getContext());
                textView.animate().translationYBy(m_maxValueView.getHeight() * 0.5f);
                addView(textView);
                m_midValueViews.set(index, textView);
              }
            }

            textView.setText(String.valueOf(midValues[index]));
            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(TimeGraph.this);
            constraintSet.connect(textView.getId(), ConstraintSet.RIGHT, m_maxValueView.getId(), ConstraintSet.RIGHT);
            constraintSet.connect(textView.getId(), ConstraintSet.TOP, m_graphSurfaceView.getId(), ConstraintSet.TOP);
            constraintSet.connect(textView.getId(), ConstraintSet.BOTTOM, m_graphSurfaceView.getId(), ConstraintSet.BOTTOM);
            constraintSet.setHorizontalBias(textView.getId(), 1.0f);
            constraintSet.setVerticalBias(textView.getId(), (m_maxValue - midValues[index]) / (m_maxValue - m_minValue));
            constraintSet.applyTo(TimeGraph.this);
          }

          for (int i = m_midValueViews.size() - 1; i >= index; i--)
          {
            removeView(m_midValueViews.get(i));
            m_midValueViews.remove(i);
          }
        }
      });
    }
  }

  public void setTimeAxisLabels(final TimeLabel[] timeLabels)
  {
    if (timeLabels != null)
    {
      m_timeLabelsLayoutView.post(new Runnable()
      {
        @Override
        public void run()
        {
          if (m_timeLabelViews == null)
          {
            m_timeLabelViews = new ArrayList<>(timeLabels.length);
          }

          int index = 0;
          for (; index < timeLabels.length; index++)
          {

            TextView textView;
            if (index >= m_timeLabelViews.size())
            {
              textView = createTextView(getContext());
              m_timeLabelsLayoutView.addView(textView);
              m_timeLabelViews.add(textView);
            }
            else
            {
              textView = m_timeLabelViews.get(index);
              if (textView == null)
              {
                textView = createTextView(getContext());
                m_timeLabelsLayoutView.addView(textView);
                m_timeLabelViews.set(index, textView);
              }
            }

            textView.setText(timeLabels[index].label);
            float widthMultiplier = 1.0f - (float)((double)(m_endTimestamp - timeLabels[index].timestamp) / (double)(m_endTimestamp - m_startTimestamp));
            float width = getWidth() - (m_maxValueView.getWidth() + dpToPx(getContext(), VALUE_AXIS_MARGIN_DP));
            float offset = widthMultiplier * width;
            textView.animate().translationX(0.0f).setDuration(0).start();
            textView.animate().translationXBy(offset).setDuration(0).start();
          }

          for (int i = m_timeLabelViews.size() - 1; i >= index; i--)
          {
            m_timeLabelsLayoutView.removeView(m_timeLabelViews.get(i));
            m_timeLabelViews.remove(i);
          }
        }
      });
    }
  }

  private void applyAttributes(Context context, AttributeSet attrs)
  {
    TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimeGraph, 0, 0);
    try
    {
      m_showTimeAxis = attributes.getBoolean(R.styleable.TimeGraph_showTimeAxis, DEFAULT_SHOW_TIME_AXIS);
      m_showValueAxis = attributes.getBoolean(R.styleable.TimeGraph_showValueAxis, DEFAULT_SHOW_VALUE_AXIS);
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
    m_graphSurfaceView = new SurfaceView(context);
    m_graphSurfaceView.setId(View.generateViewId());
    addView(m_graphSurfaceView);

    m_timeLabelsLayoutView = new RelativeLayout(context);
    m_timeLabelsLayoutView.setId(View.generateViewId());
    addView(m_timeLabelsLayoutView);

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

    // Graph Surface View
    constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.TOP, getId(), ConstraintSet.TOP);
    constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.BOTTOM, m_timeLabelsLayoutView.getId(), ConstraintSet.TOP);
    constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.RIGHT, getId(), ConstraintSet.RIGHT);
    constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.LEFT, m_maxValueView.getId(), ConstraintSet.RIGHT, m_showValueAxis ? (int)dpToPx(context, VALUE_AXIS_MARGIN_DP) : 0);
    constraintSet.setHorizontalBias(m_graphSurfaceView.getId(), 0.0f);
    constraintSet.setVerticalBias(m_graphSurfaceView.getId(), 1.0f);

    // Time Labels Layout View
    constraintSet.connect(m_timeLabelsLayoutView.getId(), ConstraintSet.BOTTOM, getId(), ConstraintSet.BOTTOM);
    constraintSet.connect(m_timeLabelsLayoutView.getId(), ConstraintSet.LEFT, m_graphSurfaceView.getId(), ConstraintSet.LEFT);
    constraintSet.connect(m_timeLabelsLayoutView.getId(), ConstraintSet.RIGHT, m_graphSurfaceView.getId(), ConstraintSet.RIGHT);
    constraintSet.constrainWidth(m_timeLabelsLayoutView.getId(), m_graphSurfaceView.getWidth());
    constraintSet.setHorizontalBias(m_timeLabelsLayoutView.getId(), 0.0f);
    constraintSet.setVerticalBias(m_timeLabelsLayoutView.getId(), 0.0f);

    // Min Value View
    constraintSet.connect(m_minValueView.getId(), ConstraintSet.BOTTOM, m_graphSurfaceView.getId(), ConstraintSet.BOTTOM);
    constraintSet.connect(m_minValueView.getId(), ConstraintSet.RIGHT, m_maxValueView.getId(), ConstraintSet.RIGHT);
    constraintSet.setHorizontalBias(m_minValueView.getId(), 1.0f);
    constraintSet.setVerticalBias(m_minValueView.getId(), 1.0f);

    // Max Value View
    constraintSet.connect(m_minValueView.getId(), ConstraintSet.TOP, m_graphSurfaceView.getId(), ConstraintSet.TOP);
    constraintSet.connect(m_minValueView.getId(), ConstraintSet.LEFT, getId(), ConstraintSet.LEFT);
    constraintSet.setHorizontalBias(m_maxValueView.getId(), 0.0f);
    constraintSet.setVerticalBias(m_maxValueView.getId(), 0.0f);

    constraintSet.applyTo(this);

    if (!m_showTimeAxis)
    {
      m_timeLabelsLayoutView.setVisibility(View.GONE);
    }
    if (!m_showValueAxis)
    {
      m_minValueView.setVisibility(View.GONE);
      m_maxValueView.setVisibility(View.GONE);
    }

    m_maxValueView.post(new Runnable()
    {
      @Override
      public void run()
      {
        m_minValueView.animate().translationYBy(m_minValueView.getHeight() * 0.5f).setDuration(0).start();
        m_maxValueView.animate().translationYBy(m_maxValueView.getHeight() * -0.5f).setDuration(0).start();
      }
    });
  }

  private static TextView createTextView(Context context)
  {
    TextView textView = new TextView(context);
    textView.setId(View.generateViewId());
    return textView;
  }

  private static float dpToPx(Context context, float dp)
  {
    return dp * context.getResources().getDisplayMetrics().density;
  }

  private static float pxToDp(Context context, float px)
  {
    return px / context.getResources().getDisplayMetrics().density;
  }

  public static class TimeLabel
  {
    public long timestamp;
    public String label;

    public TimeLabel(long timestamp, String label)
    {
      this.timestamp = timestamp;
      this.label = label;
    }
  }

  public static class Data
  {
    public long timestamp;
    public float value;

    public Data(long timestamp, float value)
    {
      this.timestamp = timestamp;
      this.value = value;
    }
  }
}
