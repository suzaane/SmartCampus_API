package com.smartcampus;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import javax.ws.rs.ApplicationPath;

@ApplicationPath("/api/v1")
public class ApplicationConfig extends ResourceConfig {

    public ApplicationConfig() {
        packages(
            "com.smartcampus.resource",
            "com.smartcampus.exception.mapper",
            "com.smartcampus.filter"
        );
        register(JacksonFeature.class);
    }
}