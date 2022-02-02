package uk.gov.ons.bulk.controllers;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryError;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.InsertAllRequest;
import com.google.cloud.bigquery.InsertAllResponse;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.StandardTableDefinition;
import com.google.cloud.bigquery.TableDefinition;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableInfo;
import com.google.cloud.tasks.v2.CloudTasksClient;
import com.google.cloud.tasks.v2.HttpMethod;
import com.google.cloud.tasks.v2.HttpRequest;
import com.google.cloud.tasks.v2.OidcToken;
import com.google.cloud.tasks.v2.QueueName;
import com.google.cloud.tasks.v2.Task;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.protobuf.ByteString;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.BulkRequest;
import uk.gov.ons.bulk.entities.BulkRequestContainer;
import uk.gov.ons.bulk.entities.Job;
import uk.gov.ons.bulk.entities.Result;
import uk.gov.ons.bulk.entities.ResultContainer;

import javax.annotation.PostConstruct;

@Slf4j
@Controller
public class BulkAddressController {

	// BigQuery client object provided by our autoconfiguration.
	@Autowired
	BigQuery bigquery;

	@Value("${spring.cloud.gcp.project-id}")
	private String projectId;
	
	@Value("${spring.cloud.gcp.bigquery.dataset-name}")
	private String datasetName;  
	
	@Value("${aims.bigquery.info-table}")
	private String infoTable;  
	
	@Value("${aims.cloud-functions.api-call-function}")
	private String apiCallFunction;

	private String BASE_DATASET_QUERY;
	private String INFO_TABLE_QUERY;
	private String JOBS_QUERY;
	private String JOB_QUERY;

	@PostConstruct
	public void postConstruct() {

	BASE_DATASET_QUERY = new StringBuilder()
			.append("SELECT * FROM ")
			.append(projectId)
			.append(".")
			.append(datasetName).toString();
	
	INFO_TABLE_QUERY = new StringBuilder()
			.append(BASE_DATASET_QUERY)
			.append(".")
			.append(infoTable).toString(); 
	
	JOBS_QUERY = new StringBuilder()
			.append(INFO_TABLE_QUERY)
			.append(";").toString();
	
	JOB_QUERY = new StringBuilder()
			.append(INFO_TABLE_QUERY)
			.append(" WHERE runid = %s;").toString();
	}

	@GetMapping(value = "/")
	@ResponseStatus(HttpStatus.OK)
	public String index(Model model) {
		return "index";
	}

	public Iterable<FieldValueList> runQuery(QueryJobConfiguration queryConfig) throws  java.lang.InterruptedException {

		return bigquery.query(queryConfig).iterateAll();

	}

