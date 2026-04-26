package com.smartcampus.exception;

/**
 * Thrown when a required linked resource (like a room) is not found.
 * The exception mapper returns HTTP 422.
 */
public class LinkedResourceNotFoundException extends RuntimeException {

    public LinkedResourceNotFoundException(String message) {
        super(message);
    }
}