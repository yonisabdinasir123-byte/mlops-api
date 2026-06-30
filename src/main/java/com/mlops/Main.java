package com.mlops;

import com.mlops.api.RestApplication;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

/**
 * Bootstraps an embedded Grizzly HTTP server hosting the JAX-RS application.
 *
 * Standalone Grizzly uses the supplied base URI as the context root, so the
 * URI deliberately includes the same "/api/v1" path declared by
 * {@code @ApplicationPath} on {@link RestApplication}. Every endpoint is
 * therefore served under http://localhost:8080/api/v1.
 */
public class Main {

    public static final String BASE_URI = "http://localhost:8080/api/v1/";

    public static HttpServer startServer() {
        // forApplicationClass instantiates RestApplication and reads getClasses().
        ResourceConfig config = ResourceConfig.forApplicationClass(RestApplication.class);
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), config);
    }

    public static void main(String[] args) throws InterruptedException {
        final HttpServer server = startServer();

        System.out.println("================================================");
        System.out.println(" MLOps Pipeline Management API is running");
        System.out.println(" Discovery endpoint: http://localhost:8080/api/v1");
        System.out.println(" Press Ctrl+C to stop the server");
        System.out.println("================================================");

        Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));

        // Block the main thread so the server keeps running.
        Thread.currentThread().join();
    }
}
