package com.smartcampus.resource;

import com.smartcampus.exception.RoomNotEmptyException;
import com.smartcampus.model.Room;
import com.smartcampus.service.DataStore;

import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manages rooms at /api/v1/rooms.
 * Supports listing, creating, fetching by ID, and deleting with checks.
 */
@Path("/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    // ── GET /rooms ────────────────────────────────────────────────────

    @GET
    public Response getAllRooms() {
        return Response.ok(DataStore.getAllRooms()).build();
    }

    // ── POST /rooms ───────────────────────────────────────────────────

    @POST
    public Response createRoom(Room room, @Context UriInfo uriInfo) {
        // Check that a valid ID was supplied
        if (room == null || room.getId() == null || room.getId().isBlank()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(error("Room ID is required"))
                    .build();
        }

        // Prevent duplicate room IDs
        if (DataStore.roomExists(room.getId())) {
            return Response.status(Response.Status.CONFLICT)
                    .entity(error("Room with ID '" + room.getId() + "' already exists"))
                    .build();
        }

        // Make sure sensor list isn't null
        if (room.getSensorIds() == null) {
            room.setSensorIds(new CopyOnWriteArrayList<>());
        }

        DataStore.addRoom(room);

        // Return 201 Created with a Location header
        URI location = uriInfo.getAbsolutePathBuilder().path(room.getId()).build();
        return Response.created(location).entity(room).build();
    }

    // ── GET /rooms/{roomId} ───────────────────────────────────────────

    @GET
    @Path("/{roomId}")
    public Response getRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);
        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error("Room not found: " + roomId))
                    .build();
        }
        return Response.ok(room).build();
    }

    // ── DELETE /rooms/{roomId} ────────────────────────────────────────

    @DELETE
    @Path("/{roomId}")
    public Response deleteRoom(@PathParam("roomId") String roomId) {
        Room room = DataStore.getRoom(roomId);

        if (room == null) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(error("Room not found: " + roomId))
                    .build();
        }

        // Safety: a room with sensors still attached cannot be deleted
        if (!room.getSensorIds().isEmpty()) {
            throw new RoomNotEmptyException(
                "Cannot delete room '" + roomId + "' — it still has "
                + room.getSensorIds().size() + " sensor(s) assigned: "
                + room.getSensorIds()
            );
        }

        DataStore.removeRoom(roomId);
        return Response.noContent().build();
    }

    // ── helper ────────────────────────────────────────────────────────

    private Map<String, String> error(String message) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", message);
        return body;
    }
}