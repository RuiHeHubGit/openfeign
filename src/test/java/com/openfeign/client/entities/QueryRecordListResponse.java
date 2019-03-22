package com.openfeign.client.entities;

import com.openfeign.BaseResponse;

import java.util.List;

public class QueryRecordListResponse extends BaseResponse {
    private List<Record> recordList;

    public List<Record> getRecordList() {
        return recordList;
    }

    public void setRecordList(List<Record> recordList) {
        this.recordList = recordList;
    }

    @Override
    public ErrorType getError() {
        return (ErrorType) super.getError();
    }
}
