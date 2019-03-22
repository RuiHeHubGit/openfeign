package com.openfeign.client.entities;

import com.openfeign.BaseResponse;

public class RecordResponse extends BaseResponse {
    private Record record;

    public Record getRecord() {
        return record;
    }

    public void setRecord(Record record) {
        this.record = record;
    }

    @Override
    public ErrorType getError() {
        return (ErrorType) super.getError();
    }
}
