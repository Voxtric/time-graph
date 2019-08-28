package com.voxtric.timegraph;

public interface GraphDataProvider
{
  GraphData[] getData(long startTimestamp, long endTimestamp, long visibleStartTimestamp, long visibleEndTimestamp);
  TimeAxisLabelData[] getLabelsForData(GraphData[] data);
}
