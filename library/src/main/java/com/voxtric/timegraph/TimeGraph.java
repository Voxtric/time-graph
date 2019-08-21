package com.voxtric.timegraph;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.PointF;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.voxtric.timegraph.opengl.GraphSurface;
import com.voxtric.timegraph.opengl.Line;
import com.voxtric.timegraph.opengl.LineStrip;
import com.voxtric.timegraph.opengl.Renderable;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class TimeGraph extends ConstraintLayout
{
  private static final boolean DEFAULT_SHOW_VALUE_AXIS = true;
  private static final float DEFAULT_VALUE_AXIS_TEXT_SIZE_SP = 14.0f;
  private static final int DEFAULT_VALUE_AXIS_TEXT_COLOR = Color.GRAY;
  private static final float DEFAULT_VALUE_AXIS_MIN = 0.0f;
  private static final float DEFAULT_VALUE_AXIS_MAX = 100.0f;

  private static final boolean DEFAULT_SHOW_TIME_AXIS = true;
  private static final float DEFAULT_TIME_AXIS_TEXT_SIZE_SP = 14.0f;
  private static final int DEFAULT_TIME_AXIS_TEXT_COLOR = Color.GRAY;

  private static final boolean DEFAULT_SHOW_NO_DATA_TEXT = true;
  private static final String DEFAULT_NO_DATA_TEXT = "No Data To Display";
  private static final float DEFAULT_NO_DATA_TEXT_SIZE_SP = 18.0f;
  private static final int DEFAULT_NO_DATA_TEXT_COLOR = Color.BLACK;

  private static final boolean DEFAULT_SHOW_REFRESH_PROGRESS = true;

  private static final boolean DEFAULT_ALLOW_SCROLL = true;
  private static final boolean DEFAULT_ALLOW_SCALE = true;

  private static final float VALUE_AXIS_MARGIN_DP = 4.0f;
  private static final long NEW_DATA_ANIMATION_DURATION = 600L;

  private boolean m_showValueAxis = DEFAULT_SHOW_VALUE_AXIS;
  private float m_valueAxisTextSizeSp = DEFAULT_VALUE_AXIS_TEXT_SIZE_SP;
  private int m_valueAxisTextColor = DEFAULT_VALUE_AXIS_TEXT_COLOR;
  private float m_valueAxisMin = DEFAULT_VALUE_AXIS_MIN;
  private float m_valueAxisMax = DEFAULT_VALUE_AXIS_MAX;

  private boolean m_showTimeAxis = DEFAULT_SHOW_TIME_AXIS;
  private float m_timeAxisTextSizeSp = DEFAULT_TIME_AXIS_TEXT_SIZE_SP;
  private int m_timeAxisTextColor = DEFAULT_TIME_AXIS_TEXT_COLOR;

  private boolean m_showNoDataText = DEFAULT_SHOW_NO_DATA_TEXT;
  private CharSequence m_noDataText = DEFAULT_NO_DATA_TEXT;
  private float m_noDataTextSizeSp = DEFAULT_NO_DATA_TEXT_SIZE_SP;
  private int m_noDataTextColor = DEFAULT_NO_DATA_TEXT_COLOR;

  private boolean m_showRefreshProgress = DEFAULT_SHOW_REFRESH_PROGRESS;

  private boolean m_allowScroll = DEFAULT_ALLOW_SCROLL;
  private boolean m_allowScale = DEFAULT_ALLOW_SCALE;

  private long m_startTimestamp = 0L;
  private long m_endTimestamp = 0L;
  private long m_beforeScalingStartTimestamp = Long.MIN_VALUE;
  private long m_beforeScalingEndTimestamp = Long.MAX_VALUE;
  private DataAccessor m_dataAccessor = null;

  private boolean m_refreshing = false;
  private boolean m_newRefreshRequested = false;
  private ProgressBar m_refreshProgressView = null;
  private TextView m_noDataView = null;

  private GraphSurface m_graphSurfaceView = null;
  private float m_xOffset = 0.0f;
  private float m_xScale = 1.0f;
  private float m_normalisedForcedXCentre = -1.0f;

  private LineStrip m_dataLineStrip = null;
  private ValueAnimator m_newDataAnimator = null;

  private Data m_firstDataEntry = null;
  private Data m_lastDataEntry = null;

  private RelativeLayout m_timeLabelsLayoutView = null;
  private ArrayList<TextView> m_valueAxisMidViews = new ArrayList<>();

  private TextView m_valueAxisMinView = null;
  private TextView m_valueAxisMaxView = null;
  private ArrayList<TimeAxisLabel> m_timeAxisLabels = new ArrayList<>();

  private float[] m_rangeHighlightingValues = null;
  private int[] m_rangeHighlightingColors = null;

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

  @Override
  protected Parcelable onSaveInstanceState()
  {
    Parcelable superState = super.onSaveInstanceState();

    Bundle state = new Bundle();
    state.putParcelable("superState", superState);

    state.putBoolean("m_showValueAxis", m_showValueAxis);
    state.putFloat("m_valueAxisTextSizeSp", m_valueAxisTextSizeSp);
    state.putInt("m_valueAxisTextColor", m_valueAxisTextColor);
    state.putFloat("m_valueAxisMin", m_valueAxisMin);
    state.putFloat("m_valueAxisMax", m_valueAxisMax);

    state.putBoolean("m_showTimeAxis", m_showTimeAxis);
    state.putFloat("m_timeAxisTextSizeSp", m_timeAxisTextSizeSp);
    state.putInt("m_timeAxisTextColor", m_timeAxisTextColor);

    state.putBoolean("m_showNoDataText", m_showNoDataText);
    state.putCharSequence("m_noDataText", m_noDataText);
    state.putFloat("m_noDataTextSizeSp", m_noDataTextSizeSp);
    state.putInt("m_noDataTextColor", m_noDataTextColor);

    state.putBoolean("m_showRefreshProgress", m_showRefreshProgress);

    state.putBoolean("m_allowScroll", m_allowScroll);
    state.putBoolean("m_allowScale", m_allowScale);

    float[] valueAxisMidValues = new float[m_valueAxisMidViews.size()];
    for (int i = 0; i < valueAxisMidValues.length; i++)
    {
      valueAxisMidValues[i] = Float.valueOf(m_valueAxisMidViews.get(i).getText().toString());
    }
    state.putFloatArray("valueAxisMidValues", valueAxisMidValues);

    return state;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state)
  {
    if (state instanceof Bundle)
    {
      Bundle bundle = (Bundle)state;
      state = bundle.getParcelable("superState");

      setShowValueAxis(bundle.getBoolean("m_showValueAxis"));
      setValueAxisTextSizeSp(bundle.getFloat("m_valueAxisTextSizeSp"));
      setValueAxisTextColor(bundle.getInt("m_valueAxisTextColor"));
      setValueAxisMin(bundle.getFloat("m_valueAxisMin"));
      setValueAxisMax(bundle.getFloat("m_valueAxisMax"));

      setShowTimeAxis(bundle.getBoolean("m_showTimeAxis"));
      setTimeAxisTextSizeSp(bundle.getFloat("m_timeAxisTextSizeSp"));
      setTimeAxisTextColor(bundle.getInt("m_timeAxisTextColor"));

      setShowNoDataText(bundle.getBoolean("m_showNoDataText"));
      setNoDataText(bundle.getCharSequence("m_noDataText"));
      setNoDataTextSizeSp(bundle.getFloat("m_noDataTextSizeSp"));
      setNoDataTextColor(bundle.getInt("m_noDataTextColor"));

      setShowRefreshProgress(bundle.getBoolean("m_showRefreshProgress"));

      setAllowScroll(bundle.getBoolean("m_allowScroll"));
      setAllowScale(bundle.getBoolean("m_allowScale"));

      setValueAxisMidLabels(bundle.getFloatArray("valueAxisMidValues"));
    }
    super.onRestoreInstanceState(state);
  }

  public void setShowValueAxis(boolean value)
  {
    m_showValueAxis = value;
    int visibility = value ? View.VISIBLE : View.GONE;

    m_valueAxisMinView.setVisibility(visibility);
    m_valueAxisMaxView.setVisibility(visibility);
    for (TextView view : m_valueAxisMidViews)
    {
      view.setVisibility(visibility);
    }

    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(this);
    if (value)
    {
      constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.LEFT,
                            m_valueAxisMaxView.getId(), ConstraintSet.RIGHT,
                            dpToPx(getContext(), VALUE_AXIS_MARGIN_DP));
    }
    else
    {
      constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.LEFT,
                            m_valueAxisMaxView.getId(), ConstraintSet.RIGHT, 0);
    }
    constraintSet.applyTo(this);
  }

  public boolean getShowValueAxis()
  {
    return m_showValueAxis;
  }

  public void setValueAxisTextSizeSp(float textSizeSp)
  {
    m_valueAxisTextSizeSp = textSizeSp;
    m_valueAxisMinView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
    m_valueAxisMaxView.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
    for (TextView view : m_valueAxisMidViews)
    {
      view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
    }
  }

  public float getValueAxisTextSizeSp()
  {
    return m_valueAxisTextSizeSp;
  }

  public void setValueAxisTextColor(int color)
  {
    m_valueAxisTextColor = color;
    m_valueAxisMinView.setTextColor(color);
    m_valueAxisMaxView.setTextColor(color);
    for (TextView view : m_valueAxisMidViews)
    {
      view.setTextColor(color);
    }
  }

  public int getValueAxisTextColor(int color)
  {
    return m_valueAxisTextColor;
  }

  public void setValueAxisMin(float value)
  {
    m_valueAxisMin = value;
    m_valueAxisMinView.setText(String.valueOf(value));
  }

  public float getValueAxisMin()
  {
    return m_valueAxisMin;
  }

  public void setValueAxisMax(float value)
  {
    m_valueAxisMax = value;
    m_valueAxisMaxView.setText(String.valueOf(value));
  }

  public float getValueAxisMax()
  {
    return m_valueAxisMax;
  }

  public void setShowTimeAxis(boolean value)
  {
    m_showTimeAxis = value;
    m_timeLabelsLayoutView.setVisibility(value ? View.VISIBLE : View.GONE);
  }

  public boolean getShowTimeAxis()
  {
    return m_showTimeAxis;
  }

  public void setTimeAxisTextSizeSp(float textSizeSp)
  {
    m_timeAxisTextSizeSp = textSizeSp;
    for (TimeAxisLabel label : m_timeAxisLabels)
    {
      label.view.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSizeSp);
    }
  }

  public float getTimeAxisTextSizeSp()
  {
    return m_timeAxisTextSizeSp;
  }

  public void setTimeAxisTextColor(int color)
  {
    m_timeAxisTextColor = color;
    for (TimeAxisLabel label : m_timeAxisLabels)
    {
      label.view.setTextColor(color);
    }
  }

  public int getTimeAxisTextColor(int color)
  {
    return m_timeAxisTextColor;
  }

  public void setShowNoDataText(boolean value)
  {
    m_showNoDataText = value;
    if (!value)
    {
      m_noDataView.setVisibility(View.INVISIBLE);
    }
    else if (m_endTimestamp - m_startTimestamp == 0L)
    {
      m_noDataView.setVisibility(View.VISIBLE);
    }
  }

  public boolean getShowNoDataText()
  {
    return m_showNoDataText;
  }

  public void setNoDataText(CharSequence text)
  {
    m_noDataText = text;
    m_noDataView.setText(text);
  }

  public CharSequence getNoDataText()
  {
    return m_noDataText;
  }

  public void setNoDataTextSizeSp(float textSizeSp)
  {
    m_noDataTextSizeSp = textSizeSp;
    m_noDataView.setTextSize(textSizeSp);
  }

  public float getNoDataTextSizeSp()
  {
    return m_timeAxisTextSizeSp;
  }

  public void setNoDataTextColor(int color)
  {
    m_noDataTextColor = color;
    m_noDataView.setTextColor(color);
  }

  public int getNoDataTextColor(int color)
  {
    return m_noDataTextColor;
  }

  public void setShowRefreshProgress(boolean value)
  {
    m_showRefreshProgress = value;
    m_refreshProgressView.setVisibility(value && m_refreshing ? View.VISIBLE : View.INVISIBLE);
  }

  public boolean getShowRefreshProgress()
  {
    return m_showRefreshProgress;
  }

  public void setAllowScroll(boolean allow)
  {
    m_allowScroll = allow;
  }

  public boolean getAllowScroll()
  {
    return m_allowScroll;
  }

  public void setAllowScale(boolean allow)
  {
    m_allowScale = allow;
  }

  public boolean getAllowScale()
  {
    return m_allowScale;
  }

  public void setValueAxisMidLabels(final float[] midValues)
  {
    if (midValues != null)
    {
      int index = 0;
      for (; index < midValues.length; index++)
      {
        if (midValues[index] < m_valueAxisMin)
        {
          throw new IllegalArgumentException("'midValues' array must not contain values lower than the minimum.");
        }
        if (midValues[index] > m_valueAxisMax)
        {
          throw new IllegalArgumentException("'midValues' array must not contain values greater than the maximum.");
        }

        TextView textView;
        if (index >= m_valueAxisMidViews.size())
        {
          textView = createTextView(getContext(), m_valueAxisTextSizeSp, m_valueAxisTextColor);
          addView(textView);
          m_valueAxisMidViews.add(textView);
        }
        else
        {
          textView = m_valueAxisMidViews.get(index);
          if (textView == null)
          {
            textView = createTextView(getContext(), m_valueAxisTextSizeSp, m_valueAxisTextColor);
            addView(textView);
            m_valueAxisMidViews.set(index, textView);
          }
        }

        textView.setText(String.valueOf(midValues[index]));
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(TimeGraph.this);
        constraintSet.connect(textView.getId(), ConstraintSet.RIGHT, m_valueAxisMaxView.getId(), ConstraintSet.RIGHT);
        constraintSet.connect(textView.getId(), ConstraintSet.TOP, m_graphSurfaceView.getId(), ConstraintSet.TOP);
        constraintSet.connect(textView.getId(), ConstraintSet.BOTTOM, m_graphSurfaceView.getId(), ConstraintSet.BOTTOM);
        constraintSet.setHorizontalBias(textView.getId(), 1.0f);
        constraintSet.setVerticalBias(textView.getId(), (m_valueAxisMax - midValues[index]) / (m_valueAxisMax - m_valueAxisMin));
        constraintSet.applyTo(TimeGraph.this);
      }

      for (int i = m_valueAxisMidViews.size() - 1; i >= index; i--)
      {
        removeView(m_valueAxisMidViews.get(i));
        m_valueAxisMidViews.remove(i);
      }
    }
  }

  public void setTimeAxisLabels(final TimeAxisLabelData[] timeAxisLabelData)
  {
    if (timeAxisLabelData != null)
    {
      post(new Runnable()
      {
        @Override
        public void run()
        {
          double difference = (double)(m_endTimestamp - m_startTimestamp);
          int index = 0;
          for (; index < timeAxisLabelData.length; index++)
          {

            TimeAxisLabel timeAxisLabel;
            if (index >= m_timeAxisLabels.size())
            {
              timeAxisLabel = new TimeAxisLabel(createTextView(getContext(), m_timeAxisTextSizeSp, m_timeAxisTextColor));
              timeAxisLabel.view.setBackgroundResource(R.drawable.label_border);
              timeAxisLabel.view.setPadding(dpToPx(getContext(), 3), 0, 0, 0);
              m_timeLabelsLayoutView.addView(timeAxisLabel.view);
              m_timeAxisLabels.add(timeAxisLabel);
            }
            else
            {
              timeAxisLabel = m_timeAxisLabels.get(index);
              if (timeAxisLabel == null)
              {
                timeAxisLabel = new TimeAxisLabel(createTextView(getContext(), m_timeAxisTextSizeSp, m_timeAxisTextColor));
                timeAxisLabel.view.setBackgroundResource(R.drawable.label_border);
                timeAxisLabel.view.setPadding(dpToPx(getContext(), 3), 0, 0, 0);
                m_timeLabelsLayoutView.addView(timeAxisLabel.view);
              }
            }

            timeAxisLabel.view.setText(timeAxisLabelData[index].label);
            float widthMultiplier = 1.0f - (float)((double)(m_endTimestamp - timeAxisLabelData[index].timestamp) / difference);
            float offset = widthMultiplier * m_graphSurfaceView.getWidth();
            timeAxisLabel.view.animate().translationX(0.0f).setDuration(0).start();
            timeAxisLabel.view.animate().translationXBy(offset).setDuration(0).start();
            timeAxisLabel.offset = offset;

            if (timeAxisLabel.marker != null)
            {
              m_graphSurfaceView.removeRenderable(timeAxisLabel.marker);
            }
            float markerX = (((offset / m_graphSurfaceView.getWidth()) * 2.0f) - 1.0f) + 0.0015f;
            timeAxisLabel.marker = m_graphSurfaceView.addLine(markerX, -1.0f, markerX, -0.9f);
          }

          for (int i = m_timeAxisLabels.size() - 1; i >= index; i--)
          {
            TimeAxisLabel label = m_timeAxisLabels.get(i);
            m_timeLabelsLayoutView.removeView(label.view);
            if (label.marker != null)
            {
              m_graphSurfaceView.removeRenderable(label.marker);
            }
            m_timeAxisLabels.remove(i);
          }
        }
      });
    }
  }

  public void setRangeHighlights(float[] upperBoundaries, int[] colors)
  {
    m_rangeHighlightingValues = upperBoundaries;
    m_rangeHighlightingColors = colors;
  }

  public void setVisibleDataPeriod(long startTimestamp, long endTimestamp, @NonNull final DataAccessor dataAccessor, boolean animate)
  {
    m_startTimestamp = startTimestamp;
    m_endTimestamp = endTimestamp;
    m_dataAccessor = dataAccessor;

    refresh(animate);
  }

  public long getVisibleStartTimestamp()
  {
    return m_startTimestamp;
  }

  public long getVisibleEndTimestamp()
  {
    return m_endTimestamp;
  }

  public void clearData()
  {
    m_startTimestamp = 0L;
    m_endTimestamp = 0L;
    m_dataAccessor = null;

    refresh(false);
  }

  public void refresh(boolean animateNew)
  {
    m_normalisedForcedXCentre = -1.0f;
    final long timeDifference = m_endTimestamp - m_startTimestamp;
    if (timeDifference > 0L)
    {
      createNewDataLineStrip(timeDifference, animateNew);
    }
    else
    {
      clearDataLineStrip();
    }
  }

  private void createNewDataLineStrip(final long timeDifference, final boolean animate)
  {
    if (m_refreshing)
    {
      m_newRefreshRequested = true;
    }
    else
    {
      m_refreshing = true;
      m_newRefreshRequested = true;
      post(new Runnable()
      {
        @Override
        public void run()
        {
          if (m_showRefreshProgress)
          {
            m_refreshProgressView.setVisibility(View.VISIBLE);
          }
          m_noDataView.setVisibility(View.INVISIBLE);
        }
      });

      AsyncTask.execute(new Runnable()
      {
        @Override
        public void run()
        {
          float valueDifference = m_valueAxisMax - m_valueAxisMin;
          if (m_dataLineStrip != null)
          {
            m_graphSurfaceView.removeRenderable(m_dataLineStrip);
          }

          Data[] data = null;
          if (m_dataAccessor != null)
          {
            while (m_newRefreshRequested)
            {
              m_newRefreshRequested = false;
              data = m_dataAccessor.getData(m_startTimestamp - timeDifference,
                                            m_endTimestamp + timeDifference,
                                            m_startTimestamp,
                                            m_endTimestamp);
            }

            if (data != null && data.length > 0)
            {
              m_firstDataEntry = data[0];
              m_lastDataEntry = data[data.length - 1];
              setTimeAxisLabels(m_dataAccessor.getLabelsForData(data));
              float floatTimeDifference = (float)timeDifference;
              if (m_firstDataEntry.timestamp > m_startTimestamp)
              {
                m_startTimestamp = m_firstDataEntry.timestamp;
                floatTimeDifference = (float)(m_endTimestamp - m_startTimestamp);
              }
              if (m_lastDataEntry.timestamp < m_endTimestamp)
              {
                m_endTimestamp = m_lastDataEntry.timestamp;
                floatTimeDifference = (float)(m_endTimestamp - m_startTimestamp);
              }

              createDataLineStrip(data, floatTimeDifference, valueDifference);
              createHighlightMesh(data, floatTimeDifference, valueDifference);

              m_xOffset = 0.0f;
              m_xScale = 1.0f;
              m_beforeScalingStartTimestamp = Long.MIN_VALUE;
              m_beforeScalingEndTimestamp = Long.MAX_VALUE;
            }
          }

          m_refreshing = false;
          if (m_showRefreshProgress)
          {
            final boolean dataApplied = data != null && data.length > 0;
            post(new Runnable()
            {
              @Override
              public void run()
              {
                m_refreshProgressView.setVisibility(View.INVISIBLE);
                m_noDataView.setVisibility(!dataApplied && m_showNoDataText ? View.VISIBLE : View.INVISIBLE);
              }
            });
          }

          if (animate)
          {
            post(new Runnable()
            {
              @Override
              public void run()
              {
                m_newDataAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
                m_newDataAnimator.setDuration(NEW_DATA_ANIMATION_DURATION);
                m_newDataAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
                {
                  @Override
                  public void onAnimationUpdate(ValueAnimator valueAnimator)
                  {
                    if (m_dataLineStrip != null)
                    {
                      m_dataLineStrip.setYScale((float)valueAnimator.getAnimatedValue());
                      m_graphSurfaceView.requestRender();
                    }
                  }
                });
                m_newDataAnimator.start();
              }
            });
          }
        }
      });
    }
  }

  private void clearDataLineStrip()
  {
    if (m_dataLineStrip != null)
    {
      m_graphSurfaceView.removeRenderable(m_dataLineStrip);
    }
    setTimeAxisLabels(new TimeAxisLabelData[0]);
    post(new Runnable()
    {
      @Override
      public void run()
      {
        m_noDataView.setVisibility(m_showNoDataText ? View.VISIBLE : View.INVISIBLE);
      }
    });
  }

  private void createDataLineStrip(Data[] data, float timeDifference, float valueDifference)
  {
    float[] coords = new float[data.length * Renderable.COORDS_PER_VERTEX];
    int coordsIndex = 0;
    for (Data datum : data)
    {
      float xCoord = 1.0f - (m_endTimestamp - datum.timestamp) / timeDifference;
      float yCoord = (m_valueAxisMax - datum.value) / valueDifference;
      coords[coordsIndex] = (xCoord * 2.0f) - 1.0f;
      coords[coordsIndex + 1] = (yCoord * 2.0f) - 1.0f;
      coordsIndex += 2;
    }
    m_dataLineStrip = m_graphSurfaceView.addLineStrip(coords);
  }

  private void createHighlightMesh(Data[] data, float timeDifference, float valueDifference)
  {
    ArrayList<Float> coords = new ArrayList<>();
    ArrayList<Short> indices = new ArrayList<>();
    ArrayList<Float> colors = new ArrayList<>();
    short indexStart = 0;
    for (int i = 0; i < data.length - 1; i++)
    {
      Data start = data[i];
      float startXCoord = 1.0f - (m_endTimestamp - start.timestamp) / timeDifference;
      float startYCoord = (m_valueAxisMax - start.value) / valueDifference;
      Data end = data[i + 1];
      float endXCoord = 1.0f - (m_endTimestamp - end.timestamp) / timeDifference;
      float endYCoord = (m_valueAxisMax - end.value) / valueDifference;
      if (endYCoord < startYCoord)
      {
        float tempXCoord = startXCoord;
        float tempYCoord = startYCoord;
        startXCoord = endXCoord;
        startYCoord = endYCoord;
        endXCoord = tempXCoord;
        endYCoord = tempYCoord;
      }

      float lastX = startXCoord;
      float lastY = startYCoord;
      int lowestHighlightValueIntersected = -1;
      for (int j = 0; j < m_rangeHighlightingValues.length; j++)
      {
        float normalisedRangeValue = m_rangeHighlightingValues[j] / valueDifference;
        PointF intersection = new PointF();
        if (getRangeIntersection(startXCoord, startYCoord, endXCoord, endYCoord, normalisedRangeValue, intersection))
        {
          coords.add(lastX);
          coords.add(lastY);
          coords.add(intersection.x);
          coords.add(intersection.y);
          coords.add(endXCoord);
          coords.add(intersection.y);
          coords.add(endXCoord);
          coords.add(lastY);

          indices.add(indexStart);
          indices.add((short)(indexStart + 1));
          indices.add((short)(indexStart + 2));
          indices.add(indexStart);
          indices.add((short)(indexStart + 2));
          indices.add((short)(indexStart + 3));
          indexStart += 4;

          colors.add(1.0f);
          colors.add(0.0f);
          colors.add(0.0f);
          colors.add(0.0f);
          colors.add(0.0f);
          colors.add(1.0f);
          colors.add(0.0f);
          colors.add(0.0f);
          colors.add(0.0f);
          colors.add(1.0f);
          colors.add(0.0f);
          colors.add(0.0f);
          colors.add(1.0f);
          colors.add(0.0f);
          colors.add(0.0f);
          colors.add(0.0f);

          lastX = intersection.x;
          lastY = intersection.y;

          if (lowestHighlightValueIntersected == -1)
          {
            lowestHighlightValueIntersected = j;
          }
        }
        else if (lowestHighlightValueIntersected != -1)
        {
          break;
        }
      }

      coords.add(lastX);
      coords.add(lastY);
      coords.add(endXCoord);
      coords.add(endYCoord);
      coords.add(endXCoord);
      coords.add(lastY);

      indices.add(indexStart);
      indices.add((short)(indexStart + 1));
      indices.add((short)(indexStart + 2));
      indexStart += 3;

      colors.add(1.0f);
      colors.add(0.0f);
      colors.add(0.0f);
      colors.add(0.0f);
      colors.add(0.0f);
      colors.add(1.0f);
      colors.add(0.0f);
      colors.add(0.0f);
      colors.add(1.0f);
      colors.add(0.0f);
      colors.add(0.0f);
      colors.add(0.0f);

      lastY = startYCoord;
      for (int j = lowestHighlightValueIntersected - 1; j >= 0; j--)
      {
        float normalisedRangeValue = m_rangeHighlightingValues[j] / valueDifference;

        coords.add(startXCoord);
        coords.add(normalisedRangeValue);
        coords.add(startXCoord);
        coords.add(lastY);
        coords.add(endXCoord);
        coords.add(lastY);
        coords.add(endXCoord);
        coords.add(normalisedRangeValue);

        indices.add(indexStart);
        indices.add((short)(indexStart + 1));
        indices.add((short)(indexStart + 2));
        indices.add(indexStart);
        indices.add((short)(indexStart + 2));
        indices.add((short)(indexStart + 3));
        indexStart += 4;

        colors.add(1.0f);
        colors.add(0.0f);
        colors.add(0.0f);
        colors.add(0.0f);
        colors.add(0.0f);
        colors.add(1.0f);
        colors.add(0.0f);
        colors.add(0.0f);
        colors.add(0.0f);
        colors.add(1.0f);
        colors.add(0.0f);
        colors.add(0.0f);
        colors.add(1.0f);
        colors.add(0.0f);
        colors.add(0.0f);
        colors.add(0.0f);

        lastY = normalisedRangeValue;
      }
    }

    float[] coordArray = new float[coords.size()];
    for (int i = 0; i < coordArray.length; i++)
    {
      coordArray[i] = (coords.get(i) * 2.0f) - 1.0f;
      //coordArray[i] = coords.get(i);
    }
    short[] indexArray = new short[indices.size()];
    for (int i = 0; i < indexArray.length; i++)
    {
      indexArray[i] = indices.get(i);
    }
    float[] colorArray = new float[colors.size()];
    for (int i = 0; i < colorArray.length; i++)
    {
      colorArray[i] = colors.get(i);
    }
    m_graphSurfaceView.addMesh(coordArray, indexArray, colorArray);

    /*for (int i = 0; i < indices.size(); i += 3)
    {
      m_graphSurfaceView.addLine(coordArray[indexArray[i] * 2] + 0.1f,
                                 coordArray[indexArray[i] * 2 + 1],
                                 coordArray[indexArray[i + 1] * 2] + 0.1f,
                                 coordArray[indexArray[i + 1] * 2 + 1]);
      m_graphSurfaceView.addLine(coordArray[indexArray[i + 1] * 2] + 0.1f,
                                 coordArray[indexArray[i + 1] * 2 + 1],
                                 coordArray[indexArray[i + 2] * 2] + 0.1f,
                                 coordArray[indexArray[i + 2] * 2 + 1]);
      m_graphSurfaceView.addLine(coordArray[indexArray[i + 2] * 2] + 0.1f,
                                 coordArray[indexArray[i + 2] * 2 + 1],
                                 coordArray[indexArray[i] * 2] + 0.1f,
                                 coordArray[indexArray[i] * 2 + 1]);
    }*/
  }

  public void scrollData(float normalisedScrollDelta)
  {
    if (m_allowScroll)
    {
      long timeDifference = m_endTimestamp - m_startTimestamp;

      long startToFirstDifference = m_startTimestamp - m_firstDataEntry.timestamp;
      float normalisedStartToFirstDifference = startToFirstDifference / (float)timeDifference;
      long endToLastDifference = m_endTimestamp - m_lastDataEntry.timestamp;
      float normalisedEndToLastDifference = endToLastDifference / (float)timeDifference;

      if (dataFits())
      {
        if (normalisedScrollDelta > 0.0f)
        {
          normalisedScrollDelta = Math.min(normalisedScrollDelta, normalisedStartToFirstDifference);
        }
        else
        {
          normalisedScrollDelta = Math.max(normalisedScrollDelta, normalisedEndToLastDifference);
        }
      }
      else
      {
        if (normalisedScrollDelta > 0.0f)
        {
          normalisedScrollDelta = Math.min(normalisedScrollDelta, normalisedEndToLastDifference);
        }
        else
        {
          normalisedScrollDelta = Math.max(normalisedScrollDelta, normalisedStartToFirstDifference);
        }
      }

      long timeChange = (long)(timeDifference * normalisedScrollDelta);
      m_startTimestamp -= timeChange;
      m_endTimestamp -= timeChange;

      normalisedScrollDelta = timeChange / (float)timeDifference; // Take into account rounding errors.
      m_xOffset += normalisedScrollDelta;

      float pixelMove = normalisedScrollDelta * m_graphSurfaceView.getWidth();
      for (TimeAxisLabel label : m_timeAxisLabels)
      {
        label.view.animate().translationXBy(pixelMove).setDuration(0).start();
        label.offset += pixelMove;
      }

      if (m_dataLineStrip != null)
      {
        m_dataLineStrip.setXOffset(m_xOffset * 2.0f);
      }
      for (TimeAxisLabel label : m_timeAxisLabels)
      {
        if (label.marker != null)
        {
          label.marker.setXOffset(m_xOffset * 2.0f);
        }
      }
      m_graphSurfaceView.requestRender();
    }
  }

  public void scaleData(float normalisedScaleDelta, float normalisedXCentre)
  {
    if (m_allowScale && (dataFits() || normalisedScaleDelta > 0.0f))
    {
      if (m_normalisedForcedXCentre != -1.0f)
      {
        normalisedXCentre = m_normalisedForcedXCentre;
      }

      if (m_beforeScalingStartTimestamp == Long.MIN_VALUE && m_beforeScalingEndTimestamp == Long.MAX_VALUE)
      {
        scrollData(-m_xOffset);
        m_beforeScalingStartTimestamp = m_startTimestamp;
        m_beforeScalingEndTimestamp = m_endTimestamp;
      }

      m_xScale *= 1.0f + normalisedScaleDelta;

      float timingScale = 1.0f / m_xScale;
      m_startTimestamp = (long)scaleValue(
          m_beforeScalingStartTimestamp,
          m_beforeScalingEndTimestamp,
          m_beforeScalingStartTimestamp,
          timingScale,
          normalisedXCentre);
      m_endTimestamp = (long)scaleValue(
          m_beforeScalingStartTimestamp,
          m_beforeScalingEndTimestamp,
          m_beforeScalingEndTimestamp,
          timingScale,
          normalisedXCentre);

      long startToFirstDifference = m_firstDataEntry.timestamp - m_startTimestamp;
      long endToLastDifference = m_endTimestamp - m_lastDataEntry.timestamp;
      if (startToFirstDifference > 0 && endToLastDifference <= 0)
      {
        m_startTimestamp += startToFirstDifference;
        m_endTimestamp += startToFirstDifference;
        refresh(false);
        m_normalisedForcedXCentre = 0.0f;
      }
      else if (endToLastDifference > 0 && startToFirstDifference <= 0)
      {
        m_startTimestamp -= endToLastDifference;
        m_endTimestamp -= endToLastDifference;
        refresh(false);
        m_normalisedForcedXCentre = 1.0f;
      }
      else
      {
        for (TimeAxisLabel label : m_timeAxisLabels)
        {
          float labelPosition = (float)scaleValue(0.0,
                                                  m_graphSurfaceView.getWidth(),
                                                  label.offset,
                                                  m_xScale,
                                                  normalisedXCentre);
          label.view.animate().translationX(labelPosition).setDuration(0).start();
        }

        if (m_dataLineStrip != null)
        {
          m_dataLineStrip.setXScale(m_xScale, (normalisedXCentre * 2.0f) - 1.0f);
        }
        for (TimeAxisLabel label : m_timeAxisLabels)
        {
          if (label.marker != null)
          {
            label.marker.setXScale(m_xScale, (normalisedXCentre * 2.0f) - 1.0f);
          }
        }
        m_graphSurfaceView.requestRender();
      }
    }
  }

  private boolean dataFits()
  {
    long timeDifference = m_endTimestamp - m_startTimestamp;
    long firstToLastDifference = m_lastDataEntry.timestamp - m_firstDataEntry.timestamp;
    return firstToLastDifference > timeDifference;
  }

  private void applyAttributes(Context context, AttributeSet attrs)
  {
    TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimeGraph, 0, 0);
    try
    {
      m_showValueAxis = attributes.getBoolean(R.styleable.TimeGraph_showValueAxis, DEFAULT_SHOW_VALUE_AXIS);
      m_valueAxisTextSizeSp = attributes.getDimension(R.styleable.TimeGraph_valueAxis_textSize, DEFAULT_VALUE_AXIS_TEXT_SIZE_SP);
      m_valueAxisTextColor = attributes.getColor(R.styleable.TimeGraph_valueAxis_textColor, DEFAULT_VALUE_AXIS_TEXT_COLOR);
      m_valueAxisMin = attributes.getFloat(R.styleable.TimeGraph_valueAxis_min, DEFAULT_VALUE_AXIS_MIN);
      m_valueAxisMax = attributes.getFloat(R.styleable.TimeGraph_valueAxis_max, DEFAULT_VALUE_AXIS_MAX);

      m_showTimeAxis = attributes.getBoolean(R.styleable.TimeGraph_showTimeAxis, DEFAULT_SHOW_TIME_AXIS);
      m_timeAxisTextSizeSp = attributes.getDimension(R.styleable.TimeGraph_timeAxis_textSize, DEFAULT_TIME_AXIS_TEXT_SIZE_SP);
      m_timeAxisTextColor = attributes.getColor(R.styleable.TimeGraph_timeAxis_textColor, DEFAULT_TIME_AXIS_TEXT_COLOR);

      m_showNoDataText = attributes.getBoolean(R.styleable.TimeGraph_showNoDataText, DEFAULT_SHOW_NO_DATA_TEXT);
      m_noDataText = attributes.getText(R.styleable.TimeGraph_noData_text);
      if (m_noDataText == null)
      {
        m_noDataText = DEFAULT_NO_DATA_TEXT;
      }
      m_noDataTextSizeSp = attributes.getDimension(R.styleable.TimeGraph_noData_textSize, DEFAULT_NO_DATA_TEXT_SIZE_SP);
      m_noDataTextColor = attributes.getColor(R.styleable.TimeGraph_noData_textColor, DEFAULT_NO_DATA_TEXT_COLOR);

      m_showRefreshProgress = attributes.getBoolean(R.styleable.TimeGraph_showRefreshProgress, DEFAULT_SHOW_REFRESH_PROGRESS);

      m_allowScroll = attributes.getBoolean(R.styleable.TimeGraph_allowScroll, DEFAULT_ALLOW_SCROLL);
      m_allowScale = attributes.getBoolean(R.styleable.TimeGraph_allowScale, DEFAULT_ALLOW_SCALE);

      if (m_valueAxisMin > m_valueAxisMax)
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
    m_graphSurfaceView = new GraphSurface(context);
    m_graphSurfaceView.setId(R.id.graph_surface);
    m_graphSurfaceView.initialise(this);
    addView(m_graphSurfaceView);

    m_timeLabelsLayoutView = new RelativeLayout(context);
    m_timeLabelsLayoutView.setId(R.id.time_labels_layout);
    addView(m_timeLabelsLayoutView);

    m_valueAxisMinView = new TextView(context);
    m_valueAxisMinView.setId(R.id.min_value);
    m_valueAxisMinView.setText(String.valueOf(m_valueAxisMin));
    m_valueAxisMinView.setTextSize(m_valueAxisTextSizeSp);
    m_valueAxisMinView.setTextColor(m_valueAxisTextColor);
    addView(m_valueAxisMinView);

    m_valueAxisMaxView = new TextView(context);
    m_valueAxisMaxView.setId(R.id.max_value);
    m_valueAxisMaxView.setText(String.valueOf(m_valueAxisMax));
    m_valueAxisMaxView.setTextSize(m_valueAxisTextSizeSp);
    m_valueAxisMaxView.setTextColor(m_valueAxisTextColor);
    addView(m_valueAxisMaxView);

    m_refreshProgressView = new ProgressBar(context);
    m_refreshProgressView.setId(R.id.refresh_progress);
    addView(m_refreshProgressView);

    m_noDataView = new TextView(context);
    m_noDataView.setId(R.id.no_data);
    m_noDataView.setGravity(Gravity.CENTER);
    m_noDataView.setText(m_noDataText);
    m_noDataView.setTextSize(m_noDataTextSizeSp);
    m_noDataView.setTextColor(m_noDataTextColor);
    addView(m_noDataView);

    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(context, R.layout.graph_view);
    constraintSet.applyTo(this);

    if (!m_showTimeAxis)
    {
      m_timeLabelsLayoutView.setVisibility(View.GONE);
    }
    if (!m_showValueAxis)
    {
      m_valueAxisMinView.setVisibility(View.GONE);
      m_valueAxisMaxView.setVisibility(View.GONE);
    }
    m_refreshProgressView.setVisibility(View.INVISIBLE);
    if (!m_showNoDataText)
    {
      m_noDataView.setVisibility(View.INVISIBLE);
    }
  }

  private static TextView createTextView(Context context, float textSize, int textColor)
  {
    TextView textView = new TextView(context);
    textView.setId(View.generateViewId());
    textView.setTextSize(textSize);
    textView.setTextColor(textColor);
    return textView;
  }

  private static double scaleValue(double rangeStart, double rangeEnd, double value, double scale, double normalisedScaleFrom)
  {
    double rangeSize = rangeEnd - rangeStart;
    double normalisedValue = (value - rangeStart) / rangeSize;
    double normalisedValueDistance = normalisedScaleFrom - normalisedValue;
    double normalisedNewValueDistance = normalisedValueDistance * scale;
    return rangeStart + (rangeSize * (normalisedScaleFrom - normalisedNewValueDistance));
  }

  private static int dpToPx(Context context, float dp)
  {
    return (int)(dp * context.getResources().getDisplayMetrics().density);
  }

  private static float pxToDp(Context context, float px)
  {
    return px / context.getResources().getDisplayMetrics().density;
  }

  private static boolean getRangeIntersection(float startX, float startY, float endX, float endY, float rangeY, PointF point)
  {
    boolean intersected = false;
    if (startY != rangeY && endY != rangeY)
    {
      float a1 = endY - startY;
      float b1 = startX - endX;
      float c1 = a1 * (startX) + b1 * (startY);

      float a2 = 0.0f;
      float b2 = startX - endX;
      float c2 = a2 * (startX) + b2 * (rangeY);

      float determinant = a1 * b2 - a2 * b1;

      if (determinant != 0)
      {
        point.x = (b2 * c1 - b1 * c2) / determinant;
        point.y = (a1 * c2 - a2 * c1) / determinant;
        intersected = (point.x <= Math.max(startX, endX) && point.x >= Math.min(startX, endX) &&
            point.y <= Math.max(startY, endY) && point.y >= Math.min(startY, endY));
      }
    }
    return intersected;
  }

  public static class TimeAxisLabelData
  {
    long timestamp;
    String label;

    public TimeAxisLabelData(long timestamp, String label)
    {
      this.timestamp = timestamp;
      this.label = label;
    }

    public static TimeAxisLabelData[] labelDays(Data[] data)
    {
      ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
      long lastDay = Long.MIN_VALUE;
      Calendar calendar = Calendar.getInstance();

      for (Data datum : data)
      {
        calendar.setTimeInMillis(datum.timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
        calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
        calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
        calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

        if (calendar.getTimeInMillis() > lastDay)
        {
          lastDay = calendar.getTimeInMillis();
          Date date = calendar.getTime();
          DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
          timeAxisLabelData.add(new TimeAxisLabelData(lastDay, dateFormat.format(date)));
        }
      }

      return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
    }
  }

  private static class TimeAxisLabel
  {
    float offset;
    TextView view;
    Line marker;

    TimeAxisLabel(TextView view)
    {
      this.offset = 0.0f;
      this.view = view;
      this.marker = null;
    }

    TimeAxisLabel(float offset, TextView view, Line marker)
    {
      this.offset = offset;
      this.view = view;
      this.marker = marker;
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

  public interface DataAccessor
  {
    Data[] getData(long startTimestamp, long endTimestamp, long visibleStartTimestamp, long visibleEndTimestamp);
    TimeAxisLabelData[] getLabelsForData(Data[] data);
  }
}
