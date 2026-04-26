package com.smartcampus.exception.mapper;

import com.smartcampus.exception.RoomNotEmptyException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Returns 409 Conflict when trying to delete a room that still has sensors.
 */
@Provider
public class RoomNotEmptyExceptionMapper implements ExceptionMapper<RoomNotEmptyException> {

    @Override
    public Response toResponse(RoomNotEmptyException exception) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error",   "RoomNotEmpty");
        body.put("message", exception.getMessage());

        return Response
                .status(Response.Status.CONFLICT)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}