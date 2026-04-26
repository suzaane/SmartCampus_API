package com.smartcampus.model;

/**
 * A timestamped measurement from a sensor.
 * Each reading is stored as part of the sensor's history.
 */
public class SensorReading {

    private String id;
    private long timestamp;   // epoch time in milliseconds
    private double value;     // the recorded measurement

    // No-arg constructor required by Jackson
    public SensorReading() {}

    public SensorReading(String id, long timestamp, double value) {
        this.id = id;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }
}