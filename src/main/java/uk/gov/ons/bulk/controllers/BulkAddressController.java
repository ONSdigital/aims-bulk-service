package uk.gov.ons.bulk.controllers;

import static uk.gov.ons.bulk.util.BulkAddressConstants.BAD_AUX_ADDRESS_FILE_NAME;
import static uk.gov.ons.bulk.util.BulkAddressConstants.BAD_UNIT_ADDRESS_FILE_NAME;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.AuxAddress;
import uk.gov.ons.bulk.entities.UnitAddress;
//import uk.gov.ons.bulk.service.AddressService;
import uk.gov.ons.bulk.util.ValidatedAddress;
import uk.gov.ons.bulk.entities.Job;

import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.HttpRequest;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.charset.Charset;

@Slf4j
@Controller
public class BulkAddressController {

//	@Autowired
//	private AddressService addressService;

	// BigQuery client object provided by our autoconfiguration.
	@Autowired
	BigQuery bigquery;

	@Value("${aims.gcp.bucket}")
	private String gcsBucket;
	
	@Value("${aims.elasticsearch.cluster.fat-enabled}")
	private boolean fatClusterEnabled;
	
	@Value("${aims.display.limit}")
	private int displayLimit;

	@GetMapping(value = "/")
	@ResponseStatus(HttpStatus.OK)
	public String index(Model model) {
		
		model.addAttribute("fatClusterEnabled", fatClusterEnabled);
		
		return "index";
	}

	@GetMapping(value = "/jobs")
	public String getBulkRequestProgress(Model model) {

			try {
		String query = "SELECT * FROM ons-aims-initial-test.bulk_status.bulkinfo;";
		QueryJobConfiguration queryConfig =
				QueryJobConfiguration.newBuilder(query).build();

		// Run the query using the BigQuery object
	//	for (FieldValueList row : bigquery.query(queryConfig).iterateAll()) {
	//		for (FieldValue val : row) {
	//			System.out.println(val);
	//		}
	//	}

	//	TableResult jobs = bigquery.query(queryConfig);


		ArrayList<Job> joblist = new ArrayList<Job>();
		for (FieldValueList row : bigquery.query(queryConfig).iterateAll()) {
			Job nextJob = new Job();
			nextJob.setRunid(row.get("runid").getStringValue());
			nextJob.setUserid(row.get("userid").getStringValue());
			nextJob.setStatus(row.get("status").getStringValue());
			nextJob.setTotalrecs(row.get("totalrecs").getStringValue());
			nextJob.setRecssofar(row.get("recssofar").getStringValue());
			joblist.add(nextJob);
		}

		model.addAttribute("jobslist",joblist);

	} catch (Exception ex) {
		model.addAttribute("message",
				String.format("An error occurred while processing the CSV file: %s", ex.getMessage()));
		model.addAttribute("status", true);
		return "error";
	}

		return "jobstable";
	}
	
	@GetMapping(value = "/error")
	@ResponseStatus(HttpStatus.OK)
	public String error(Model model) {
		return "error";
	}

//	GetMapping(path = {"/user", "/user/{data}"})
//	public void user(@PathVariable(required=false,name="data") String data,
//					 @RequestParam(required=false) Map<String,String> qparams) {
//		qparams.forEach((a,b) -> {
//			System.out.println(String.format("%s -> %s",a,b));
//		}
//
//		if (data != null) {
//			System.out.println(data);
//		}
//	}

		@GetMapping(path = {"/paramtest", "/paramtest/{data}"})
        public String testVariables(@PathVariable(required=false,name="data") String data,
					  @RequestParam(required=false) Map<String,String> qparams) {
		qparams.forEach((a,b) -> {
			System.out.println(String.format("%s -> %s",a,b));
		});

		if (data != null) {
			System.out.println(data);
		}

		return "progress";
	}



//	@GetMapping(path = "/hello", produces= MediaType.APPLICATION_JSON_VALUE)
//	public ResponseEntity<Object> sayHello()
//	{
//		//Get data from service layer into entityList.
//
//		List<JSONObject> entities = new ArrayList<JSONObject>();
//		for (Entity n : entityList) {
//			JSONObject entity = new JSONObject();
//			entity.put("aa", "bb");
//			entities.add(entity);
//		}
//		return new ResponseEntity<Object>(entities, HttpStatus.OK);
//	}

