package com.smartcampus.model;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a room on campus.
 * Sensor IDs are stored in a thread-safe list to handle concurrent requests safely.
 */
public class Room {

    private String id;
    private String name;
    private int capacity;

    // Thread-safe list — multiple requests can add sensors without corrupting the data
    private List<String> sensorIds = new CopyOnWriteArrayList<>();

    // No-arg constructor required by Jackson for JSON deserialisation
    public Room() {}

    public Room(String id, String name, int capacity) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    public List<String> getSensorIds() {
        return sensorIds;
    }

    public void setSensorIds(List<String> sensorIds) {
        this.sensorIds = sensorIds;
    }
}