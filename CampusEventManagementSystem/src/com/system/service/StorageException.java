package com.system.service;

/**
 * Thrown when a text file cannot be read or written. It extends IllegalStateException
 * so existing GUI handlers that already catch IllegalStateException show it as a normal error message.
 */
public class StorageException extends IllegalStateException {
    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
