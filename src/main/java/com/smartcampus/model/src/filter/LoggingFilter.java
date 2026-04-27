package com.smartcampus.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * =========================================================================
 * LoggingFilter — Part 5 (5 marks)
 * Implements: ContainerRequestFilter + ContainerResponseFilter
 * =========================================================================
 *
 * A JAX-RS filter is a cross-cutting concern — it intercepts EVERY request
 * and response without the resource methods knowing about it.
 *
 * This single class implements BOTH filter interfaces:
 *   ContainerRequestFilter  → runs BEFORE the resource method executes
 *   ContainerResponseFilter → runs AFTER the resource method has returned
 *
 * @Provider registers this with Jersey's component registry during startup.
 * Jersey then calls the appropriate filter() method on every HTTP cycle.
 *
 * REPORT Q10 — Why filters for logging instead of Logger.info() in every method?
 * -----------------------------------------------------------------------
 * Using Logger.info() inside each resource method has these problems:
 *
 *   1. DUPLICATION: With 10+ endpoints, you'd write 20+ log statements.
 *      Every new endpoint requires remembering to add logging.
 *
 *   2. OMISSION RISK: It's easy to forget. A developer adds a new endpoint,
 *      forgets the log call, and that endpoint has no observability.
 *
 *   3. SEPARATION OF CONCERNS VIOLATION: Logging is infrastructure code,
 *      not business logic. Mixing it into resource methods violates the
 *      Single Responsibility Principle. Resource methods should only contain
 *      business logic (create room, validate sensor, etc.).
 *
 *   4. FORMAT INCONSISTENCY: Different developers log different fields in
 *      different formats, making log analysis difficult.
 *
 *   5. MAINTENANCE COST: To change the log format (e.g. add a request ID),
 *      you'd have to update every single resource method.
 *
 *   FILTER SOLUTION:
 *   One filter = guaranteed, consistent, zero-overhead logging for EVERY
 *   endpoint automatically. Change the format in one place. Add a new endpoint
 *   — it's already logged. This is the AOP (Aspect-Oriented Programming)
 *   principle applied to JAX-RS.
 *
 * LOGGED INFORMATION:
 *   Incoming: [METHOD] URI        e.g.  [GET] http://localhost:8080/api/v1/rooms
 *   Outgoing: [METHOD] URI → 201 e.g.  [POST] http://localhost:8080/api/v1/rooms → 201
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = Logger.getLogger(LoggingFilter.class.getName());

    /**
     * Called BEFORE the resource method executes.
     * Logs the HTTP method and full request URI.
     *
     * ContainerRequestContext provides:
     *   - getMethod()   → "GET", "POST", "DELETE", etc.
     *   - getUriInfo()  → full URI, path params, query params
     *   - getHeaders()  → request headers (not logged here for brevity)
     */
    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        String method = requestContext.getMethod();
        String uri    = requestContext.getUriInfo().getRequestUri().toString();
        LOG.info(String.format(">>> Incoming request : [%s] %s", method, uri));
    }

    /**
     * Called AFTER the resource method has returned (and after the response
     * entity has been set, but before the bytes are written to the socket).
     * Logs the HTTP method, URI, and response status code.
     *
     * ContainerResponseContext provides:
     *   - getStatus()   → e.g. 200, 201, 404, 409, 500
     *   - getHeaders()  → response headers
     *   - getEntity()   → the response body object (before Jackson serialises it)
     *
     * We re-read the method and URI from the REQUEST context here — the
     * response context doesn't have them, so we use the request context
     * that JAX-RS passes alongside.
     */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) throws IOException {
        int    status = responseContext.getStatus();
        String method = requestContext.getMethod();
        String uri    = requestContext.getUriInfo().getRequestUri().toString();
        LOG.info(String.format("<<< Outgoing response: [%s] %s → %d", method, uri, status));
    }
}