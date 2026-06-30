package com.mlops.exception.mapper;

import com.mlops.exception.ErrorResponse;
import com.mlops.exception.WorkspaceNotEmptyException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps {@link WorkspaceNotEmptyException} to an HTTP 409 Conflict with a clear
 * JSON body explaining the data-integrity constraint that was violated.
 */
@Provider
public class WorkspaceNotEmptyExceptionMapper implements ExceptionMapper<WorkspaceNotEmptyException> {

    @Override
    public Response toResponse(WorkspaceNotEmptyException ex) {
        ErrorResponse body = new ErrorResponse(
                Response.Status.CONFLICT.getStatusCode(),
                "Conflict",
                ex.getMessage());
        return Response.status(Response.Status.CONFLICT)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
