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
import com.google.cloud.tasks.v2.*;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

import com.google.protobuf.ByteString;
import java.io.IOException;
import java.nio.charset.Charset;

import org.springframework.core.env.Environment;

@Slf4j
@Controller
public class BulkAddressController {

//	@Autowired
//	private AddressService addressService;

	@Autowired
	private Environment env;

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
		String locationId = "europe-west2";
		String queueId = "test-queue";

		try {
		    createTask(projectId, locationId, queueId);
		} catch (Exception ex) {
			model.addAttribute("message",
					String.format("An error occurred while processing the CSV file: %s", ex.getMessage()));
			model.addAttribute("status", true);
			return "error";
		}

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
		return "progress";
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


		// Create a task with a HTTP target using the Cloud Tasks client.
		public void createTask(String projectId, String locationId, String queueId)
				throws IOException {

			// Instantiates a client.
			try (CloudTasksClient client = CloudTasksClient.create()) {
				String creds = env.getProperty("GOOGLE_APPLICATION_CREDENTIALS");
				String url = "https://europe-west2-ons-aims-initial-test.cloudfunctions.net/api-call-http-function";
				//String url = "https://initial-test-open-api.aims.gcp.onsdigital.uk/addresses/bulk";
				String jsonString = "{'jobId':'job2','id':'1','address':'7 Gate Reach'}";
				JsonParser jsonParser = new JsonParser();
				JsonObject payload = (JsonObject) jsonParser.parse(jsonString);

			//	ObjectMapper mapper = new ObjectMapper();
			//	JsonNode payload = mapper.readTree("{\"jobId\":\"job2\",\"id\":\"1\",\"address\":\"7 gate reach exeter\"}");
			//	String payload = "{\"jobId\":\"job2\",\"id\":\"1\",\"address\":\"7 gate reach exeter\"}";
			//	ArrayList<String> plList =
			//	String url = "https://example.com/taskhandler";
			//	String payload = "Hello, World!";
				String serviceAccountEmail =
						"spring-boot-bulk-service@ons-aims-initial-test.iam.gserviceaccount.com";
				// Construct the fully qualified queue name.
				String queuePath = QueueName.of(projectId, locationId, queueId).toString();
				OidcToken.Builder oidcTokenBuilder =
						OidcToken.newBuilder().setServiceAccountEmail(serviceAccountEmail);
				// Construct the task body.
				Task.Builder taskBuilder =
						Task.newBuilder()
								.setHttpRequest(
										HttpRequest.newBuilder()
												.setBody(ByteString.copyFrom(payload.toString(), Charset.defaultCharset()))
												.setUrl(url)
												.putHeaders("Content-Type","application/json")
												.setHttpMethod(HttpMethod.POST)
												.setOidcToken(oidcTokenBuilder)
												.build());

				// Send create task request.
				Task task = client.createTask(queuePath, taskBuilder.build());
				System.out.println("Task created: " + task.getName());
			}
		}
}
