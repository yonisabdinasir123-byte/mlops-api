package com.mlops.exception.mapper;

import com.mlops.exception.ErrorResponse;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Global "catch-all" safety net (Part 5.4). Because it maps the most generic type
 * (Throwable), JAX-RS only selects it when no more specific mapper matches an
 * exception. This guarantees the API never leaks a raw stack trace: any
 * unexpected runtime error (NullPointerException, IndexOutOfBoundsException, ...)
 * is turned into a generic HTTP 500 with a clean JSON body.
 *
 * It deliberately re-uses the response carried by a {@link WebApplicationException}
 * (e.g. the 404 from a NotFoundException) so that intentional HTTP errors keep
 * their correct status instead of being masked as 500.
 */
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger LOGGER = Logger.getLogger(GenericExceptionMapper.class.getName());

    @Override
    public Response toResponse(Throwable exception) {
        // Preserve deliberate JAX-RS HTTP errors (404, 400, 405, ...).
        if (exception instanceof WebApplicationException) {
            return ((WebApplicationException) exception).getResponse();
        }

        // Log the full detail server-side for debugging, but never return it.
        LOGGER.log(Level.SEVERE, "Unhandled exception intercepted by the global safety net", exception);

        ErrorResponse body = new ErrorResponse(
                Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(),
                "Internal Server Error",
                "An unexpected error occurred. Please contact the API administrator.");
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
