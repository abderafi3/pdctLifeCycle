package com.checkmk.pdctLifeCycle.exception;

public class HostServiceException extends Exception {
    public HostServiceException(String message) {
        super(message);
    }

    public HostServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}

