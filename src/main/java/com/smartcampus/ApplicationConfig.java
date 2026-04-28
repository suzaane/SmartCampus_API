package com.smartcampus;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import javax.ws.rs.ApplicationPath;

//@ApplicationPath("/api/v1")         // This creates the /api/v1 context path
public class ApplicationConfig extends ResourceConfig {
    public ApplicationConfig() {
        // Scan the same packages your old Main was scanning
        packages("com.smartcampus.resource",
                 "com.smartcampus.exception.mapper",
                 "com.smartcampus.filter");
        // Enable JSON conversion
        register(JacksonFeature.class);
    }
}