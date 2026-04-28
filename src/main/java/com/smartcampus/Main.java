package com.smartcampus;

import java.io.IOException;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    // The base URI only sets the server location. The /api/v1 prefix comes from
    // Applicatio nConfig's @ApplicationPath annotation.
    private static final URI BASE_URI = URI.create("http://localhost:8080/api/v1/");
    private static final Logger LOG = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try {
            // Load our custom ApplicationConfig which already scans the correct packages
            ResourceConfig config = ResourceConfig.forApplication(new ApplicationConfig());

            HttpServer server = GrizzlyHttpServerFactory.createHttpServer(BASE_URI, config, false);
            server.start();

            LOG.info("Smart Campus API running at http://localhost:8080/api/v1/");
            LOG.info("Discovery : GET http://localhost:8080/api/v1/");
            LOG.info("Rooms     : GET http://localhost:8080/api/v1/rooms");
            LOG.info("Sensors   : GET http://localhost:8080/api/v1/sensors");
            System.out.println("Press ENTER to stop...");
            System.in.read();
            server.shutdownNow();
            LOG.info("Server stopped.");

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Server failed to start: {0}", e.getMessage());
        }
    }
}