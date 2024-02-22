
package com.sparktechcode.springjpasearch.exceptions;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@RequiredArgsConstructor
public class BaseSparkException extends RuntimeException {

    private final HttpStatus status;
    private final Object error;
    private final String message;
    private Object data;
}
