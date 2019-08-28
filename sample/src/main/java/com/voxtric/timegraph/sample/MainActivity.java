package com.voxtric.timegraph.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;

import com.voxtric.timegraph.GraphData;
import com.voxtric.timegraph.GraphDataProvider;
import com.voxtric.timegraph.TimeAxisLabelData;
import com.voxtric.timegraph.TimeGraph;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements GraphDataProvider
{
  GraphData[] m_testData = new GraphData[250];
  TimeGraph m_timeGraph = null;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    Random random = new Random(2);
    for (int i = 0; i < m_testData.length; i++)
    {
      m_testData[i] = new GraphData((i * 86400000L)/* + (Math.abs(random.nextLong()) % 86000000L)*/, random.nextFloat() * 16.0f);
    }



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
      m_timeGraph.setVisibleDataPeriod(0, 86400000L * 5L, MainActivity.this, true);

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
    /*try
    {
      Thread.sleep(1000);
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }*/
    return m_testData;
  }

  @Override
  public TimeAxisLabelData[] getLabelsForData(GraphData[] data)
  {
    return TimeAxisLabelData.labelDays(data);
  }
}
