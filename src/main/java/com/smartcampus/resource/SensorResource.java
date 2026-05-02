package com.smartcampus.resource;

import com.smartcampus.exception.LinkedResourceNotFoundException;
import com.smartcampus.model.Sensor;
import com.smartcampus.service.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Manages sensors at /api/v1/sensors.
 * Includes a sub-resource locator for /{sensorId}/readings.
 */
@Path("/sensors")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class SensorResource {

    // ── GET /sensors (optional ?type= filter) ─────────────────────────

    @GET
    public Response getSensors(@QueryParam("type") String type) {
        Collection<Sensor> all = DataStore.getAllSensors();

        if (type != null && !type.isBlank()) {
            all = all.stream()
                    .filter(s -> type.equalsIgnoreCase(s.getType()))
                    .collect(Collectors.toList());
        }

        return Response.ok(all).build();
    }

    // ── GET /sensors/{sensorId} ───────────────────────────────────────

    @GET
    @Path("/{sensorId}")
    public Response getSensor(@PathParam("sensorId") String sensorId) {
        Sensor sensor = DataStore.getSensor(sensorId);
        if (sensor == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error("Sensor not found: " + sensorId))
                    .build();
        }
        return Response.ok(sensor).build();
    }

    // ── POST /sensors ─────────────────────────────────────────────────
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createSensor(Sensor sensor, @Context UriInfo uriInfo) {
        if (sensor == null || sensor.getId() == null || sensor.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Sensor ID is required"))
                    .build();
        }

        // The roomId must reference an existing room (otherwise 422)
        if (sensor.getRoomId() == null || !DataStore.roomExists(sensor.getRoomId())) {
            throw new LinkedResourceNotFoundException(
                "Room '" + sensor.getRoomId() + "' does not exist. Cannot register sensor."
            );
        }

        if (DataStore.sensorExists(sensor.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(error("Sensor with ID '" + sensor.getId() + "' already exists"))
                    .build();
        }

        DataStore.addSensor(sensor);

        // Add sensor ID to the room's list (thread‑safe: CopyOnWriteArrayList)
        DataStore.getRoom(sensor.getRoomId()).getSensorIds().add(sensor.getId());

        URI location = uriInfo.getAbsolutePathBuilder().path(sensor.getId()).build();
        return Response.created(location).entity(sensor).build();
    }

    // ── Sub-resource locator for readings ─────────────────────────────

    @Path("/{sensorId}/readings")
    public SensorReadingResource getSensorReadingResource(
            @PathParam("sensorId") String sensorId) {

        if (!DataStore.sensorExists(sensorId)) {
            throw new LinkedResourceNotFoundException("Sensor not found: " + sensorId);
        }

        return new SensorReadingResource(sensorId);
    }

    // ── helper ────────────────────────────────────────────────────────

    private Map<String, String> error(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return body;
    }
}