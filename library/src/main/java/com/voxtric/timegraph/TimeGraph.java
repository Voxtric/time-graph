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

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.voxtric.timegraph.opengl.GraphSurface;
import com.voxtric.timegraph.opengl.Line;
import com.voxtric.timegraph.opengl.LineStrip;
import com.voxtric.timegraph.opengl.Mesh;
import com.voxtric.timegraph.opengl.Renderable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({DISPLAY_MODE_BASIC, DISPLAY_MODE_UNDERLINE, DISPLAY_MODE_UNDERLINE_WITH_FADE})
  public @interface DisplayMode {}
  public static final int DISPLAY_MODE_BASIC = 0;
  public static final int DISPLAY_MODE_UNDERLINE = 1;
  public static final int DISPLAY_MODE_UNDERLINE_WITH_FADE = 2;

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
  private Mesh m_rangeHighlightMesh = null;
  private Line m_labelMarkersLine = null;
  private ValueAnimator m_newDataAnimator = null;

  private Data m_firstDataEntry = null;
  private Data m_lastDataEntry = null;

  private RelativeLayout m_timeAxisLabelsLayoutView = null;
  private ArrayList<TextView> m_valueAxisMidViews = new ArrayList<>();

  private TextView m_valueAxisMinView = null;
  private TextView m_valueAxisMaxView = null;
  private ArrayList<TimeAxisLabel> m_timeAxisLabels = new ArrayList<>();

  private float[] m_rangeHighlightingValues = null;
  private int[] m_rangeHighlightingColors = null;
  private @DisplayMode int m_rangeHighlightingDisplayMode = DISPLAY_MODE_BASIC;

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

    state.putFloatArray("m_rangeHighlightingValues", m_rangeHighlightingValues);
    state.putIntArray("m_rangeHighlightingColors", m_rangeHighlightingColors);
    state.putInt("m_rangeHighlightingDisplayMode", m_rangeHighlightingDisplayMode);

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
      setValueAxisMin(bundle.getFloat("m_valueAxisMin"), false);
      setValueAxisMax(bundle.getFloat("m_valueAxisMax"), false);

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

      setRangeHighlights(bundle.getFloatArray("m_rangeHighlightingValues"),
                         bundle.getIntArray("m_rangeHighlightingColors"),
                         bundle.getInt("m_rangeHighlightingDisplayMode"),
                         false);

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
    constraintSet.clear(m_graphSurfaceView.getId(), ConstraintSet.START);
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

    post(new Runnable()
    {
      @Override
      public void run()
      {
        for (TimeAxisLabel label : m_timeAxisLabels)
        {
          repositionTimeAxisLabel(label);
        }
      }
    });
  }

  public boolean getShowValueAxis()
  {
    return m_showValueAxis;
  }

  public void setValueAxisTextSizeSp(float textSizeSp)
  {
    m_valueAxisTextSizeSp = textSizeSp;

    m_valueAxisMinView.setTextSize(TypedValue.COMPLEX_UNIT_SP, m_valueAxisTextSizeSp);
    m_valueAxisMaxView.setTextSize(TypedValue.COMPLEX_UNIT_SP, m_valueAxisTextSizeSp);
    for (TextView textView : m_valueAxisMidViews)
    {
      textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, m_valueAxisTextSizeSp);
    }

    post(new Runnable()
    {
      @Override
      public void run()
      {
        resizeViewForValueAxisLabels();

        float offset = m_valueAxisMaxView.getHeight() * 0.5f;
        m_valueAxisMinView.animate().translationY(0.0f).setDuration(0L).start();
        m_valueAxisMinView.animate().translationYBy(offset).setDuration(0L).start();
        m_valueAxisMaxView.animate().translationY(0.0f).setDuration(0L).start();
        m_valueAxisMaxView.animate().translationYBy(-offset).setDuration(0L).start();

        post(new Runnable()
        {
          @Override
          public void run()
          {
            for (TextView textView : m_valueAxisMidViews)
            {
              repositionValueAxisLabel(textView);
            }

            for (TimeAxisLabel label : m_timeAxisLabels)
            {
              repositionTimeAxisLabel(label);
            }
          }
        });
      }
    });
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

  public void setValueAxisMinMax(float min, float max, boolean animate)
  {
    if (min <= max)
    {
      throw new IllegalArgumentException("Minimum value must be lower than maximum value.");
    }

    m_valueAxisMin = min;
    m_valueAxisMinView.setText(String.valueOf(min));
    setValueAxisMax(max, animate);
  }

  public void setValueAxisMin(float value, boolean animate)
  {
    if (value >= m_valueAxisMax)
    {
      throw new IllegalArgumentException("Minimum value must be lower than maximum value.");
    }

    m_valueAxisMin = value;
    m_valueAxisMinView.setText(String.valueOf(value));

    refresh(animate);
  }

  public float getValueAxisMin()
  {
    return m_valueAxisMin;
  }

  public void setValueAxisMax(float value, boolean animate)
  {
    if (value <= m_valueAxisMin)
    {
      throw new IllegalArgumentException("Maximum value must be greater than minimum value.");
    }

    m_valueAxisMax = value;
    m_valueAxisMaxView.setText(String.valueOf(value));

    refresh(animate);
  }

  public float getValueAxisMax()
  {
    return m_valueAxisMax;
  }

  public void setShowTimeAxis(boolean value)
  {
    m_showTimeAxis = value;
    m_timeAxisLabelsLayoutView.setVisibility(value ? View.VISIBLE : View.GONE);

    post(new Runnable()
    {
      @Override
      public void run()
      {
        for (TextView textView : m_valueAxisMidViews)
        {
          repositionValueAxisLabel(textView);
        }
      }
    });
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

    post(new Runnable()
    {
      @Override
      public void run()
      {
        for (TextView textView : m_valueAxisMidViews)
        {
          repositionValueAxisLabel(textView);
        }
      }
    });
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
      post(new Runnable()
      {
        @Override
        public void run()
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
            repositionValueAxisLabel(textView);

            ConstraintSet constraintSet = new ConstraintSet();
            constraintSet.clone(TimeGraph.this);
            constraintSet.connect(textView.getId(), ConstraintSet.RIGHT, m_valueAxisMaxView.getId(), ConstraintSet.RIGHT);
            constraintSet.setHorizontalBias(textView.getId(), 1.0f);
            constraintSet.applyTo(TimeGraph.this);
          }

          for (int i = m_valueAxisMidViews.size() - 1; i >= index; i--)
          {
            removeView(m_valueAxisMidViews.get(i));
            m_valueAxisMidViews.remove(i);
          }
        }
      });
    }
  }

  private void repositionValueAxisLabel(TextView textView)
  {
    float value = Float.valueOf(textView.getText().toString());
    float height = ((m_valueAxisMax - value) / (m_valueAxisMax - m_valueAxisMin)) * m_graphSurfaceView.getHeight();
    textView.animate().translationY(0.0f).setDuration(0).start();
    textView.animate().translationYBy(height).setDuration(0).start();
  }

  private void resizeViewForValueAxisLabels()
  {
    int topMargin = (int)Math.ceil(m_valueAxisMaxView.getHeight() * 0.5f);
    int bottomMargin = Math.max(topMargin - m_timeAxisLabelsLayoutView.getHeight(), 0);

    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(TimeGraph.this);
    constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, topMargin);
    constraintSet.connect(m_timeAxisLabelsLayoutView.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, bottomMargin);
    constraintSet.applyTo(TimeGraph.this);
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
          final int initialTimeAxisHeight = m_timeAxisLabelsLayoutView.getHeight();

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
              m_timeAxisLabelsLayoutView.addView(timeAxisLabel.view);
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
                m_timeAxisLabelsLayoutView.addView(timeAxisLabel.view);
              }
            }

            timeAxisLabel.timestamp = timeAxisLabelData[index].timestamp;
            timeAxisLabel.view.setText(timeAxisLabelData[index].label);
            repositionTimeAxisLabel(timeAxisLabel);
          }

          int timeAxisLabelCount = m_timeAxisLabels.size();
          float[] labelMarkerCoords = new float[timeAxisLabelCount * 4];
          for (int i = 0; i < timeAxisLabelCount; i++)
          {
            int coordsIndex = i * 4;
            float markerX = (((m_timeAxisLabels.get(i).offset / m_graphSurfaceView.getWidth()) * 2.0f) - 1.0f) + 0.0015f;
            labelMarkerCoords[coordsIndex] = markerX;
            labelMarkerCoords[coordsIndex + 1] = -1.0f;
            labelMarkerCoords[coordsIndex + 2] = markerX;
            labelMarkerCoords[coordsIndex + 3] = -0.9f;
          }
          Line oldLabelMarkersLine = m_labelMarkersLine;
          m_labelMarkersLine = m_graphSurfaceView.addLine(1, labelMarkerCoords);
          if (oldLabelMarkersLine != null)
          {
            m_graphSurfaceView.removeRenderable(oldLabelMarkersLine);
          }

          post(new Runnable()
          {
            @Override
            public void run()
            {
              if (initialTimeAxisHeight != m_timeAxisLabelsLayoutView.getHeight())
              {
                resizeViewForValueAxisLabels();

                post(new Runnable()
                {
                  @Override
                  public void run()
                  {
                    for (TextView textView : m_valueAxisMidViews)
                    {
                      repositionValueAxisLabel(textView);
                    }
                  }
                });
              }
            }
          });
        }
      });
    }
  }

  private void repositionTimeAxisLabel(TimeAxisLabel label)
  {
    double difference = (double)(m_endTimestamp - m_startTimestamp);
    float widthMultiplier = 1.0f - (float)((double)(m_endTimestamp - label.timestamp) / difference);
    float offset = widthMultiplier * m_graphSurfaceView.getWidth();
    label.view.animate().translationX(0.0f).setDuration(0).start();
    label.view.animate().translationXBy(offset).setDuration(0).start();
    label.offset = offset;
  }

  public void setRangeHighlights(float[] upperBoundaries, int[] colors, @DisplayMode int displayMode, boolean animate)
  {
    m_rangeHighlightingValues = upperBoundaries;
    m_rangeHighlightingColors = colors;
    m_rangeHighlightingDisplayMode = displayMode;

    refresh(animate);
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
              if (m_rangeHighlightingValues != null && m_rangeHighlightingColors != null)
              {
                createHighlightMesh(data, floatTimeDifference, valueDifference);
              }

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
                    float animatedValue = (float)valueAnimator.getAnimatedValue();
                    if (m_dataLineStrip != null)
                    {
                      m_dataLineStrip.setYScale(animatedValue);
                    }
                    if (m_rangeHighlightMesh != null)
                    {
                      m_rangeHighlightMesh.setYScale(animatedValue);
                    }
                    m_graphSurfaceView.requestRender();
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
      m_dataLineStrip = null;
    }
    if (m_rangeHighlightMesh != null)
    {
      m_graphSurfaceView.removeRenderable(m_rangeHighlightMesh);
      m_rangeHighlightMesh = null;
    }
    if (m_labelMarkersLine != null)
    {
      m_graphSurfaceView.removeRenderable(m_labelMarkersLine);
      m_labelMarkersLine = null;
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
      float xCoord = (datum.timestamp - m_startTimestamp) / timeDifference;
      float yCoord = (datum.value - m_valueAxisMin) / valueDifference;
      coords[coordsIndex] = (xCoord * 2.0f) - 1.0f;
      coords[coordsIndex + 1] = (yCoord * 2.0f) - 1.0f;
      coordsIndex += 2;
    }
    LineStrip oldDataLine = m_dataLineStrip;
    m_dataLineStrip = m_graphSurfaceView.addLineStrip(1, coords);
    if (oldDataLine != null)
    {
      m_graphSurfaceView.removeRenderable(oldDataLine);
    }
  }

  private void createHighlightMesh(Data[] data, float timeDifference, float valueDifference)
  {
    switch (m_rangeHighlightingDisplayMode)
    {
    case DISPLAY_MODE_BASIC:
      createHighlightMeshBasic(data, timeDifference, valueDifference);
      break;
    case DISPLAY_MODE_UNDERLINE:
      createHighlightMeshUnderline(data, timeDifference, valueDifference);
      break;
    case DISPLAY_MODE_UNDERLINE_WITH_FADE:
      break;
    default:
      throw new IllegalStateException("Unknown range highlighting display mode value.");
    }
  }

  private void createHighlightMeshBasic(Data[] data, float timeDifference, float valueDifference)
  {

  }

  private void createHighlightMeshUnderline(Data[] data, float timeDifference, float valueDifference)
  {
    ArrayList<Float> coords = new ArrayList<>();
    ArrayList<Short> indices = new ArrayList<>();
    ArrayList<Float> colors = new ArrayList<>();
    short indexStart = 0;
    for (int i = 0; i < data.length - 1; i++)
    {
      Data start = data[i];
      float startXCoord = (start.timestamp - m_startTimestamp) / timeDifference;
      float startYCoord = (start.value - m_valueAxisMin) / valueDifference;
      Data end = data[i + 1];
      float endXCoord = (end.timestamp - m_startTimestamp) / timeDifference;
      float endYCoord = (end.value - m_valueAxisMin) / valueDifference;
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
      for (int j = 1; j < m_rangeHighlightingValues.length; j++)
      {
        float normalisedRangeValue = (m_rangeHighlightingValues[j] - m_valueAxisMin) / valueDifference;
        PointF intersection = new PointF();
        if (getRangeIntersection(startXCoord, startYCoord, endXCoord, endYCoord, normalisedRangeValue, intersection))
        {
          float r = Color.red(m_rangeHighlightingColors[j - 1]) / (float)Byte.MAX_VALUE;
          float g = Color.green(m_rangeHighlightingColors[j - 1]) / (float)Byte.MAX_VALUE;
          float b = Color.blue(m_rangeHighlightingColors[j - 1]) / (float)Byte.MAX_VALUE;

          // Intersect quad
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

          colors.add(r);
          colors.add(g);
          colors.add(b);
          colors.add(1.0f);
          colors.add(r);
          colors.add(g);
          colors.add(b);
          colors.add(1.0f);
          colors.add(r);
          colors.add(g);
          colors.add(b);
          colors.add(1.0f);
          colors.add(r);
          colors.add(g);
          colors.add(b);
          colors.add(1.0f);

          lastX = intersection.x;
          lastY = intersection.y;
        }
      }

      int colorIndex = -1;
      for (int j = 0; j < m_rangeHighlightingValues.length - 1 && colorIndex == -1; j++)
      {
        float normalisedRangeStart = (m_rangeHighlightingValues[j] - m_valueAxisMin) / valueDifference;
        float normalisedRangeEnd = (m_rangeHighlightingValues[j + 1] - m_valueAxisMin) / valueDifference;
        if (normalisedRangeEnd > endYCoord)
        {
          colorIndex = j;
        }
      }

      if (colorIndex != -1)
      {
        // Peak
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

        float r = Color.red(m_rangeHighlightingColors[colorIndex]) / (float)Byte.MAX_VALUE;
        float g = Color.green(m_rangeHighlightingColors[colorIndex]) / (float)Byte.MAX_VALUE;
        float b = Color.blue(m_rangeHighlightingColors[colorIndex]) / (float)Byte.MAX_VALUE;
        colors.add(r);
        colors.add(g);
        colors.add(b);
        colors.add(1.0f);
        colors.add(r);
        colors.add(g);
        colors.add(b);
        colors.add(1.0f);
        colors.add(r);
        colors.add(g);
        colors.add(b);
        colors.add(1.0f);
      }

      boolean finish = false;
      for (int j = 0; j < m_rangeHighlightingValues.length - 1 && !finish; j++)
      {
        float normalisedRangeStart = (m_rangeHighlightingValues[j] - m_valueAxisMin) / valueDifference;
        float normalisedRangeEnd = (m_rangeHighlightingValues[j + 1] - m_valueAxisMin) / valueDifference;
        if (normalisedRangeEnd > startYCoord)
        {
          normalisedRangeEnd = startYCoord;
          finish = true;
        }
        float r = Color.red(m_rangeHighlightingColors[j]) / (float)Byte.MAX_VALUE;
        float g = Color.green(m_rangeHighlightingColors[j]) / (float)Byte.MAX_VALUE;
        float b = Color.blue(m_rangeHighlightingColors[j]) / (float)Byte.MAX_VALUE;

        // Under quad
        coords.add(startXCoord);
        coords.add(normalisedRangeStart);
        coords.add(startXCoord);
        coords.add(normalisedRangeEnd);
        coords.add(endXCoord);
        coords.add(normalisedRangeEnd);
        coords.add(endXCoord);
        coords.add(normalisedRangeStart);

        indices.add(indexStart);
        indices.add((short)(indexStart + 1));
        indices.add((short)(indexStart + 2));
        indices.add(indexStart);
        indices.add((short)(indexStart + 2));
        indices.add((short)(indexStart + 3));
        indexStart += 4;

        colors.add(r);
        colors.add(g);
        colors.add(b);
        colors.add(1.0f);
        colors.add(r);
        colors.add(g);
        colors.add(b);
        colors.add(1.0f);
        colors.add(r);
        colors.add(g);
        colors.add(b);
        colors.add(1.0f);
        colors.add(r);
        colors.add(g);
        colors.add(b);
        colors.add(1.0f);
      }
    }

    float[] coordArray = new float[coords.size()];
    for (int i = 0; i < coordArray.length; i++)
    {
      coordArray[i] = (coords.get(i) * 2.0f) - 1.0f;
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
    Mesh oldDataMesh = m_rangeHighlightMesh;
    m_rangeHighlightMesh = m_graphSurfaceView.addMesh(0, coordArray, indexArray, colorArray);
    if (oldDataMesh != null)
    {
      m_graphSurfaceView.removeRenderable(oldDataMesh);
    }
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

      float openGlXOffset = m_xOffset * 2.0f;
      if (m_dataLineStrip != null)
      {
        m_dataLineStrip.setXOffset(openGlXOffset);
      }
      if (m_rangeHighlightMesh != null)
      {
        m_rangeHighlightMesh.setXOffset(openGlXOffset);
      }
      if (m_labelMarkersLine != null)
      {
        m_labelMarkersLine.setXOffset(openGlXOffset);
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

        float openGlXScalePosition = (normalisedXCentre * 2.0f) - 1.0f;
        if (m_dataLineStrip != null)
        {
          m_dataLineStrip.setXScale(m_xScale, openGlXScalePosition);
        }
        if (m_rangeHighlightMesh != null)
        {
          m_rangeHighlightMesh.setXScale(m_xScale, openGlXScalePosition);
        }
        if (m_labelMarkersLine != null)
        {
          m_labelMarkersLine.setXScale(m_xScale, openGlXScalePosition);
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
      m_valueAxisTextSizeSp = attributes.getFloat(R.styleable.TimeGraph_valueAxis_textSizeSp, DEFAULT_VALUE_AXIS_TEXT_SIZE_SP);
      m_valueAxisTextColor = attributes.getColor(R.styleable.TimeGraph_valueAxis_textColor, DEFAULT_VALUE_AXIS_TEXT_COLOR);
      m_valueAxisMin = attributes.getFloat(R.styleable.TimeGraph_valueAxis_min, DEFAULT_VALUE_AXIS_MIN);
      m_valueAxisMax = attributes.getFloat(R.styleable.TimeGraph_valueAxis_max, DEFAULT_VALUE_AXIS_MAX);

      m_showTimeAxis = attributes.getBoolean(R.styleable.TimeGraph_showTimeAxis, DEFAULT_SHOW_TIME_AXIS);
      m_timeAxisTextSizeSp = attributes.getFloat(R.styleable.TimeGraph_timeAxis_textSizeSp, DEFAULT_TIME_AXIS_TEXT_SIZE_SP);
      m_timeAxisTextColor = attributes.getColor(R.styleable.TimeGraph_timeAxis_textColor, DEFAULT_TIME_AXIS_TEXT_COLOR);

      m_showNoDataText = attributes.getBoolean(R.styleable.TimeGraph_showNoDataText, DEFAULT_SHOW_NO_DATA_TEXT);
      m_noDataText = attributes.getText(R.styleable.TimeGraph_noData_text);
      if (m_noDataText == null)
      {
        m_noDataText = DEFAULT_NO_DATA_TEXT;
      }
      m_noDataTextSizeSp = attributes.getFloat(R.styleable.TimeGraph_noData_textSizeSp, DEFAULT_NO_DATA_TEXT_SIZE_SP);
      m_noDataTextColor = attributes.getColor(R.styleable.TimeGraph_noData_textColor, DEFAULT_NO_DATA_TEXT_COLOR);

      m_showRefreshProgress = attributes.getBoolean(R.styleable.TimeGraph_showRefreshProgress, DEFAULT_SHOW_REFRESH_PROGRESS);

      m_allowScroll = attributes.getBoolean(R.styleable.TimeGraph_allowScroll, DEFAULT_ALLOW_SCROLL);
      m_allowScale = attributes.getBoolean(R.styleable.TimeGraph_allowScale, DEFAULT_ALLOW_SCALE);

      if (m_valueAxisMin >= m_valueAxisMax)
      {
        throw new IllegalArgumentException("Minimum value must be lower than maximum value.");
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

    m_timeAxisLabelsLayoutView = new RelativeLayout(context);
    m_timeAxisLabelsLayoutView.setId(R.id.time_labels_layout);
    addView(m_timeAxisLabelsLayoutView);

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
    if (!m_showValueAxis)
    {
      constraintSet.clear(m_graphSurfaceView.getId(), ConstraintSet.START);
      constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.LEFT,
                            m_valueAxisMaxView.getId(), ConstraintSet.RIGHT, 0);
    }
    constraintSet.applyTo(this);

    if (!m_showValueAxis)
    {
      m_valueAxisMinView.setVisibility(View.GONE);
      m_valueAxisMaxView.setVisibility(View.GONE);
    }
    if (!m_showTimeAxis)
    {
      m_timeAxisLabelsLayoutView.setVisibility(View.GONE);
    }
    m_refreshProgressView.setVisibility(View.INVISIBLE);
    if (!m_showNoDataText)
    {
      m_noDataView.setVisibility(View.INVISIBLE);
    }

    post(new Runnable()
    {
      @Override
      public void run()
      {
        float offset = m_valueAxisMaxView.getHeight() * 0.5f;
        m_valueAxisMaxView.animate().translationYBy(-offset).setDuration(0).start();
        m_valueAxisMinView.animate().translationYBy(offset).setDuration(0).start();

        resizeViewForValueAxisLabels();
      }
    });
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

  private static int spToPx(Context context, float sp)
  {
    return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
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
    long timestamp;
    float offset;
    TextView view;

    TimeAxisLabel(TextView view)
    {
      this.timestamp = 0L;
      this.offset = 0.0f;
      this.view = view;
    }

    TimeAxisLabel(long timestamp, float offset, TextView view)
    {
      this.timestamp = timestamp;
      this.offset = offset;
      this.view = view;
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
