package com.mlops.resource;

import com.mlops.exception.WorkspaceNotEmptyException;
import com.mlops.model.MLWorkspace;
import com.mlops.store.DataStore;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collection;

/**
 * Resource for managing ML workspaces under /api/v1/workspaces.
 */
@Path("/workspaces")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class WorkspaceResource {

    private final DataStore store = DataStore.getInstance();

    /** GET /api/v1/workspaces - list every workspace. */
    @GET
    public Collection<MLWorkspace> listAll() {
        return store.getAllWorkspaces();
    }

    /** GET /api/v1/workspaces/{id} - fetch one workspace, 404 if it does not exist. */
    @GET
    @Path("/{workspaceId}")
    public MLWorkspace getOne(@PathParam("workspaceId") String workspaceId) {
        MLWorkspace workspace = store.getWorkspace(workspaceId);
        if (workspace == null) {
            throw new NotFoundException("Workspace not found: " + workspaceId);
        }
        return workspace;
    }

    /** POST /api/v1/workspaces - create a workspace, returning 201 Created + Location header. */
    @POST
    public Response create(MLWorkspace workspace, @Context UriInfo uriInfo) {
        if (workspace == null || workspace.getId() == null || workspace.getId().isBlank()) {
            throw new BadRequestException("Field 'id' is required to create a workspace.");
        }
        store.saveWorkspace(workspace);
        URI location = uriInfo.getAbsolutePathBuilder().path(workspace.getId()).build();
        return Response.created(location).entity(workspace).build();
    }

    /**
     * DELETE /api/v1/workspaces/{id}.
     * Safety logic: a workspace that still owns models cannot be deleted - doing so
     * would orphan those models - so the request is blocked with HTTP 409 Conflict.
     */
    @DELETE
    @Path("/{workspaceId}")
    public Response delete(@PathParam("workspaceId") String workspaceId) {
        MLWorkspace workspace = store.getWorkspace(workspaceId);
        if (workspace == null) {
            throw new NotFoundException("Workspace not found: " + workspaceId);
        }
        if (workspace.getModelIds() != null && !workspace.getModelIds().isEmpty()) {
            throw new WorkspaceNotEmptyException(workspaceId, workspace.getModelIds().size());
        }
        store.removeWorkspace(workspaceId);
        return Response.noContent().build(); // 204 No Content
    }
}
