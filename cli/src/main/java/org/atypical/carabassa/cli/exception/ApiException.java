package org.atypical.carabassa.cli.exception;

import org.springframework.web.reactive.function.client.WebClientResponseException;

public class ApiException extends Exception {

    private static final long serialVersionUID = -2248750069186985643L;

    public ApiException(String message) {
        super(message);
    }

    public ApiException(WebClientResponseException e) {
        super(e);
    }

}
