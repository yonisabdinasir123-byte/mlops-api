package com.mlops.resource;

import com.mlops.exception.LinkedWorkspaceNotFoundException;
import com.mlops.model.MLWorkspace;
import com.mlops.model.MachineLearningModel;
import com.mlops.store.DataStore;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Resource for managing ML models under /api/v1/models.
 */
@Path("/models")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ModelResource {

    private final DataStore store = DataStore.getInstance();

    /**
     * GET /api/v1/models with an optional ?status= filter.
     * When 'status' is supplied, only models whose status matches are returned.
     */
    @GET
    public List<MachineLearningModel> list(@QueryParam("status") String status) {
        List<MachineLearningModel> all = new ArrayList<>(store.getAllModels());
        if (status == null || status.isBlank()) {
            return all;
        }
        List<MachineLearningModel> filtered = new ArrayList<>();
        for (MachineLearningModel model : all) {
            if (status.equalsIgnoreCase(model.getStatus())) {
                filtered.add(model);
            }
        }
        return filtered;
    }

    /** GET /api/v1/models/{modelId} - fetch one model, 404 if it does not exist. */
    @GET
    @Path("/{modelId}")
    public MachineLearningModel getOne(@PathParam("modelId") String modelId) {
        MachineLearningModel model = store.getModel(modelId);
        if (model == null) {
            throw new NotFoundException("Model not found: " + modelId);
        }
        return model;
    }

    /**
     * POST /api/v1/models - register a new model.
     * Integrity rules:
     *   - the referenced workspaceId must exist (else 422 via mapper);
     *   - the server generates the unique id; any client-supplied id is ignored.
     */
    @POST
    public Response create(MachineLearningModel input, @Context UriInfo uriInfo) {
        if (input == null || input.getWorkspaceId() == null || input.getWorkspaceId().isBlank()) {
            throw new BadRequestException("Field 'workspaceId' is required to register a model.");
        }

        MLWorkspace workspace = store.getWorkspace(input.getWorkspaceId());
        if (workspace == null) {
            throw new LinkedWorkspaceNotFoundException(input.getWorkspaceId());
        }

        // Server-generated identifier - the client never controls the id.
        String id = "MOD-" + UUID.randomUUID();
        String status = (input.getStatus() == null || input.getStatus().isBlank())
                ? "TRAINING" : input.getStatus();

        MachineLearningModel model = new MachineLearningModel(
                id,
                input.getFramework(),
                status,
                input.getLatestAccuracy(),
                input.getWorkspaceId());

        store.saveModel(model);
        workspace.getModelIds().add(id); // keep the parent workspace consistent

        URI location = uriInfo.getAbsolutePathBuilder().path(id).build();
        return Response.created(location).entity(model).build();
    }

    /**
     * Sub-resource LOCATOR for /api/v1/models/{modelId}/metrics.
     *
     * Note there is no HTTP-method annotation (@GET/@POST) here: that is what
     * makes this a locator rather than a handler. JAX-RS calls it to obtain an
     * {@link EvaluationMetricResource} instance, then continues matching the
     * remaining path (GET / POST) against the methods of that returned object.
     */
    @Path("/{modelId}/metrics")
    public EvaluationMetricResource metrics(@PathParam("modelId") String modelId) {
        if (store.getModel(modelId) == null) {
            throw new NotFoundException("Model not found: " + modelId);
        }
        return new EvaluationMetricResource(modelId);
    }
}
