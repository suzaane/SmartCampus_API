package com.smartcampus.exception.mapper;

import com.smartcampus.exception.LinkedResourceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Converts a missing linked resource error into a 422 Unprocessable Entity.
 * (The request was syntactically fine but the reference doesn't exist.)
 */
@Provider
public class LinkedResourceNotFoundExceptionMapper
        implements ExceptionMapper<LinkedResourceNotFoundException> {

    @Override
    public Response toResponse(LinkedResourceNotFoundException exception) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error",   "InvalidReference");
        body.put("message", exception.getMessage());

        // 422 – no enum constant for this in older JAX-RS, so we use the code directly
        return Response
                .status(422)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}