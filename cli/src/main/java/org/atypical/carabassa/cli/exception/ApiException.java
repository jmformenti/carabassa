package org.atypical.carabassa.cli.exception;

import org.springframework.web.client.RestClientResponseException;

public class ApiException extends Exception {

    private static final long serialVersionUID = -2248750069186985643L;

    public ApiException(String message) {
        super(message);
    }

    public ApiException(RestClientResponseException e) {
        super(e);
    }

}
