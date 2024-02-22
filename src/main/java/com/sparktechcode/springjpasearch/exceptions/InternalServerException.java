package com.sparktechcode.springjpasearch.exceptions;


import org.springframework.http.HttpStatus;

public class InternalServerException extends BaseSparkException {

    private static final HttpStatus DEFAULT_STATUS = HttpStatus.INTERNAL_SERVER_ERROR;

    public InternalServerException() {
        super(DEFAULT_STATUS, SparkError.SERVER_ERROR, "Something went wrong");
    }

    public InternalServerException(Object error, String message) {
        super(DEFAULT_STATUS, error, message);
    }

    public InternalServerException(Object error, String message, Object data) {
        super(DEFAULT_STATUS, error, message);
        setData(data);
    }
}
