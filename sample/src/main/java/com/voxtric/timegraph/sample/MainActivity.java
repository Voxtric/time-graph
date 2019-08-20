package com.voxtric.timegraph.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.voxtric.timegraph.TimeGraph;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements TimeGraph.DataAccessor
{
  TimeGraph.Data[] m_testData = new TimeGraph.Data[1000];
  TimeGraph m_timeGraph = null;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    Random random = new Random();
    for (int i = 0; i < m_testData.length; i++)
    {
      m_testData[i] = new TimeGraph.Data((i * 100L) + (Math.abs(random.nextLong()) % 99L), random.nextFloat() * 16.0f);
    }



    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    m_timeGraph = findViewById(R.id.time_graph);

    m_timeGraph.setMidValueAxisLabels(new float[] { 4.0f, 8.0f, 12.0f });
    m_timeGraph.setVisibleDataPeriod(0, 1000, MainActivity.this);
  }

  @Override
  public TimeGraph.Data[] getData(long startTimestamp, long endTimestamp, long visibleStartTimestamp, long visibleEndTimestamp)
  {
    return m_testData;
  }

  @Override
  public TimeGraph.TimeAxisLabelData[] getLabelsForData(TimeGraph.Data[] data)
  {
    return new TimeGraph.TimeAxisLabelData[] {
        new TimeGraph.TimeAxisLabelData(0, "A"),
        new TimeGraph.TimeAxisLabelData(200, "B"),
        new TimeGraph.TimeAxisLabelData(400, "C"),
        new TimeGraph.TimeAxisLabelData(600, "D"),
        new TimeGraph.TimeAxisLabelData(800, "E"),
        new TimeGraph.TimeAxisLabelData(1000, "F"),
        new TimeGraph.TimeAxisLabelData(1200, "G"),
        new TimeGraph.TimeAxisLabelData(1400, "H"),
        new TimeGraph.TimeAxisLabelData(1600, "I"),
    };
  }
}
