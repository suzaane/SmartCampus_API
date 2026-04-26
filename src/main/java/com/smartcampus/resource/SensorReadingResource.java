package com.smartcampus.resource;

import com.smartcampus.exception.SensorUnavailableException;
import com.smartcampus.model.Sensor;
import com.smartcampus.model.SensorReading;
import com.smartcampus.service.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.UUID;

/**
 * Sub-resource for managing historical readings of a specific sensor.
 * Instantiated by SensorResource's sub-resource locator.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorReadingResource {

    private final String sensorId;

    // Constructor receives the sensor ID from the parent resource
    public SensorReadingResource(String sensorId) {
        this.sensorId = sensorId;
    }

    // GET / – return all readings for this sensor
    @GET
    public Response getReadings() {
        return Response.ok(DataStore.getReadings(sensorId)).build();
    }

    // POST / – add a new reading
    @POST
    public Response addReading(SensorReading reading) {
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            throw new NotFoundException("Sensor not found: " + sensorId);
        }

        // Sensors in maintenance cannot accept new readings
        if ("MAINTENANCE".equalsIgnoreCase(sensor.getStatus())) {
            throw new SensorUnavailableException(
                "Sensor '" + sensorId + "' is under maintenance and cannot accept readings.");
        }

        // Generate ID if missing
        if (reading.getId() == null || reading.getId().isBlank()) {
            reading.setId(UUID.randomUUID().toString());
        }

        // Use server time if timestamp is 0 or omitted
        if (reading.getTimestamp() == 0) {
            reading.setTimestamp(System.currentTimeMillis());
        }

        DataStore.addReading(sensorId, reading);

        // Side-effect: update the parent sensor's live value
        sensor.setCurrentValue(reading.getValue());

        return Response.status(Response.Status.CREATED).entity(reading).build();
    }
}