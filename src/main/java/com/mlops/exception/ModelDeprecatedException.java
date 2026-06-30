package com.mlops.exception;

/**
 * Thrown when a client attempts to POST a new evaluation metric to a model whose
 * status is "DEPRECATED". Such models are no longer monitored, so the operation
 * is rejected with HTTP 403 Forbidden by its mapper.
 */
public class ModelDeprecatedException extends RuntimeException {

    private final String modelId;

    public ModelDeprecatedException(String modelId) {
        super("Model '" + modelId + "' is DEPRECATED and can no longer accept new evaluation metrics.");
        this.modelId = modelId;
    }

    public String getModelId() {
        return modelId;
    }
}
