package com.voxtric.timegraph;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.AsyncTask;
import android.util.AttributeSet;
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
  private static final float DEFAULT_MIN_VALUE = 0.0f;
  private static final float DEFAULT_MAX_VALUE = 100.0f;
  private static final boolean DEFAULT_SHOW_TIME_AXIS = true;
  private static final boolean DEFAULT_SHOW_VALUE_AXIS = true;
  private static final boolean DEFAULT_SHOW_REFRESH_PROGRESS = true;
  private static final boolean DEFAULT_SHOW_NO_DATA_TEXT = true;
  private static final String DEFAULT_NO_DATA_TEXT = "No Data To Display";
  private static final float DEFAULT_OVER_SCROLL = 0.0f;

  private static final float VALUE_AXIS_MARGIN_DP = 4.0f;

  private float m_minValue = DEFAULT_MIN_VALUE;
  private float m_maxValue = DEFAULT_MAX_VALUE;
  private boolean m_showTimeAxis = DEFAULT_SHOW_TIME_AXIS;
  private boolean m_showValueAxis = DEFAULT_SHOW_VALUE_AXIS;
  private boolean m_showRefreshProgress = DEFAULT_SHOW_REFRESH_PROGRESS;
  private boolean m_showNoDataText = DEFAULT_SHOW_NO_DATA_TEXT;
  private CharSequence m_noDataText = DEFAULT_NO_DATA_TEXT;
  private float m_overScroll = DEFAULT_OVER_SCROLL;

  private long m_startTimestamp = 0L;
  private long m_endTimestamp = 0L;
  private long m_beforeScalingStartTimestamp = Long.MIN_VALUE;
  private long m_beforeScalingEndTimestamp = Long.MAX_VALUE;
  private DataAccessor m_dataAccessor = null;

  private boolean m_refreshing = false;
  private ProgressBar m_refreshProgressView = null;
  private TextView m_noDataView = null;

  private GraphSurface m_graphSurfaceView = null;
  private float m_xOffset = 0.0f;
  private float m_minXOffset = 0.0f;
  private float m_maxXOffset = 0.0f;
  private float m_xScale = 1.0f;
  private float m_normalisedForcedXCentre = -1.0f;
  private LineStrip m_dataLine = null;

  private Data m_firstDataEntry = null;
  private Data m_lastDataEntry = null;

  private RelativeLayout m_timeLabelsLayoutView = null;
  private ArrayList<TextView> m_midValueViews = null;

  private TextView m_minValueView = null;
  private TextView m_maxValueView = null;
  private ArrayList<TimeAxisLabel> m_timeAxisLabels = null;

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

  public void setShowTimeAxis(boolean value)
  {
    m_showTimeAxis = value;
    m_timeLabelsLayoutView.setVisibility(value ? View.VISIBLE : View.GONE);
  }

  public boolean getShowTimeAxis()
  {
    return m_showTimeAxis;
  }

  public void setShowValueAxis(boolean value)
  {
    m_showValueAxis = value;
    int visibility = value ? View.VISIBLE : View.GONE;

    m_minValueView.setVisibility(visibility);
    m_maxValueView.setVisibility(visibility);
    for (TextView view : m_midValueViews)
    {
      view.setVisibility(visibility);
    }

    ConstraintSet constraintSet = new ConstraintSet();
    constraintSet.clone(this);
    if (value)
    {
      constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.LEFT,
                            m_maxValueView.getId(), ConstraintSet.RIGHT,
                            (int)dpToPx(getContext(), VALUE_AXIS_MARGIN_DP));
    }
    else
    {
      constraintSet.connect(m_graphSurfaceView.getId(), ConstraintSet.LEFT,
                            m_maxValueView.getId(), ConstraintSet.RIGHT, 0);
    }
    constraintSet.applyTo(this);
  }

  public boolean getShowValueAxis()
  {
    return m_showValueAxis;
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

  public void setOverScroll(float value)
  {
    m_overScroll = value;
  }

  public float getOverScroll()
  {
    return m_overScroll;
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
          if (m_timeAxisLabels == null)
          {
            m_timeAxisLabels = new ArrayList<>(timeAxisLabelData.length);
          }

          double difference = (double)(m_endTimestamp - m_startTimestamp);
          int index = 0;
          for (; index < timeAxisLabelData.length; index++)
          {

            TimeAxisLabel timeAxisLabel;
            if (index >= m_timeAxisLabels.size())
            {
              timeAxisLabel = new TimeAxisLabel(createTextView(getContext()));
              timeAxisLabel.view.setBackgroundResource(R.drawable.label_border);
              timeAxisLabel.view.setPadding((int)dpToPx(getContext(), 3), 0, 0, 0);
              m_timeLabelsLayoutView.addView(timeAxisLabel.view);
              m_timeAxisLabels.add(timeAxisLabel);
            }
            else
            {
              timeAxisLabel = m_timeAxisLabels.get(index);
              if (timeAxisLabel == null)
              {
                timeAxisLabel = new TimeAxisLabel(createTextView(getContext()));
                timeAxisLabel.view.setBackgroundResource(R.drawable.label_border);
                timeAxisLabel.view.setPadding((int)dpToPx(getContext(), 3), 0, 0, 0);
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
            float markerX = (((offset / m_graphSurfaceView.getWidth()) * 2.0f) - 1.0f) + 0.003f;
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

  public void setMidValueAxisLabels(final float[] midValues)
  {
    if (midValues != null)
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
          addView(textView);
          m_midValueViews.add(textView);
        }
        else
        {
          textView = m_midValueViews.get(index);
          if (textView == null)
          {
            textView = createTextView(getContext());
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
  }

  public void setVisibleDataPeriod(long startTimestamp, long endTimestamp, @NonNull final DataAccessor dataAccessor)
  {
    m_startTimestamp = startTimestamp;
    m_endTimestamp = endTimestamp;
    m_dataAccessor = dataAccessor;

    refresh();
  }

  public void clearData()
  {
    m_startTimestamp = 0L;
    m_endTimestamp = 0L;
    m_dataAccessor = null;

    refresh();
  }

  public void refresh()
  {
    m_normalisedForcedXCentre = -1.0f;
    final long timeDifference = m_endTimestamp - m_startTimestamp;
    if (timeDifference > 0L)
    {
      createNewDataLineStrip(timeDifference);
    }
    else
    {
      clearDataLineStrip();
    }
  }

  private void createNewDataLineStrip(final long timeDifference)
  {
    m_refreshing = true;
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
        float floatTimeDifference = (float)timeDifference;
        float valueDifference = m_maxValue - m_minValue;
        if (m_dataLine != null)
        {
          m_graphSurfaceView.removeRenderable(m_dataLine);
        }

        Data[] data = null;
        if (m_dataAccessor != null)
        {
          data = m_dataAccessor.getData(m_startTimestamp - timeDifference,
                                               m_endTimestamp + timeDifference,
                                               m_startTimestamp,
                                               m_endTimestamp);
          if (data != null && data.length > 0)
          {
            m_firstDataEntry = data[0];
            m_lastDataEntry = data[data.length - 1];
            setTimeAxisLabels(m_dataAccessor.getLabelsForData(data));

            float[] coords = new float[data.length * Renderable.COORDS_PER_VERTEX];
            int coordsIndex = 0;
            for (Data datum : data)
            {
              float xCoord = 1.0f - (m_endTimestamp - datum.timestamp) / floatTimeDifference;
              float yCoord = (m_maxValue - datum.value) / valueDifference;
              coords[coordsIndex] = (xCoord * 2.0f) - 1.0f;
              coords[coordsIndex + 1] = (yCoord * 2.0f) - 1.0f;
              coordsIndex += 2;
            }

            m_xOffset = 0.0f;
            m_minXOffset = ((data[0].timestamp - m_startTimestamp - timeDifference) / -floatTimeDifference) - 1.0f + m_overScroll;
            m_maxXOffset = ((m_endTimestamp + timeDifference - data[data.length - 1].timestamp) / floatTimeDifference) - 1.0f - m_overScroll;
            m_xScale = 1.0f;
            m_beforeScalingStartTimestamp = Long.MIN_VALUE;
            m_beforeScalingEndTimestamp = Long.MAX_VALUE;

            m_dataLine = m_graphSurfaceView.addLineStrip(coords);
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
      }
    });
  }

  private void clearDataLineStrip()
  {
    if (m_dataLine != null)
    {
      m_graphSurfaceView.removeRenderable(m_dataLine);
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

  public void scrollData(float normalisedScrollDelta)
  {
    if (normalisedScrollDelta > 0.0f)
    {
      normalisedScrollDelta = Math.min(normalisedScrollDelta, m_minXOffset - m_xOffset);
    }
    else
    {
      normalisedScrollDelta = -Math.min(Math.abs(normalisedScrollDelta), m_xOffset - m_maxXOffset);
    }

    m_xOffset += normalisedScrollDelta;

    long timeChange = (long)((m_endTimestamp - m_startTimestamp) * normalisedScrollDelta);
    m_startTimestamp -= timeChange;
    m_endTimestamp -= timeChange;

    float pixelMove = normalisedScrollDelta * m_graphSurfaceView.getWidth();
    for (TimeAxisLabel label : m_timeAxisLabels)
    {
      label.view.animate().translationXBy(pixelMove).setDuration(0).start();
      label.offset += pixelMove;
    }

    if (m_dataLine != null)
    {
      m_dataLine.setXOffset(m_xOffset * 2.0f);
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

  public void scaleData(float normalisedScaleDelta, float normalisedXCentre)
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
    m_startTimestamp = (long)scaleValue(m_beforeScalingStartTimestamp, m_beforeScalingEndTimestamp, m_beforeScalingStartTimestamp, timingScale, normalisedXCentre);
    m_endTimestamp = (long)scaleValue(m_beforeScalingStartTimestamp, m_beforeScalingEndTimestamp, m_beforeScalingEndTimestamp, timingScale, normalisedXCentre);

    long startToFirstDifference = m_firstDataEntry.timestamp - m_startTimestamp;
    long endToLastDifference = m_endTimestamp - m_lastDataEntry.timestamp;
    if (startToFirstDifference > 0 && endToLastDifference <= 0)
    {
      m_startTimestamp += startToFirstDifference;
      m_endTimestamp += startToFirstDifference;
      refresh();
      m_normalisedForcedXCentre = 0.0f;
    }
    else if (endToLastDifference > 0 && startToFirstDifference <= 0)
    {
      m_startTimestamp -= endToLastDifference;
      m_endTimestamp -= endToLastDifference;
      refresh();
      m_normalisedForcedXCentre = 1.0f;
    }

    for (TimeAxisLabel label : m_timeAxisLabels)
    {
      float labelPosition = (float)scaleValue(0.0, m_graphSurfaceView.getWidth(), label.offset, m_xScale, normalisedXCentre);
      label.view.animate().translationX(labelPosition).setDuration(0).start();
    }

    if (m_dataLine != null)
    {
      m_dataLine.setXScale(m_xScale, (normalisedXCentre * 2.0f) - 1.0f);
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

  private boolean shouldApplyDataBoundaries()
  {
    long startToFirstDifference = m_firstDataEntry.timestamp - m_startTimestamp;
    long endToLastDifference = m_endTimestamp - m_lastDataEntry.timestamp;
    long firstToLastDistance = m_lastDataEntry.timestamp - m_firstDataEntry.timestamp;
    return (((startToFirstDifference <= 0 && endToLastDifference > 0) || (startToFirstDifference > 0 && endToLastDifference <= 0)) &&
        (startToFirstDifference < firstToLastDistance && endToLastDifference < firstToLastDistance));
  }

  private void applyAttributes(Context context, AttributeSet attrs)
  {
    TypedArray attributes = context.getTheme().obtainStyledAttributes(attrs, R.styleable.TimeGraph, 0, 0);
    try
    {
      m_minValue = attributes.getFloat(R.styleable.TimeGraph_minValue, DEFAULT_MIN_VALUE);
      m_maxValue = attributes.getFloat(R.styleable.TimeGraph_maxValue, DEFAULT_MAX_VALUE);
      m_showTimeAxis = attributes.getBoolean(R.styleable.TimeGraph_showTimeAxis, DEFAULT_SHOW_TIME_AXIS);
      m_showValueAxis = attributes.getBoolean(R.styleable.TimeGraph_showValueAxis, DEFAULT_SHOW_VALUE_AXIS);
      m_showRefreshProgress = attributes.getBoolean(R.styleable.TimeGraph_showRefreshProgress, DEFAULT_SHOW_REFRESH_PROGRESS);
      m_showNoDataText = attributes.getBoolean(R.styleable.TimeGraph_showNoDataText, DEFAULT_SHOW_NO_DATA_TEXT);
      m_noDataText = attributes.getText(R.styleable.TimeGraph_noDataText);
      if (m_noDataText == null)
      {
        m_noDataText = DEFAULT_NO_DATA_TEXT;
      }
      m_overScroll = attributes.getFraction(R.styleable.TimeGraph_overScroll, 1, 1, DEFAULT_OVER_SCROLL);

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
    m_graphSurfaceView = new GraphSurface(context);
    m_graphSurfaceView.setId(R.id.graph_surface);
    m_graphSurfaceView.initialise(this);
    addView(m_graphSurfaceView);

    m_timeLabelsLayoutView = new RelativeLayout(context);
    m_timeLabelsLayoutView.setId(R.id.time_labels_layout);
    addView(m_timeLabelsLayoutView);

    m_minValueView = new TextView(context);
    m_minValueView.setId(R.id.min_value);
    m_minValueView.setText(String.valueOf(m_minValue));
    addView(m_minValueView);

    m_maxValueView = new TextView(context);
    m_maxValueView.setId(R.id.max_value);
    m_maxValueView.setText(String.valueOf(m_maxValue));
    addView(m_maxValueView);

    m_refreshProgressView = new ProgressBar(context);
    m_refreshProgressView.setId(R.id.refresh_progress);
    addView(m_refreshProgressView);

    m_noDataView = new TextView(context);
    m_noDataView.setId(R.id.no_data);
    m_noDataView.setGravity(Gravity.CENTER);
    m_noDataView.setText(m_noDataText);
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
      m_minValueView.setVisibility(View.GONE);
      m_maxValueView.setVisibility(View.GONE);
    }
    m_refreshProgressView.setVisibility(View.INVISIBLE);
    if (!m_showNoDataText)
    {
      m_noDataView.setVisibility(View.INVISIBLE);
    }
  }

  private static TextView createTextView(Context context)
  {
    TextView textView = new TextView(context);
    textView.setId(View.generateViewId());
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

  private static float dpToPx(Context context, float dp)
  {
    return dp * context.getResources().getDisplayMetrics().density;
  }

  private static float pxToDp(Context context, float px)
  {
    return px / context.getResources().getDisplayMetrics().density;
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
