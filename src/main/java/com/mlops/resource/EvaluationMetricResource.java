package com.mlops.resource;

import com.mlops.exception.ModelDeprecatedException;
import com.mlops.model.EvaluationMetric;
import com.mlops.model.MachineLearningModel;
import com.mlops.store.DataStore;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Sub-resource handling the evaluation-metric history of ONE model.
 *
 * This class is NOT registered as a root resource and has no class-level @Path:
 * it is instantiated by the sub-resource locator method in {@link ModelResource}
 * (mapped to {modelId}/metrics), which passes in the parent model id. The
 * class-level @Produces/@Consumes apply to every method below.
 */
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EvaluationMetricResource {

    private final DataStore store = DataStore.getInstance();
    private final String modelId;

    public EvaluationMetricResource(String modelId) {
        this.modelId = modelId;
    }

    /** GET /api/v1/models/{modelId}/metrics - the full metric history for this model. */
    @GET
    public List<EvaluationMetric> history() {
        return store.getMetrics(modelId);
    }

    /**
     * POST /api/v1/models/{modelId}/metrics - append a new evaluation metric.
     *
     * Side effect: a successful append updates the parent model's
     * latestAccuracy so the model summary stays consistent with its history.
     * The metric id is always server-generated (UUID).
     */
    @POST
    public Response add(EvaluationMetric input, @Context UriInfo uriInfo) {
        MachineLearningModel model = store.getModel(modelId);

        // State constraint (Part 5.3): a DEPRECATED model is no longer monitored
        // and must reject new metrics with HTTP 403 Forbidden.
        if ("DEPRECATED".equalsIgnoreCase(model.getStatus())) {
            throw new ModelDeprecatedException(modelId);
        }

        long timestamp = (input != null && input.getTimestamp() > 0)
                ? input.getTimestamp() : System.currentTimeMillis();
        double accuracy = (input != null) ? input.getAccuracyScore() : 0.0;

        EvaluationMetric metric = new EvaluationMetric(UUID.randomUUID().toString(), timestamp, accuracy);
        store.addMetric(modelId, metric);

        // --- Side effect: keep the parent model's headline accuracy in sync ---
        model.setLatestAccuracy(metric.getAccuracyScore());

        URI location = uriInfo.getAbsolutePathBuilder().path(metric.getId()).build();
        return Response.created(location).entity(metric).build();
    }
}
