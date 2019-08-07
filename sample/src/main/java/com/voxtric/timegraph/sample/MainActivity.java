package com.voxtric.timegraph.sample;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import com.voxtric.timegraph.TimeGraph;

public class MainActivity extends AppCompatActivity
{

  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    TimeGraph timeGraph = findViewById(R.id.time_graph);
    timeGraph.setMidValues(new float[] { 2.3f, 4.6f, 8.0f });
  }
}
