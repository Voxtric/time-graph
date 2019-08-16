package com.voxtric.timegraph.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.voxtric.timegraph.TimeGraph;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements TimeGraph.DataAccessor
{
  TimeGraph.Data[] m_testData = new TimeGraph.Data[10];
  TimeGraph m_timeGraph = null;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    Random random = new Random();
    for (int i = 0; i < m_testData.length; i++)
    {
      m_testData[i] = new TimeGraph.Data((i * 8640000) + (Math.abs(random.nextInt()) % 8600000), random.nextFloat() * 16.0f);
    }



    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    m_timeGraph = findViewById(R.id.time_graph);

    m_timeGraph.setMidValueAxisLabels(new float[] { 4.0f, 8.0f, 12.0f });
    m_timeGraph.setVisibleDataPeriod(0, 432000000, this);
  }

  private TimeGraph.Data[] getData(long startTimestamp, long endTimestamp, long visibleStartTimestamp, long visibleEndTimestamp, int recursionDepth)
  {
    if (recursionDepth > 1)
    {
      return m_testData;
    }
    else if (visibleStartTimestamp < m_testData[0].timestamp)
    {
      long difference = m_testData[0].timestamp - visibleStartTimestamp;
      return getData(startTimestamp + difference, endTimestamp + difference,
                     visibleStartTimestamp + difference, visibleEndTimestamp + difference,
                     recursionDepth + 1);
    }
    else if (visibleEndTimestamp > m_testData[m_testData.length - 1].timestamp)
    {
      long difference = visibleEndTimestamp - m_testData[m_testData.length - 1].timestamp;
      return getData(startTimestamp - difference, endTimestamp - difference,
                     visibleStartTimestamp - difference, visibleEndTimestamp - difference,
                     recursionDepth + 1);
    }
    else
    {
      return m_testData;
    }
  }

  @Override
  public TimeGraph.Data[] getData(long startTimestamp, long endTimestamp, long visibleStartTimestamp, long visibleEndTimestamp)
  {
    return getData(startTimestamp, endTimestamp, visibleStartTimestamp, visibleEndTimestamp, 0);
  }

  @Override
  public TimeGraph.TimeLabel[] getLabelsForData(TimeGraph.Data[] data)
  {
    return TimeGraph.TimeLabel.labelDays(data);
  }
}
