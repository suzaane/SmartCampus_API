package com.smartcampus.service;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory, thread-safe storage for rooms, sensors, and readings.
 * Uses static maps so all resource classes share the same data.
 */
public class DataStore {

    // Thread-safe maps for each entity
    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();
    private static final Map<String, Sensor> sensors = new ConcurrentHashMap<>();
    private static final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // Pre-populate some test data so the API is not empty on startup
    static {
        Room lib301 = new Room("LIB-301", "Library Quiet Study", 50);
        Room eng101 = new Room("ENG-101", "Engineering Lab A", 25);
        rooms.put(lib301.getId(), lib301);
        rooms.put(eng101.getId(), eng101);

        Sensor temp001 = new Sensor("TEMP-001", "Temperature", "ACTIVE",      22.5, "LIB-301");
        Sensor co2001  = new Sensor("CO2-001",  "CO2",         "ACTIVE",     450.0, "LIB-301");
        Sensor occ001  = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE",  12.0, "ENG-101");

        sensors.put(temp001.getId(), temp001);
        sensors.put(co2001.getId(),  co2001);
        sensors.put(occ001.getId(),  occ001);

        // Link sensors to rooms
        lib301.getSensorIds().add("TEMP-001");
        lib301.getSensorIds().add("CO2-001");
        eng101.getSensorIds().add("OCC-001");

        // Seed a few readings for TEMP-001
        List<SensorReading> tempReadings = new CopyOnWriteArrayList<>();
        tempReadings.add(new SensorReading(UUID.randomUUID().toString(),
                System.currentTimeMillis() - 60000, 21.0));
        tempReadings.add(new SensorReading(UUID.randomUUID().toString(),
                System.currentTimeMillis(), 22.5));
        readings.put("TEMP-001", tempReadings);

        // Empty reading lists for the other sensors
        readings.put("CO2-001", new CopyOnWriteArrayList<>());
        readings.put("OCC-001", new CopyOnWriteArrayList<>());
    }

    //── Room helpers ────────────────────────────────────────────────
    public static Collection<Room> getAllRooms() { return rooms.values(); }
    public static Room getRoom(String id) { return rooms.get(id); }
    public static void addRoom(Room room) {
        if (room != null && room.getId() != null) rooms.put(room.getId(), room);
    }
    public static void removeRoom(String id) { rooms.remove(id); }
    public static boolean roomExists(String id) { return id != null && rooms.containsKey(id); }

    //── Sensor helpers ──────────────────────────────────────────────
    public static Collection<Sensor> getAllSensors() { return sensors.values(); }
    public static Sensor getSensor(String id) { return sensors.get(id); }
    public static void addSensor(Sensor sensor) {
        if (sensor != null && sensor.getId() != null) sensors.put(sensor.getId(), sensor);
    }
    public static boolean sensorExists(String id) { return id != null && sensors.containsKey(id); }

    //── Reading helpers ─────────────────────────────────────────────
    public static List<SensorReading> getReadings(String sensorId) {
        return readings.getOrDefault(sensorId, new CopyOnWriteArrayList<>());
    }
    public static void addReading(String sensorId, SensorReading reading) {
        if (sensorId == null || reading == null) return;
        readings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>()).add(reading);
    }

    // Prevent instantiation – this is a static utility class
    private DataStore() {}
}