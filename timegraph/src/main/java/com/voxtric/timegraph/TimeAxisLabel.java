package com.voxtric.timegraph;

import android.widget.TextView;

class TimeAxisLabel
{
  long timestamp;
  float offset;
  TextView view;
  boolean anchor = false;

  TimeAxisLabel(TextView view)
  {
    this.timestamp = 0L;
    this.offset = 0.0f;
    this.view = view;
  }

  TimeAxisLabel(long timestamp, float offset, TextView view)
  {
    this.timestamp = timestamp;
    this.offset = offset;
    this.view = view;
  }
}
