package com.smartcampus.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root endpoint – returns API information and a map of available
 * resources (HATEOAS). Accessed at GET /api/v1/
 */
@Path("/")
public class DiscoveryResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiInfo() {

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("api",     "Smart Campus Sensor & Room Management API");
        response.put("version", "v1.0");
        response.put("status",  "RUNNING");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("team",  "Campus Infrastructure Team");
        contact.put("email", "smartcampus@university.edu");
        response.put("contact", contact);

        // Navigation links for clients
        Map<String, String> links = new LinkedHashMap<>();
        links.put("self",    "/api/v1/");
        links.put("rooms",   "/api/v1/rooms");
        links.put("sensors", "/api/v1/sensors");
        response.put("links", links);

        // What you can do with each resource
        Map<String, Object> actions = new LinkedHashMap<>();

        Map<String, String> roomActions = new LinkedHashMap<>();
        roomActions.put("list all rooms",  "GET    /api/v1/rooms");
        roomActions.put("get room by id",  "GET    /api/v1/rooms/{roomId}");
        roomActions.put("create room",     "POST   /api/v1/rooms");
        roomActions.put("delete room",     "DELETE /api/v1/rooms/{roomId}");
        actions.put("rooms", roomActions);

        Map<String, String> sensorActions = new LinkedHashMap<>();
        sensorActions.put("list all sensors",   "GET  /api/v1/sensors");
        sensorActions.put("filter by type",     "GET  /api/v1/sensors?type={type}");
        sensorActions.put("get sensor by id",   "GET  /api/v1/sensors/{sensorId}");
        sensorActions.put("create sensor",      "POST /api/v1/sensors");
        sensorActions.put("get readings",       "GET  /api/v1/sensors/{sensorId}/readings");
        sensorActions.put("add reading",        "POST /api/v1/sensors/{sensorId}/readings");
        actions.put("sensors", sensorActions);

        response.put("actions", actions);

        return Response.ok(response).build();
    }
}