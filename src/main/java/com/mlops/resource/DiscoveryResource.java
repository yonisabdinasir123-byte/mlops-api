package com.mlops.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root "Discovery" endpoint mapped to GET /api/v1.
 *
 * Returns API metadata (version, administrative contact) plus a map of the
 * primary resource collections, so a client can discover the API surface
 * without prior knowledge of every path (HATEOAS-style entry point).
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
public class DiscoveryResource {

    @GET
    public Response discover() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("apiName", "MLOps Pipeline Management API");
        info.put("version", "v1");
        info.put("status", "operational");

        Map<String, String> contact = new LinkedHashMap<>();
        contact.put("team", "AI Platform Engineering");
        contact.put("email", "mlops-support@ailab.example");
        info.put("administrativeContact", contact);

        Map<String, String> resources = new LinkedHashMap<>();
        resources.put("workspaces", "/api/v1/workspaces");
        resources.put("models", "/api/v1/models");
        resources.put("metrics", "/api/v1/models/{modelId}/metrics");
        info.put("resources", resources);

        return Response.ok(info).build();
    }
}
