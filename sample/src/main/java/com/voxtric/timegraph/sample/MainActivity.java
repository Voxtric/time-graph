package com.voxtric.timegraph.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.voxtric.timegraph.TimeGraph;

import java.util.Random;

public class MainActivity extends AppCompatActivity
{
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    TimeGraph timeGraph = findViewById(R.id.time_graph);

    timeGraph.setMidValueAxisLabels(new float[] { 4.0f, 8.0f, 12.0f });

    Random random = new Random();
    TimeGraph.Data[] data = new TimeGraph.Data[500];
    for (int i = 0; i < data.length; i++)
    {
      data[i] = new TimeGraph.Data((i * 1000) + (random.nextInt() % 999), random.nextFloat() * 16.0f);
    }
    timeGraph.setFreshData(data, 10000, 110000);

    timeGraph.setTimeAxisLabels(new TimeGraph.TimeLabel[] {
          new TimeGraph.TimeLabel(0, "Test A")/*,
          new TimeGraph.TimeLabel(30000, "In it"),
          new TimeGraph.TimeLabel(90000, "Ending")*/
        });
  }
}
