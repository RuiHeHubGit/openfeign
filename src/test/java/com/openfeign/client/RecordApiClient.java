package com.openfeign.client;

import com.openfeign.ApiClient;
import com.openfeign.client.entities.QueryRecordListResponse;
import com.openfeign.client.entities.Record;
import com.openfeign.client.entities.RecordResponse;
import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;

@Headers({"Accept: application/json", "Content-Type: application/x-www-form-urlencoded"})
public interface RecordApiClient extends ApiClient {
    @RequestLine("POST /uid/{uid}/record")
    @Headers("Content-Type: application/json")
    RecordResponse postRecord(@Param("uid") Long uid, Record record);

    @RequestLine("PUT /uid/{uid}/record/{cid}")
    @Body("content={content}")
    RecordResponse putRecord(@Param("uid") Long uid, @Param("cid") Long cid, @Param("content") String content);

    @RequestLine("GET /uid/{uid}/record/{cid}")
    RecordResponse getRecord(@Param("uid") Long uid, @Param("cid") Long cid);

    @RequestLine("GET /uid/{uid}/record")
    QueryRecordListResponse getRecordsOfUser(@Param("uid") Long uid);

    @RequestLine("DELETE /uid/{uid}/record/{cid}")
    RecordResponse deleteRecord(@Param("uid") Long uid, @Param("cid") Long cid);
}
