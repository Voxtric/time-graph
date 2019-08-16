package com.voxtric.timegraph.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.voxtric.timegraph.TimeGraph;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements TimeGraph.DataAccessor
{
  TimeGraph.Data[] m_testData = new TimeGraph.Data[100];

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

    final TimeGraph timeGraph = findViewById(R.id.time_graph);

    timeGraph.setMidValueAxisLabels(new float[] { 4.0f, 8.0f, 12.0f });
    timeGraph.setVisibleDataPeriod(0, 432000000, this);
  }

  @Override
  public TimeGraph.Data[] getData(long startTimestamp, long endTimestamp)
  {
    /*try
    {
      Thread.sleep(500);
    }
    catch (InterruptedException e)
    {
      e.printStackTrace();
    }*/
    return m_testData;
  }

  @Override
  public TimeGraph.TimeLabel[] getLabelsForData(TimeGraph.Data[] data)
  {
    return TimeGraph.TimeLabel.labelDays(data);
  }
}
