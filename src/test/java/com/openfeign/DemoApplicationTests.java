package com.openfeign;

import com.openfeign.client.RecordApiClient;
import com.openfeign.client.entities.ErrorType;
import com.openfeign.client.entities.QueryRecordListResponse;
import com.openfeign.client.entities.Record;
import com.openfeign.client.entities.RecordResponse;
import com.openfeign.testserver.DemoApplication;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = DemoApplication.class)
public class DemoApplicationTests {

	@Test
	public void test() {
		ClientFactory factory = ClientFactory.Builder()
				.errorType(ErrorType.class)
				.defaultBaseUrl("http://localhost:8080").build();
		RecordApiClient client = factory.createJsonClient(RecordApiClient.class, null);

		Record record = new Record();
		record.setUserId(1L);
		record.setContent("record1");
		RecordResponse response = client.postRecord(1L, record);
		System.out.println(response.getRecord());
		record.setId(response.getRecord().getId());

		response = client.putRecord(1L, response.getRecord().getId(), "update1");
		System.out.println(response.getRecord());

		response = client.getRecord(1L, response.getRecord().getId());
		System.out.println(response.getRecord());

		record.setUserId(1L);
		record.setContent("record2");
		response = client.postRecord(1L, record);
		System.out.println(response.getRecord());

		QueryRecordListResponse queryRecordListResponse = client.getRecordsOfUser(1L);
		System.out.println(queryRecordListResponse.getRecordList());

		response = client.deleteRecord(1L, record.getId());
		System.out.println(response.getHttpStatus());

		response = client.getRecord(1L, record.getId());
		System.out.println(response.getHttpStatus());
		System.out.println("error:"+response.getError().getErrorCode()+","+response.getError().getDescribe());
	}

}