	@GetMapping(value = "/jobs")
	public String getAllBulkRequestProgress(Model model) {
		try {
			QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(JOBS_QUERY).build();

			ArrayList<Job> joblist = new ArrayList<Job>();
			for (FieldValueList row : runQuery(queryConfig)) {
			//for (FieldValueList row : bigquery.query(queryConfig).iterateAll()) {
				Job nextJob = new Job();
				nextJob.setRunid(row.get("runid").getStringValue());
				nextJob.setUserid(row.get("userid").getStringValue());
				nextJob.setStatus(row.get("status").getStringValue());
				nextJob.setTotalrecs(row.get("totalrecs").getStringValue());
				nextJob.setRecssofar(row.get("recssofar").getStringValue());
				joblist.add(nextJob);
			}

			model.addAttribute("jobslist", joblist);

		} catch (Exception ex) {
			model.addAttribute("message", String.format("An error occurred : %s", ex.getMessage()));
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



	@PostMapping(value = "/bulk")
	public String runBulkRequest(@RequestBody String addressesJson, Model model) {

		/*
		 * We are using a single Dataset for the bulk service which makes gathering info on each bulk run easier.
		 * We need a more robust method of naming the tables for each bulk run.
		 * Should probably use UUID.randomUUID() or similar.
		 * Don't need to look up the latest ID used if using UUID. 
		 */
		
		// Create dataset UUID
		Long jobId = 0L;
		BulkRequestContainer bcont;
		try {

			ObjectMapper objectMapper = new ObjectMapper();
			// List<BulkRequest> bulkAdds = objectMapper.readValue(addressesJson,
			// objectMapper.getTypeFactory().constructCollectionType(List.class,
			// BulkRequest.class));
			bcont = objectMapper.readValue(addressesJson, BulkRequestContainer.class);
			int recs = bcont.getAddresses().length;

			String query = "SELECT MAX(runid) FROM ons-aims-initial-test.bulk_status.bulkinfo;";
			QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
			long newKey = 0;
			for (FieldValueList row : bigquery.query(queryConfig).iterateAll()) {
				for (FieldValue val : row) {
					newKey = val.getLongValue() + 1;
					log.info(String.format("newkey:%d", newKey));
				}
			}
			// Create new Job record
			String tableName = "bulkinfo";
			Map<String, Object> row1Data = new HashMap<>();
			row1Data.put("runid", newKey);
			row1Data.put("userid", "bigqueryboy");
			row1Data.put("status", "waiting");
			row1Data.put("totalrecs", recs);
			row1Data.put("recssofar", 0);
			TableId tableId = TableId.of(datasetName, tableName);
			InsertAllResponse response = bigquery
					.insertAll(InsertAllRequest.newBuilder(tableId).addRow("runid", row1Data).build());
			if (response.hasErrors()) {
				// If any of the insertions failed, this lets you inspect the errors
				for (Map.Entry<Long, List<BigQueryError>> entry : response.getInsertErrors().entrySet()) {
					log.error(String.format("entry: %s", entry.toString()));
				}
			}

			tableName = "results" + newKey;
			jobId = newKey;
			model.addAttribute("jobid", newKey);
			Schema schema = Schema.of(
					Field.of("id", StandardSQLTypeName.INT64),
					Field.of("inputaddress", StandardSQLTypeName.STRING),
					Field.of("response", StandardSQLTypeName.STRING));
			createTable(datasetName, tableName, schema);
		} catch (Exception ex) {
			model.addAttribute("message",
					String.format("An error occurred creating results table : %s", ex.getMessage()));
			model.addAttribute("status", true);
			return "error";
		}

		String locationId = "europe-west2";
		String queueId = "test-queue";
		/*
		 * Should set this from a config value. But will be unnecessary when we create the task in a Cloud Function.
		 */
		String serviceAccountEmail = "spring-boot-bulk-service@ons-aims-initial-test.iam.gserviceaccount.com";

		BulkRequest[] adds = bcont.getAddresses();
		for (int i = 0; i < adds.length; i++) {

			String id = adds[i].getId();
			String input = adds[i].getAddress();
			try {
				createTask(projectId, locationId, queueId, jobId.toString(), serviceAccountEmail, id, input);
			} catch (Exception ex) {
				model.addAttribute("message",
						String.format("An error occurred creating the cloud task : %s", ex.getMessage()));
				model.addAttribute("status", true);
				return "error";
			}
		}

		return "submitted";
	}

	@GetMapping(value = "/bulk-progress/{jobid}")
	public String getBulkRequestProgress(@PathVariable(required = true, name = "jobid") String jobid, Model model) {

		try {
			
			QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(String.format(JOB_QUERY, jobid)).build();

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

			model.addAttribute("jobslist", joblist);

		} catch (Exception ex) {
			model.addAttribute("message", String.format("An error occurred : %s", ex.getMessage()));
			model.addAttribute("status", true);
			return "error";
		}

		return "jobstable";
	}

	@GetMapping(value = "/bulk-result/{jobid}", produces = "application/json")
	public @ResponseBody String getBulkResults(@PathVariable(required = true, name = "jobid") String jobid,
			Model model) {

		ArrayList<Result> rlist = new ArrayList<Result>();
		ResultContainer rcont = new ResultContainer();
		try {
			String query = "SELECT * FROM ons-aims-initial-test.bulk_status.results" + jobid + ";";
			QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();

			for (FieldValueList row : bigquery.query(queryConfig).iterateAll()) {
				Result nextResult = new Result();
				nextResult.setId(row.get("id").getStringValue());
				nextResult.setInputaddress(row.get("inputaddress").getStringValue());
				String jsonString = row.get("response").getStringValue();
				ObjectMapper objectMapper = new ObjectMapper();
				Map<String, Object> jsonMap = objectMapper.readValue(jsonString,
						new TypeReference<Map<String, Object>>() {
						});
				nextResult.setResponse(jsonMap);
				rlist.add(nextResult);
			}

		} catch (Exception ex) {
			model.addAttribute("message", String.format("An error occurred : %s", ex.getMessage()));
			model.addAttribute("status", true);
			return "error";
		}
		rcont.setResults(rlist);
		rcont.setJobid(jobid);
		rcont.setStatus("200");
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json2 = gson.toJson(rcont);
		return json2;
	}

	public void createTable(String datasetName, String tableName, Schema schema) {
		try {
			// Initialize client that will be used to send requests. This client only needs
			// to be created
			// once, and can be reused for multiple requests.

			TableId tableId = TableId.of(datasetName, tableName);
			TableDefinition tableDefinition = StandardTableDefinition.of(schema);
			TableInfo tableInfo = TableInfo.newBuilder(tableId, tableDefinition).build();

			bigquery.create(tableInfo);
			log.info(String.format("Table %s created successfully", tableId));
		} catch (BigQueryException e) {
			log.error(String.format("Table was not created. \n %s", e.toString()));
		}
	}

	/*
	 * This method can go when we move it to a Cloud Function
	 */
	// Create a task with a HTTP target using the Cloud Tasks client.
	public void createTask(String projectId, String locationId, String queueId, String jobId,
			String serviceAccountEmail, String id, String input) throws IOException {

		// Instantiates a client.
		try (CloudTasksClient client = CloudTasksClient.create()) {
			
			String jsonString = "{'jobId':'" + jobId + "','id':'" + id + "','address':'" + input + "'}";
			JsonObject payload = (JsonObject) JsonParser.parseString(jsonString);
			// Construct the fully qualified queue name.
			String queuePath = QueueName.of(projectId, locationId, queueId).toString();
			OidcToken.Builder oidcTokenBuilder = OidcToken.newBuilder().setServiceAccountEmail(serviceAccountEmail)
					.setAudience(apiCallFunction);
			// Construct the task body.
			Task.Builder taskBuilder = Task.newBuilder()
					.setHttpRequest(HttpRequest.newBuilder()
							.setBody(ByteString.copyFrom(payload.toString(), Charset.defaultCharset())).setUrl(apiCallFunction)
							.putHeaders("Content-Type", "application/json").setHttpMethod(HttpMethod.POST)
							.setOidcToken(oidcTokenBuilder).build());

			// Send create task request.
			Task task = client.createTask(queuePath, taskBuilder.build());
			log.info(String.format("Task created: %s", task.getName()));
			
		}
	}
}
