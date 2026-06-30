package com.mlops.exception.mapper;

import com.mlops.exception.ErrorResponse;
import com.mlops.exception.LinkedWorkspaceNotFoundException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Maps {@link LinkedWorkspaceNotFoundException} to HTTP 422 Unprocessable Entity.
 *
 * 422 is used (rather than 404) because the failure is a semantic validation
 * problem with the request body, not a missing target resource at this URI.
 * The numeric status is used directly because the javax.ws.rs Response.Status
 * enum does not define a 422 constant.
 */
@Provider
public class LinkedWorkspaceNotFoundExceptionMapper implements ExceptionMapper<LinkedWorkspaceNotFoundException> {

    private static final int UNPROCESSABLE_ENTITY = 422;

    @Override
    public Response toResponse(LinkedWorkspaceNotFoundException ex) {
        ErrorResponse body = new ErrorResponse(UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage());
        return Response.status(UNPROCESSABLE_ENTITY)
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
