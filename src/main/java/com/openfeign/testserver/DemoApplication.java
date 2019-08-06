package com.openfeign.testserver;

import com.openfeign.testserver.entities.Record;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@SpringBootApplication
@RestController
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	private static ConcurrentHashMap<Long, Record> repository = new ConcurrentHashMap<>();
	private static AtomicLong index = new AtomicLong();

	@PostMapping("/uid/{uid}/record")
	public synchronized Object createRecord(@PathVariable("uid") Long uid,
											@RequestBody Record record) {
		record.setId(index.getAndIncrement());
		repository.put(record.getId(), record);
		return record;
	}

	@PutMapping("/uid/{uid}/record/{cid}")
	public synchronized Object updateRecord(@PathVariable("uid") Long uid,
											@PathVariable("cid") Long cid,
											@RequestParam("content") String content,
											HttpServletResponse response) {
		Map result = new HashMap();
		Record record = repository.get(cid);
		if(record == null) {
			response.setStatus(400);
			ErrorType errorType = new ErrorType();
			errorType.setErrorCode("11404");
			errorType.setDescribe("Invalid record id");
			return errorType;
		} else {
			record.setContent(content);
			repository.put(record.getId(), record);
			return record;
		}
	}

	@GetMapping("/uid/{uid}/record/{cid}")
	public synchronized Object getRecord(@PathVariable("uid") Long uid,
											@PathVariable("cid") Long cid,
											HttpServletResponse response) {
		Map result = new HashMap();
		Record record = repository.get(cid);
		if(record == null) {
			response.setStatus(400);
			ErrorType errorType = new ErrorType();
			errorType.setErrorCode("11404");
			errorType.setDescribe("Invalid record id");
			return errorType;
		} else {
			result.put("record", record);
		}
		return result;
	}

	@GetMapping("/uid/{uid}/record")
	public synchronized Object getAllRecordOfUser(@PathVariable("uid") Long uid) {
		Map result = new HashMap();
		List<Record> recordList = new ArrayList<>();
		Collection<Record> records = repository.values();
		for (Record record : records) {
			if(uid.equals(record.getUserId())) {
				recordList.add(record);
			}
		}
		result.put("recordList", recordList);
		return result;
	}

	@DeleteMapping("/uid/{uid}/record/{cid}")
	public synchronized Object deleteRecord(@PathVariable("uid") Long uid,
										 @PathVariable("cid") Long cid,
										 HttpServletResponse response) {
		Map result = new HashMap();
		Record record = repository.get(cid);
		if(record == null) {
			response.setStatus(400);
			ErrorType errorType = new ErrorType();
			errorType.setErrorCode("11404");
			errorType.setDescribe("Invalid record id");
			return errorType;
		} else {
			repository.remove(cid);
		}
		return result;
	}

}
