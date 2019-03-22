package com.openfeign;

public class BaseResponse {
    private int httpStatus;
    private Object error;

    public BaseResponse init(int httpStatus, Object error) {
        this.httpStatus = httpStatus;
        this.error = error;
        return this;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public void setHttpStatus(int httpStatus) {
        this.httpStatus = httpStatus;
    }

    public Object getError() {
        return error;
    }

    public void setError(Object error) {
        this.error = error;
    }
}
