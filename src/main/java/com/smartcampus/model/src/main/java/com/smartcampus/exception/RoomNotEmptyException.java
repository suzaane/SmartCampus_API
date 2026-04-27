package com.smartcampus.exception;

/**
 * Thrown when trying to delete a room that still has sensors.
 * Results in HTTP 409 Conflict.
 */
public class RoomNotEmptyException extends RuntimeException {

    public RoomNotEmptyException(String message) {
        super(message);
    }
}