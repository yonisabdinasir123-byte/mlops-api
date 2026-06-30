package com.mlops.api;

import com.mlops.exception.mapper.GenericExceptionMapper;
import com.mlops.exception.mapper.LinkedWorkspaceNotFoundExceptionMapper;
import com.mlops.exception.mapper.ModelDeprecatedExceptionMapper;
import com.mlops.exception.mapper.WorkspaceNotEmptyExceptionMapper;
import com.mlops.filter.LoggingFilter;
import com.mlops.resource.DebugResource;
import com.mlops.resource.DiscoveryResource;
import com.mlops.resource.ModelResource;
import com.mlops.resource.WorkspaceResource;

import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashSet;
import java.util.Set;

/**
 * JAX-RS application entry point. The @ApplicationPath annotation establishes
 * the versioned base path for every resource in the API: /api/v1.
 *
 * Resources and providers (exception mappers, filters, JSON provider) are
 * registered explicitly in {@link #getClasses()}.
 */
@ApplicationPath("/api/v1")
public class RestApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();

        // ----- Resources -----
        classes.add(DiscoveryResource.class);
        classes.add(WorkspaceResource.class);
        classes.add(ModelResource.class);
        classes.add(DebugResource.class);

        // ----- Exception mappers -----
        classes.add(WorkspaceNotEmptyExceptionMapper.class);    // 409
        classes.add(LinkedWorkspaceNotFoundExceptionMapper.class); // 422
        classes.add(ModelDeprecatedExceptionMapper.class);      // 403
        classes.add(GenericExceptionMapper.class);              // 500 catch-all

        // ----- Providers / filters -----
        classes.add(JacksonFeature.class); // enables automatic POJO <-> JSON conversion
        classes.add(LoggingFilter.class);  // request/response observability logging

        return classes;
    }
}
