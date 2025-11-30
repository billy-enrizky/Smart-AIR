package com.example.myapplication.medication;

import java.util.ArrayList;
import java.util.List;

public class ControllerSchedule {
    private List<String> times;

    public ControllerSchedule() {
        this.times = new ArrayList<>();
    }

    public ControllerSchedule(List<String> times) {
        this.times = times != null ? new ArrayList<>(times) : new ArrayList<>();
    }

    public List<String> getTimes() {
        return times;
    }

    public void setTimes(List<String> times) {
        this.times = times != null ? new ArrayList<>(times) : new ArrayList<>();
    }

    public void addTime(String time) {
        if (times == null) {
            times = new ArrayList<>();
        }
        if (time != null && !times.contains(time)) {
            times.add(time);
        }
    }

    public void removeTime(String time) {
        if (times != null) {
            times.remove(time);
        }
    }

    public boolean isEmpty() {
        return times == null || times.isEmpty();
    }
}

