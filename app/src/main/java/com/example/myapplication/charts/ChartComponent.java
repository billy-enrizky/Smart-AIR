package com.example.myapplication.charts;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.myapplication.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChartComponent {
    public enum ChartType {
        LINE,
        BAR,
        PIE
    }

    public static View createChartView(Context context, ViewGroup parent, ChartType type) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.component_chart, parent, false);

        LineChart lineChart = view.findViewById(R.id.lineChart);
        BarChart barChart = view.findViewById(R.id.barChart);
        PieChart pieChart = view.findViewById(R.id.pieChart);

        switch (type) {
            case LINE:
                lineChart.setVisibility(View.VISIBLE);
                barChart.setVisibility(View.GONE);
                pieChart.setVisibility(View.GONE);
                break;
            case BAR:
                lineChart.setVisibility(View.GONE);
                barChart.setVisibility(View.VISIBLE);
                pieChart.setVisibility(View.GONE);
                break;
            case PIE:
                lineChart.setVisibility(View.GONE);
                barChart.setVisibility(View.GONE);
                pieChart.setVisibility(View.VISIBLE);
                break;
        }

        return view;
    }

    public static void setupLineChart(LineChart chart, List<PEFDataPoint> dataPoints, String label) {
        if (chart == null || dataPoints == null || dataPoints.isEmpty()) {
            return;
        }

        List<Entry> entries = new ArrayList<>();
        float minY = Float.MAX_VALUE;
        float maxY = Float.MIN_VALUE;
        for (int i = 0; i < dataPoints.size(); i++) {
            PEFDataPoint point = dataPoints.get(i);
            float pefValue = (float) point.getPefValue();
            entries.add(new Entry(i, pefValue));
            if (pefValue < minY) minY = pefValue;
            if (pefValue > maxY) maxY = pefValue;
        }

        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setColor(Color.parseColor("#2196F3"));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.parseColor("#2196F3"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setYOffset(5f);
        if (entries.size() > 0) {
            float xPadding = 0.5f;
            xAxis.setAxisMinimum(-xPadding);
            xAxis.setAxisMaximum(entries.size() - 1 + xPadding);
        }
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < dataPoints.size()) {
                    long timestamp = dataPoints.get(index).getTimestamp();
                    SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
                    return sdf.format(new Date(timestamp));
                }
                return "";
            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        float circleRadius = 4f;
        float yRange = maxY - minY;
        
        if (yRange == 0) {
            yRange = maxY > 0 ? maxY : 100f;
        }
        
        float paddingPercent = 0.15f;
        float absoluteMinPadding = circleRadius * 2 + 10f;
        float absoluteMaxPadding = circleRadius * 2 + 10f;
        
        float minPadding = Math.max(yRange * paddingPercent, absoluteMinPadding);
        float maxPadding = Math.max(yRange * paddingPercent, absoluteMaxPadding);
        
        float axisMin = Math.max(0f, minY - minPadding);
        float axisMax = maxY + maxPadding;
        
        leftAxis.setAxisMinimum(axisMin);
        leftAxis.setAxisMaximum(axisMax);

        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setAutoScaleMinMaxEnabled(false);
        
        float sideOffset = 20f;
        float topOffset = 20f;
        float bottomOffset = 40f;
        chart.setExtraOffsets(sideOffset, topOffset, sideOffset, bottomOffset);
        
        chart.invalidate();
    }

    public static void setupBarChart(BarChart chart, Map<String, Integer> data, String label) {
        if (chart == null || data == null || data.isEmpty()) {
            return;
        }

        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>(data.keySet());
        for (int i = 0; i < labels.size(); i++) {
            entries.add(new BarEntry(i, data.get(labels.get(i))));
        }

        BarDataSet dataSet = new BarDataSet(entries, label);
        dataSet.setColors(getZoneColors(labels));
        dataSet.setDrawValues(true);

        BarData barData = new BarData(dataSet);
        chart.setData(barData);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1f);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) {
                    return labels.get(index);
                }
                return "";
            }
        });

        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMinimum(0f);

        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.invalidate();
    }

    public static void setupPieChart(PieChart chart, Map<String, Integer> data, String label) {
        if (chart == null || data == null || data.isEmpty()) {
            return;
        }

        List<PieEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>(data.keySet());
        int total = 0;
        for (String key : labels) {
            total += data.get(key);
        }

        for (String key : labels) {
            float percentage = total > 0 ? (data.get(key) * 100f / total) : 0f;
            entries.add(new PieEntry(percentage, key));
        }

        PieDataSet dataSet = new PieDataSet(entries, label);
        dataSet.setColors(getZoneColors(labels));
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(Color.WHITE);

        PieData pieData = new PieData(dataSet);
        chart.setData(pieData);
        chart.getDescription().setEnabled(false);
        chart.setEntryLabelTextSize(12f);
        chart.invalidate();
    }

    private static List<Integer> getZoneColors(List<String> zones) {
        List<Integer> colors = new ArrayList<>();
        for (String zone : zones) {
            if ("Green".equalsIgnoreCase(zone)) {
                colors.add(Color.parseColor("#4CAF50"));
            } else if ("Yellow".equalsIgnoreCase(zone)) {
                colors.add(Color.parseColor("#FFC107"));
            } else if ("Red".equalsIgnoreCase(zone)) {
                colors.add(Color.parseColor("#F44336"));
            } else {
                colors.add(Color.parseColor("#9E9E9E"));
            }
        }
        return colors;
    }

    public static class PEFDataPoint {
        private long timestamp;
        private int pefValue;

        public PEFDataPoint(long timestamp, int pefValue) {
            this.timestamp = timestamp;
            this.pefValue = pefValue;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public int getPefValue() {
            return pefValue;
        }
    }
}

