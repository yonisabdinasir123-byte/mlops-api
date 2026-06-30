package com.mlops.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Diagnostic endpoint used ONLY to demonstrate the global ExceptionMapper&lt;Throwable&gt;
 * safety net (Part 5.4). Calling GET /api/v1/_debug/trigger-error throws an
 * unchecked exception on purpose, so you can verify the API responds with a clean
 * generic HTTP 500 (and a JSON body) instead of leaking a raw stack trace.
 */
@Path("/_debug")
@Produces(MediaType.APPLICATION_JSON)
public class DebugResource {

    @GET
    @Path("/trigger-error")
    public String triggerError() {
        throw new RuntimeException("Deliberate failure to demonstrate the global 500 safety net.");
    }
}
