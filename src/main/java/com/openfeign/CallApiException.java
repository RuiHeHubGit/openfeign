package com.openfeign;

import feign.Response;

public class CallApiException extends RuntimeException {
    private Response response;

    public CallApiException(Response response) {
        super("http error code " + response.status());
        this.response = response;
    }

    public Response getCopyResponse() {
        return response;
    }
}
