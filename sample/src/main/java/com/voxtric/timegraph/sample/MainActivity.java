package com.voxtric.timegraph.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;

import com.voxtric.timegraph.GraphData;
import com.voxtric.timegraph.GraphDataProvider;
import com.voxtric.timegraph.TimeAxisLabelData;
import com.voxtric.timegraph.TimeGraph;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity implements GraphDataProvider
{
  private TimeGraph m_timeGraph = null;
  private boolean m_dataGiven = false;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    m_timeGraph = findViewById(R.id.time_graph);
    if (savedInstanceState == null)
    {
      int red = Color.rgb(222, 0, 0);
      int green = Color.rgb(0, 181, 24);
      int yellow = Color.rgb(242, 250, 0);

      m_timeGraph.setValueAxisMidLabels(new float[]{ 4.0f, 8.0f, 12.0f });
      m_timeGraph.setRangeHighlights(new float[] { 0.0f, 4.0f, 8.0f, 12.0f, 16.0f },
                                     new int[] { red, green, yellow, red },
                                     TimeGraph.DISPLAY_MODE_UNDERLINE_WITH_FADE,
                                     true);
      m_timeGraph.setVisibleDataPeriod(0, 86400000L, MainActivity.this, true);

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
    if (!m_dataGiven)
    {
      m_dataGiven = true;
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
    else
    {
      return new GraphData[0];
    }
  }

  @Override
  public TimeAxisLabelData[] getLabelsForData(GraphData[] data)
  {
    return TimeAxisLabelData.autoLabel(data);
  }
}
