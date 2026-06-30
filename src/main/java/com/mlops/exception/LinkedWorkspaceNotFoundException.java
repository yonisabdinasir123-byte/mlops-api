package com.mlops.exception;

/**
 * Thrown when a client tries to register a model whose {@code workspaceId} does
 * not refer to an existing workspace. Mapped to HTTP 422 Unprocessable Entity
 * (a 4xx client error) because the request was well-formed but references data
 * that fails a business-rule / referential-integrity check.
 */
public class LinkedWorkspaceNotFoundException extends RuntimeException {

    private final String workspaceId;

    public LinkedWorkspaceNotFoundException(String workspaceId) {
        super("Cannot register model: linked workspace '" + workspaceId + "' does not exist.");
        this.workspaceId = workspaceId;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }
}
