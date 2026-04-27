package com.smartcampus.exception;

/**
 * Thrown when a sensor in MAINTENANCE is asked to accept a new reading.
 * Mapped to HTTP 403 Forbidden by SensorUnavailableExceptionMapper.
 */
public class SensorUnavailableException extends RuntimeException {

    public SensorUnavailableException(String message) {
        super(message);
    }
}