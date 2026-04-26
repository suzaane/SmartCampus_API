package com.smartcampus.service;

import com.smartcampus.model.Room;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * =========================================================================
 * DataStore — Shared In-Memory State (Thread-Safe)
 * =========================================================================
 *
 * WHY THIS CLASS EXISTS (directly answers Report Q1 on resource lifecycle):
 * -----------------------------------------------------------------------
 * JAX-RS creates a NEW instance of each @Path resource class for EVERY
 * incoming HTTP request. If we stored our rooms/sensors as instance variables
 * on RoomResource, they would be re-initialised on every request and all
 * data would be lost.
 *
 * Solution: ALL shared state lives here as STATIC fields. Static fields
 * belong to the JVM Class object (loaded once), not to any particular
 * resource instance. Every request thread — regardless of which resource
 * instance it uses — reads from and writes to the same maps.
 *
 * THREAD SAFETY — ConcurrentHashMap:
 * -----------------------------------------------------------------------
 * A standard HashMap is NOT thread-safe. Two simultaneous PUT operations
 * from different request threads can corrupt its internal hash table
 * (infinite loop, ArrayIndexOutOfBoundsException, silent data loss).
 *
 * ConcurrentHashMap uses fine-grained bucket-level locking (Java 8+: lock-free
 * CAS operations). Multiple threads can read/write simultaneously without
 * corrupting data. This is the correct choice for a shared in-memory store.
 *
 * SEED DATA:
 * -----------------------------------------------------------------------
 * The static initialiser block runs ONCE when the JVM first loads this class.
 * It inserts realistic demo data so the API is immediately usable for testing
 * without needing to POST anything first.
 *
 * Seed rooms  : LIB-301 (Library), ENG-101 (Engineering Lab)
 * Seed sensors: TEMP-001 (Active), CO2-001 (Active), OCC-001 (Maintenance)
 * Seed reading: One historical reading for TEMP-001
 */
public class DataStore {

    // ── Storage Maps ────────────────────────────────────────────────────────

    /** Primary store for Room objects. Key = room ID (e.g. "LIB-301"). */
    private static final Map<String, Room> rooms = new ConcurrentHashMap<>();

    /** Primary store for Sensor objects. Key = sensor ID (e.g. "TEMP-001"). */
    private static final Map<String, Sensor> sensors = new ConcurrentHashMap<>();

    /**
     * Historical readings per sensor.
     * Key = sensor ID, Value = ordered list of SensorReading objects.
     * CopyOnWriteArrayList ensures concurrent POSTs to the same sensor's
     * readings list don't corrupt the list's internal array.
     */
    private static final Map<String, List<SensorReading>> readings = new ConcurrentHashMap<>();

    // ── Seed Data ────────────────────────────────────────────────────────────

    static {
        // --- Rooms ---
        Room lib301 = new Room("LIB-301", "Library Quiet Study", 50);
        Room eng101 = new Room("ENG-101", "Engineering Lab A", 25);
        rooms.put(lib301.getId(), lib301);
        rooms.put(eng101.getId(), eng101);

        // --- Sensors ---
        Sensor temp001 = new Sensor("TEMP-001", "Temperature", "ACTIVE",     22.5, "LIB-301");
        Sensor co2001  = new Sensor("CO2-001",  "CO2",         "ACTIVE",    450.0, "LIB-301");
        // OCC-001 is deliberately MAINTENANCE so the 403 behaviour can be tested immediately
        Sensor occ001  = new Sensor("OCC-001",  "Occupancy",   "MAINTENANCE", 12.0, "ENG-101");

        sensors.put(temp001.getId(), temp001);
        sensors.put(co2001.getId(),  co2001);
        sensors.put(occ001.getId(),  occ001);

        // Link sensors to their rooms (keeps sensorIds list in sync)
        lib301.getSensorIds().add("TEMP-001");
        lib301.getSensorIds().add("CO2-001");
        eng101.getSensorIds().add("OCC-001");

        // --- Seed readings for TEMP-001 ---
        List<SensorReading> temp001Readings = new CopyOnWriteArrayList<>();
        temp001Readings.add(new SensorReading(UUID.randomUUID().toString(),
                System.currentTimeMillis() - 60000, 21.0)); // 1 min ago
        temp001Readings.add(new SensorReading(UUID.randomUUID().toString(),
                System.currentTimeMillis(), 22.5));          // now
        readings.put("TEMP-001", temp001Readings);

        // Initialise empty reading lists for other sensors
        readings.put("CO2-001", new CopyOnWriteArrayList<>());
        readings.put("OCC-001", new CopyOnWriteArrayList<>());
    }

    // ── Room Operations ──────────────────────────────────────────────────────

    public static Collection<Room> getAllRooms() {
        return rooms.values();
    }

    public static Room getRoom(String id) {
        return rooms.get(id);
    }

    public static void addRoom(Room room) {
        if (room != null && room.getId() != null) {
            rooms.put(room.getId(), room);
        }
    }

    public static void removeRoom(String id) {
        rooms.remove(id);
    }

    public static boolean roomExists(String id) {
        return id != null && rooms.containsKey(id);
    }

    // ── Sensor Operations ────────────────────────────────────────────────────

    public static Collection<Sensor> getAllSensors() {
        return sensors.values();
    }

    public static Sensor getSensor(String id) {
        return sensors.get(id);
    }

    public static void addSensor(Sensor sensor) {
        if (sensor != null && sensor.getId() != null) {
            sensors.put(sensor.getId(), sensor);
        }
    }

    public static void removeSensor(String id) {
        sensors.remove(id);
    }

    public static boolean sensorExists(String id) {
        return id != null && sensors.containsKey(id);
    }

    // ── Sensor Reading Operations ────────────────────────────────────────────

    public static List<SensorReading> getReadings(String sensorId) {
        return readings.getOrDefault(sensorId, new CopyOnWriteArrayList<>());
    }

    public static void addReading(String sensorId, SensorReading reading) {
        if (sensorId == null || reading == null) return;
        // computeIfAbsent is atomic: creates the list only if the key is absent
        readings.computeIfAbsent(sensorId, k -> new CopyOnWriteArrayList<>()).add(reading);
    }

    // ── Utility ──────────────────────────────────────────────────────────────

    /** Prevent instantiation — this is a pure static utility class. */
    private DataStore() {}
}