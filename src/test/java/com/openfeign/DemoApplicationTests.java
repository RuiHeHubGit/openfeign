package com.openfeign;

import com.openfeign.client.RecordApiClient;
import com.openfeign.testserver.ErrorType;
import com.openfeign.client.entities.Record;
import com.openfeign.client.entities.RecordList;
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
				.defaultBaseUrl("http://localhost:8080").build();
		RecordApiClient client = factory.createJsonClient(RecordApiClient.class, null);

		Record record = new Record();
		record.setUserId(1L);
		record.setContent("record1");
		BaseResponse<Record, ErrorType> response = client.postRecord(1L, record);
		System.out.println(response.getData());
		record.setId(response.getData().getId());

		response = client.putRecord(1L, response.getData().getId(), "update1");
		System.out.println(response.getData());

		response = client.getRecord(1L, response.getData().getId());
		System.out.println(response.getData());

		record.setUserId(1L);
		record.setContent("record2");
		response = client.postRecord(1L, record);
		System.out.println(response.getData());

		BaseResponse<RecordList, ErrorType> queryRecordListResponse = client.getRecordsOfUser(1L);
		System.out.println(queryRecordListResponse.getData());

		response = client.deleteRecord(1L, record.getId());
		System.out.println(response.getStatus());

		response = client.getRecord(1L, record.getId());
		System.out.println(response.getStatus());
		System.out.println("error:"+response.getError().getErrorCode()+","+response.getError().getDescribe());
	}

}
