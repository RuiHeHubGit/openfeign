package com.openfeign;

public class CallApiException extends RuntimeException{
    private Object data;
    private int httpStatus;

    public CallApiException(int httpStatus, Object data) {
        super("http error code "+httpStatus);
        this.data = data;
        this.httpStatus = httpStatus;
    }

    public Object getData() {
        return data;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
