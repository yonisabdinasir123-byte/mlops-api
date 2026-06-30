package com.mlops.exception;

/**
 * Thrown when a client attempts to DELETE a workspace that still has one or more
 * models assigned to it. Mapped to HTTP 409 Conflict by
 * {@code WorkspaceNotEmptyExceptionMapper} to prevent orphaned model data.
 */
public class WorkspaceNotEmptyException extends RuntimeException {

    private final String workspaceId;
    private final int modelCount;

    public WorkspaceNotEmptyException(String workspaceId, int modelCount) {
        super("Workspace '" + workspaceId + "' cannot be deleted because it still has "
                + modelCount + " model(s) assigned. Remove or reassign the models first.");
        this.workspaceId = workspaceId;
        this.modelCount = modelCount;
    }

    public String getWorkspaceId() {
        return workspaceId;
    }

    public int getModelCount() {
        return modelCount;
    }
}
