package com.voxtric.timegraph;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

public class TimeAxisLabelData
{
  long timestamp;
  String label;

  public TimeAxisLabelData(long timestamp, String label)
  {
    this.timestamp = timestamp;
    this.label = label;
  }

  public static TimeAxisLabelData[] labelDays(GraphData[] data)
  {
    ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
    long lastDay = Long.MIN_VALUE;
    Calendar calendar = Calendar.getInstance();

    for (GraphData datum : data)
    {
      calendar.setTimeInMillis(datum.timestamp);
      calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
      calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
      calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
      calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

      if (calendar.getTimeInMillis() > lastDay)
      {
        lastDay = calendar.getTimeInMillis();
        Date date = calendar.getTime();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        timeAxisLabelData.add(new TimeAxisLabelData(lastDay, dateFormat.format(date)));
      }
    }

    return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
  }
}
