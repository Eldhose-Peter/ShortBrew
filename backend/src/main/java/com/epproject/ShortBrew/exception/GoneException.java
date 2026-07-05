package com.epproject.ShortBrew.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class GoneException extends RuntimeException {
    public GoneException(String message) {
        super(message);
    }
}
