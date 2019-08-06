``` JAVA
@Headers({"Accept: application/json", "Content-Type: application/x-www-form-urlencoded"})
public interface RecordApiClient {
    @RequestLine("POST /uid/{uid}/record")
    @Headers("Content-Type: application/json")
    BaseResponse<Record, ErrorType> postRecord(@Param("uid") Long uid, Record record);

    @RequestLine("PUT /uid/{uid}/record/{cid}")
    @Body("content={content}")
    BaseResponse<Record, ErrorType> putRecord(@Param("uid") Long uid, @Param("cid") Long cid, @Param("content") String content);

    @RequestLine("GET /uid/{uid}/record/{cid}")
    BaseResponse<Record, ErrorType> getRecord(@Param("uid") Long uid, @Param("cid") Long cid);

    @RequestLine("GET /uid/{uid}/record")
    BaseResponse<RecordList, ErrorType> getRecordsOfUser(@Param("uid") Long uid);

    @RequestLine("DELETE /uid/{uid}/record/{cid}")
    BaseResponse<Record, ErrorType> deleteRecord(@Param("uid") Long uid, @Param("cid") Long cid);
}

```

```JAVA
@Test
public void test() {
		ClientFactory factory = ClientFactory.Builder()
            .defaultBaseUrl("http://localhost:8080")
            .build();
		RecordApiClient client = factory.createJsonClient(RecordApiClient.class, null);
		...
}
```