    @GetMapping(path = "/jsontest", produces= MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<JsonNode> get() throws JsonProcessingException {
		ObjectMapper mapper = new ObjectMapper();
		JsonNode json = mapper.readTree("{\"id\": \"132\", \"name\": \"Alice\"}");
		return ResponseEntity.ok(json);
	}

	@PostMapping(value = "/bulk")
	public String runBulkRequest(@RequestBody String addressesJson, Model model) {
	//	model.addAttribute("status", true);
		// Create dataset UUID
		// get highest ID so far

		// Create results table for UUID
		// Create one cloud task for each address
		// Execute task with backoff / throttling
		// Capture results in BigQuery table
		return "request-id";
	}

	@GetMapping(value = "/single")
	public String runTestRequest(Model model) {
	 //   model.addAttribute("status", false);
		// Create dataset UUID
		try {
			String query = "SELECT MAX(runid) FROM ons-aims-initial-test.bulk_status.bulkinfo;";
			QueryJobConfiguration queryConfig =
					QueryJobConfiguration.newBuilder(query).build();
            long newKey = 0;
				for (FieldValueList row : bigquery.query(queryConfig).iterateAll()) {
					for (FieldValue val : row) {
						newKey = val.getLongValue() + 1;
						System.out.println(newKey);
					}
				}
			// Create results table for UUID
			String datasetName = "bulk_status";
			String tableName = "bulkresults" + newKey;
			Schema schema =
					Schema.of(
							Field.of("id", StandardSQLTypeName.INT64),
							Field.of("inputaddress", StandardSQLTypeName.STRING),
							Field.of("uprn", StandardSQLTypeName.INT64),
							Field.of("address", StandardSQLTypeName.STRING),
							Field.of("score", StandardSQLTypeName.FLOAT64)
					);
			createTable(datasetName, tableName, schema);
		} catch (Exception ex) {
			model.addAttribute("message",
					String.format("An error occurred while processing the CSV file: %s", ex.getMessage()));
			model.addAttribute("status", true);
			return "error";
		}


		// Create one cloud task for each address
		// Execute task with backoff / throttling
		// Capture results in BigQuery table
		return "progress";
	}

	@GetMapping(value = "/bulk-progress")
	public String getBulkRequestProgress(String requestId,Model model) {


		String projectId = "ons-aims-initial-test";
		String locationId = "test-queue";
		String queueId = "europe-west2";

		try {
		    createTask(projectId, locationId, queueId);
		} catch (Exception ex) {
			model.addAttribute("message",
					String.format("An error occurred while processing the CSV file: %s", ex.getMessage()));
			model.addAttribute("status", true);
			return "error";
		}
	//	model.addAttribute("message", "unexpected error");
	//	model.addAttribute("status", false);
	//	String curi = "https://europe-west2-ons-aims-initial-test.cloudfunctions.net/create-cloud-task";
     //   String curi = "https://europe-west2-ons-aims-initial-test.cloudfunctions.net/test-http-func";
    //    RestTemplate restTemplate = new RestTemplate();
//		String result = restTemplate.getForObject(curi,String.class);
	//	System.out.println(result);
	//	model.addAttribute("status", true);
		// get count of completed queries for given requestId
		// compare with number of queries requested in order to give progress
		// give ETA ?
		return "progress";
	}

	@GetMapping(value = "/bulk-result")
	public String getBulkResults(String requestId,Model model) {
	//	model.addAttribute("status", false);
		// fetch contents of results table for requestId
		// present contents as JSON response
		return "7 Gate Reach";
	}

	public void createTable(String datasetName, String tableName, Schema schema) {
		try {
			// Initialize client that will be used to send requests. This client only needs to be created
			// once, and can be reused for multiple requests.
		//	BigQuery bigquery = BigQueryOptions.getDefaultInstance().getService();

			TableId tableId = TableId.of(datasetName, tableName);
			TableDefinition tableDefinition = StandardTableDefinition.of(schema);
			TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();

			bigquery.create(tableInfo);
			System.out.println("Table created successfully");
		} catch (BigQueryException e) {
			System.out.println("Table was not created. \n" + e.toString());
		}
	}



//	public class CreateHttpTask {





		// Create a task with a HTTP target using the Cloud Tasks client.
		public static void createTask(String projectId, String locationId, String queueId)
				throws IOException {

			// Instantiates a client.
			try (CloudTasksClient client = CloudTasksClient.create()) {
				String url = "https://example.com/taskhandler";
				String payload = "Hello, World!";

				// Construct the fully qualified queue name.
				String queuePath = QueueName.of(projectId, locationId, queueId).toString();

				// Construct the task body.
				Task.Builder taskBuilder =
						Task.newBuilder()
								.setHttpRequest(
										HttpRequest.newBuilder()
												.setBody(ByteString.copyFrom(payload, Charset.defaultCharset()))
												.setUrl(url)
												.setHttpMethod(HttpMethod.POST)
												.build());

				// Send create task request.
				Task task = client.createTask(queuePath, taskBuilder.build());
				System.out.println("Task created: " + task.getName());
			}
		}
//	}

//	@PostMapping(value = "/upload-csv-aux-file")
//	public String uploadCSVAuxFile(@RequestParam(name = "file") MultipartFile file, Model model) {
//
//		/*
//		 * TODO: Reactify the web page to stream the results of the add operation.
//		 */
//
//		// validate file
//		if (file.isEmpty()) {
//			model.addAttribute("message", "Select a CSV file to upload and an Index to load to.");
//			model.addAttribute("status", false);
//		} else {
//
//			try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
//
//				CsvToBean<AuxAddress> csvToBean = new CsvToBeanBuilder<AuxAddress>(reader).withType(AuxAddress.class)
//						.withIgnoreLeadingWhiteSpace(true).build();
//
//				List<ValidatedAddress<AuxAddress>> validatedAddresses = csvToBean.parse().stream()
//						.map(address -> new ValidatedAddress<AuxAddress>(address)).collect(Collectors.toList());
//
//				List<ValidatedAddress<AuxAddress>> invalidAddresses = validatedAddresses.stream()
//						.filter(address -> !address.isValid()).collect(Collectors.toList());
//
//				if (invalidAddresses.size() > 0) {
//					model.addAttribute("badAddressCSVPath", String.format("Bad addresss file name: %s. In bucket: %s",
//							addressService.writeBadAddressesCsv(invalidAddresses, BAD_AUX_ADDRESS_FILE_NAME), gcsBucket));
//					model.addAttribute("badAddresses", invalidAddresses.stream().limit(displayLimit).collect(Collectors.toList()));
//					model.addAttribute("badAddressSize", String.format("Total invalid aux addresses: %d", invalidAddresses.size()));
//				}
//
//				List<ValidatedAddress<AuxAddress>> validAddresses = validatedAddresses.stream()
//						.filter(address -> address.isValid()).collect(Collectors.toList());
//
//				if (validAddresses.size() > 0) {
//					model.addAttribute("addresses", validAddresses.stream().limit(displayLimit).collect(Collectors.toList()));
//					model.addAttribute("addressesSize", String.format("Total valid aux addresses: %d", validAddresses.size()));
//
//
//					// Add the good addresses to Elasticsearch
//					addressService.createAuxAddressesFromCsv(validAddresses).doOnNext(output -> {
//						log.debug(String.format("Added: %s", output.toString()));
//
//						/*
//						 * This is very basic at the moment and just returns to the view the addresses
//						 * that were attempted to load into ES. It won't show any that failed. For
//						 * example an illegal lat or long will cause the load to fail from that point.
//						 * Needs very clean input data.
//						 */
//					}).subscribe();
//				}
//
//				model.addAttribute("status", true);
//			} catch (Exception ex) {
//				model.addAttribute("message",
//						String.format("An error occurred while processing the CSV file: %s", ex.getMessage()));
//				model.addAttribute("status", false);
//				return "error";
//			}
//		}
//
//		return "file-upload-status";
//	}

//	@PostMapping(value = "/upload-csv-unit-file")
//	public String uploadCSVUnitFile(@RequestParam("file") MultipartFile file, Model model) {
//
//		/*
//		 * TODO: Reactify the web page to stream the results of the add operation.
//		 */
//		// validate file
//		if (file.isEmpty()) {
//			model.addAttribute("message", "Select a CSV file to upload and an Index to load to.");
//			model.addAttribute("status", false);
//		} else {
//
//			try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
//
//				CsvToBean<UnitAddress> csvToBean = new CsvToBeanBuilder<UnitAddress>(reader).withType(UnitAddress.class)
//						.withIgnoreLeadingWhiteSpace(true)
//						.withIgnoreEmptyLine(true)
//						.withSeparator('|').build();
//
//				List<ValidatedAddress<UnitAddress>> validatedAddresses = csvToBean.parse().stream()
//						.map(address -> new ValidatedAddress<UnitAddress>(address)).collect(Collectors.toList());
//
//				List<ValidatedAddress<UnitAddress>> invalidAddresses = validatedAddresses.stream()
//						.filter(address -> !address.isValid()).collect(Collectors.toList());
//
//				if (invalidAddresses.size() > 0) {
//					model.addAttribute("badAddressCSVPath", String.format("Bad addresss file name: %s. In bucket: %s",
//							addressService.writeBadAddressesCsv(invalidAddresses, BAD_UNIT_ADDRESS_FILE_NAME), gcsBucket));
//					model.addAttribute("badAddresses", invalidAddresses.stream().limit(displayLimit).collect(Collectors.toList()));
//					model.addAttribute("badAddressSize", String.format("Total invalid unit addresses: %d", invalidAddresses.size()));
//				}
//
//				List<ValidatedAddress<UnitAddress>> validAddresses = validatedAddresses.stream()
//						.filter(address -> address.isValid()).collect(Collectors.toList());
//
//				if (validAddresses.size() > 0) {
//					model.addAttribute("addresses", validAddresses.stream().limit(displayLimit).collect(Collectors.toList()));
//					model.addAttribute("addressesSize", String.format("Total valid unit addresses: %d", validAddresses.size()));
//
//					/* Add the good addresses to Elasticsearch
//					 * This is very basic at the moment and just returns to the view the addresses
//					 * that were attempted to load into ES. It won't show any that failed. For
//					 * example an illegal lat or long will cause the load to fail from that point.
//					 * Needs very clean input data.
//					 */
//					if (fatClusterEnabled) {
//						addressService.createFatUnitAddressesFromCsv(validAddresses).doOnNext(output -> {
//							log.debug(String.format("Added: %s", output.toString()));
//						}).subscribe();
//					} else {
//						addressService.createSkinnyUnitAddressesFromCsv(validAddresses).doOnNext(output -> {
//							log.debug(String.format("Added: %s", output.toString()));
//						}).subscribe();
//					}
//				}
//
//				model.addAttribute("status", true);
//			} catch (Exception ex) {
//				model.addAttribute("message",
//						String.format("An error occurred while processing the CSV file: %s", ex.getMessage()));
//				model.addAttribute("status", false);
//				return "error";
//			}
//		}
//
//		return "unit-address-upload-status";
//	}
}
