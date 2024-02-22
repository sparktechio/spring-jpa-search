package com.sparktechcode.springjpasearch.exceptions;


import org.springframework.http.HttpStatus;

public class BadRequestException extends BaseSparkException {

    private static final HttpStatus DEFAULT_STATUS = HttpStatus.BAD_REQUEST;

    public BadRequestException() {
        super(DEFAULT_STATUS, SparkError.BAD_REQUEST, "Invalid request.");
    }

    public BadRequestException(String message) {
        super(DEFAULT_STATUS, SparkError.BAD_REQUEST, message);
    }

    public BadRequestException(Object error, String message) {
        super(DEFAULT_STATUS, error, message);
    }

    public BadRequestException(Object error, Throwable throwable) {
        super(DEFAULT_STATUS, error, throwable.getMessage());
        initCause(throwable);
    }

    public BadRequestException(Object error, String message, Object data) {
        super(DEFAULT_STATUS, error, message);
        setData(data);
    }
}
