package com.voxtric.timegraph.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.voxtric.timegraph.TimeGraph;

import java.util.Random;

public class MainActivity extends AppCompatActivity implements TimeGraph.DataAccessor
{
  TimeGraph.Data[] m_testData = new TimeGraph.Data[18];
  TimeGraph m_timeGraph = null;

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    Random random = new Random();
    for (int i = 0; i < m_testData.length; i++)
    {
      m_testData[i] = new TimeGraph.Data((i * 100L) /*+ (Math.abs(random.nextLong()) % 99L)*/, random.nextFloat() * 16.0f);
    }



    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    m_timeGraph = findViewById(R.id.time_graph);

    m_timeGraph.setValueAxisMidLabels(new float[] { 4.0f, 8.0f, 12.0f });
    m_timeGraph.setVisibleDataPeriod(-500, 500, MainActivity.this);

    m_timeGraph.postDelayed(new Runnable()
    {
      @Override
      public void run()
      {
      }
    }, 1000);
  }

  @Override
  public TimeGraph.Data[] getData(long startTimestamp, long endTimestamp, long visibleStartTimestamp, long visibleEndTimestamp)
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
  public TimeGraph.TimeAxisLabelData[] getLabelsForData(TimeGraph.Data[] data)
  {
    return new TimeGraph.TimeAxisLabelData[] {
        new TimeGraph.TimeAxisLabelData(0, "1"),
        new TimeGraph.TimeAxisLabelData(200, "2"),
        new TimeGraph.TimeAxisLabelData(400, "3"),
        new TimeGraph.TimeAxisLabelData(600, "4"),
        new TimeGraph.TimeAxisLabelData(800, "5"),
        new TimeGraph.TimeAxisLabelData(1000, "6"),
        new TimeGraph.TimeAxisLabelData(1200, "7"),
        new TimeGraph.TimeAxisLabelData(1400, "8"),
        new TimeGraph.TimeAxisLabelData(1600, "9"),
    };
  }
}
