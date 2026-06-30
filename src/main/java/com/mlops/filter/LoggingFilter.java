package com.mlops.filter;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.ext.Provider;
import java.util.logging.Logger;

/**
 * Observability filter (Part 5.5). Implements BOTH the request and response
 * filter interfaces so it can log every incoming request and outgoing response.
 *
 * Uses java.util.logging to record the HTTP method, the request URI, and (on the
 * way out) the final status code - the core data needed to trace what the server
 * did with each call.
 */
@Provider
public class LoggingFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOGGER = Logger.getLogger(LoggingFilter.class.getName());

    /** Runs for every INCOMING request before it reaches a resource method. */
    @Override
    public void filter(ContainerRequestContext requestContext) {
        LOGGER.info(String.format("--> REQUEST  %s %s",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri()));
    }

    /** Runs for every OUTGOING response after the resource method completes. */
    @Override
    public void filter(ContainerRequestContext requestContext,
                       ContainerResponseContext responseContext) {
        LOGGER.info(String.format("<-- RESPONSE %s %s : status %d",
                requestContext.getMethod(),
                requestContext.getUriInfo().getRequestUri(),
                responseContext.getStatus()));
    }
}
