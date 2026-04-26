package com.smartcampus.exception.mapper;

import com.smartcampus.exception.SensorUnavailableException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Returns 403 Forbidden when trying to add a reading to a sensor
 * that is currently under maintenance.
 */
@Provider
public class SensorUnavailableExceptionMapper
        implements ExceptionMapper<SensorUnavailableException> {

    @Override
    public Response toResponse(SensorUnavailableException exception) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error",   "SensorUnavailable");
        body.put("message", exception.getMessage());

        return Response
                .status(Response.Status.FORBIDDEN)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}