package com.voxtric.timegraph;

import androidx.annotation.NonNull;

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

  public static TimeAxisLabelData[] labelMonths(@NonNull GraphData[] data)
  {
    ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
    if (data.length > 0)
    {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(data[0].timestamp);
      calendar.set(Calendar.DAY_OF_MONTH, calendar.getMinimum(Calendar.DAY_OF_MONTH));
      calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
      calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
      calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
      calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

      GraphData lastEntry = data[data.length - 1];
      while (calendar.getTimeInMillis() < lastEntry.timestamp)
      {
        Date date = calendar.getTime();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        timeAxisLabelData.add(new TimeAxisLabelData(calendar.getTimeInMillis(), dateFormat.format(date)));
        calendar.add(Calendar.MONTH, 1);
      }
    }

    return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
  }

  public static TimeAxisLabelData[] labelWeeks(@NonNull GraphData[] data)
  {
    ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
    if (data.length > 0)
    {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(data[0].timestamp);
      calendar.set(Calendar.DAY_OF_WEEK, calendar.getMinimum(Calendar.DAY_OF_WEEK));
      calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
      calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
      calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
      calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

      GraphData lastEntry = data[data.length - 1];
      while (calendar.getTimeInMillis() < lastEntry.timestamp)
      {
        Date date = calendar.getTime();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        timeAxisLabelData.add(new TimeAxisLabelData(calendar.getTimeInMillis(), dateFormat.format(date)));
        calendar.add(Calendar.WEEK_OF_YEAR, 1);
      }
    }

    return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
  }

  public static TimeAxisLabelData[] labelDays(@NonNull GraphData[] data)
  {
    ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
    if (data.length > 0)
    {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(data[0].timestamp);
      calendar.set(Calendar.HOUR_OF_DAY, calendar.getMinimum(Calendar.HOUR_OF_DAY));
      calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
      calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
      calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

      GraphData lastEntry = data[data.length - 1];
      while (calendar.getTimeInMillis() < lastEntry.timestamp)
      {
        Date date = calendar.getTime();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.SHORT);
        timeAxisLabelData.add(new TimeAxisLabelData(calendar.getTimeInMillis(), dateFormat.format(date)));
        calendar.add(Calendar.DATE, 1);
      }
    }

    return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
  }

  public static TimeAxisLabelData[] labelHours(@NonNull GraphData[] data)
  {
    ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
    if (data.length > 0)
    {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(data[0].timestamp);
      calendar.set(Calendar.MINUTE, calendar.getMinimum(Calendar.MINUTE));
      calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
      calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

      GraphData lastEntry = data[data.length - 1];
      while (calendar.getTimeInMillis() < lastEntry.timestamp)
      {
        Date date = calendar.getTime();
        DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        timeAxisLabelData.add(new TimeAxisLabelData(calendar.getTimeInMillis(), dateFormat.format(date)));
        calendar.add(Calendar.HOUR_OF_DAY, 1);
      }
    }

    return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
  }

  public static TimeAxisLabelData[] labelMinutes(@NonNull GraphData[] data)
  {
    ArrayList<TimeAxisLabelData> timeAxisLabelData = new ArrayList<>();
    if (data.length > 0)
    {
      Calendar calendar = Calendar.getInstance();
      calendar.setTimeInMillis(data[0].timestamp);
      calendar.set(Calendar.SECOND, calendar.getMinimum(Calendar.SECOND));
      calendar.set(Calendar.MILLISECOND, calendar.getMinimum(Calendar.MILLISECOND));

      GraphData lastEntry = data[data.length - 1];
      while (calendar.getTimeInMillis() < lastEntry.timestamp)
      {
        Date date = calendar.getTime();
        DateFormat dateFormat = DateFormat.getTimeInstance(DateFormat.SHORT);
        timeAxisLabelData.add(new TimeAxisLabelData(calendar.getTimeInMillis(), dateFormat.format(date)));
        calendar.add(Calendar.MINUTE, 1);
      }
    }

    return timeAxisLabelData.toArray(new TimeAxisLabelData[0]);
  }
}
