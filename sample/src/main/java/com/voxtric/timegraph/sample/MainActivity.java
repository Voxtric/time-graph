package com.voxtric.timegraph.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;

import com.voxtric.timegraph.GraphData;
import com.voxtric.timegraph.GraphDataProvider;
import com.voxtric.timegraph.TimeAxisLabelData;
import com.voxtric.timegraph.TimeGraph;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements GraphDataProvider
{
  private TimeGraph m_timeGraph = null;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    m_timeGraph = findViewById(R.id.time_graph);
    if (savedInstanceState == null)
    {
      final int red = Color.rgb(222, 0, 0);
      final int green = Color.rgb(0, 181, 24);
      final int yellow = Color.rgb(242, 250, 0);

      m_timeGraph.setValueAxisMidLabels(new float[]{ 4.0f, 8.0f, 12.0f });
      m_timeGraph.setRangeHighlights(new float[] { 0.0f, 4.0f, 8.0f, 12.0f, 16.0f },
                                     new int[] { red, green, yellow, red },
                                     TimeGraph.DISPLAY_MODE_UNDERLINE_WITH_FADE,
                                     true);
      m_timeGraph.setVisibleDataPeriod(0, 86400000L, MainActivity.this, true);

      m_timeGraph.setOnDataPointClickedListener(new TimeGraph.OnDataPointClickedListener()
      {
        @Override
        public void onDataPointClicked(TimeGraph graph, long timestamp, float value)
        {
          Log.e(String.valueOf(timestamp), String.valueOf(value));
        }
      });

      m_timeGraph.postDelayed(new Runnable()
      {
        @Override
        public void run()
        {
        }
      }, 2000);
    }
  }

  @Override
  public void onResume()
  {
    super.onResume();
    m_timeGraph.refresh(this, true);
  }

  @Override
  public GraphData[] getData(long startTimestamp, long endTimestamp, long visibleStartTimestamp, long visibleEndTimestamp)
  {
    ArrayList<GraphData> data = new ArrayList<>();

    Random random = new Random(2);
    long timestamp = startTimestamp;
    while (timestamp < endTimestamp)
    {
      data.add(new GraphData(timestamp, random.nextFloat() * 16.0f));
      timestamp += 86400000L / 8;
    }

    return data.toArray(new GraphData[0]);
  }

  @Override
  public TimeAxisLabelData[] getLabelsForData(GraphData[] data)
  {
    return TimeAxisLabelData.autoLabel(data);
  }
}
