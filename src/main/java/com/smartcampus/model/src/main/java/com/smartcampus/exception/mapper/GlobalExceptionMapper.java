package com.smartcampus.exception.mapper;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Catch-all mapper – any unhandled exception becomes a safe 500 response.
 * JAX-RS built-in exceptions (like 404) still pass through correctly.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {

        // Don't interfere with JAX-RS's own exceptions
        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse();
        }

        // Log the real error for debugging
        LOG.severe("Unexpected error: " + exception.getClass().getName()
                   + " - " + exception.getMessage());

        // Never expose stack trace to the client
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error",   "Internal Server Error");
        body.put("message", "An unexpected error occurred. Please contact support.");

        return Response
                .status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}