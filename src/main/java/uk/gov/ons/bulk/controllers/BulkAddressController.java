package uk.gov.ons.bulk.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import com.google.api.client.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableId;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.BulkRequestContainer;
import uk.gov.ons.bulk.entities.BulkRequestParams;
import uk.gov.ons.bulk.entities.Job;
import uk.gov.ons.bulk.entities.Result;
import uk.gov.ons.bulk.entities.ResultContainer;
import uk.gov.ons.bulk.service.CloudTaskService;
import uk.gov.ons.bulk.util.QueryFuncs;
import uk.gov.ons.bulk.validator.Epoch;

@Slf4j
@Validated
@RestController
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
	
	@Autowired
	private CloudTaskService cloudTaskService;

	private String BASE_DATASET_QUERY;
	private String INFO_TABLE_QUERY;
	private String JOBS_QUERY;
	private String JOB_QUERY;
	private String RESULT_QUERY;
	private String MAX_QUERY;

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

		RESULT_QUERY = new StringBuilder()
				.append(BASE_DATASET_QUERY)
				.append(".")
				.append("results%s;").toString();

		MAX_QUERY = new StringBuilder()
				.append("SELECT MAX(runid) FROM ")
				.append(projectId)
				.append(".")
				.append(datasetName)
				.append(".")
				.append(infoTable).toString();
	}

	@GetMapping(value = "/jobs", produces = "application/json")
	public ResponseEntity<String> getBulkRequestProgress() {

		String output;
		
		try {
			ArrayList<Job> joblist = new ArrayList<Job>();
			
			QueryFuncs.runQuery(JOBS_QUERY,bigquery).forEach(row -> {
				Job job = new Job();
				job.setRunid(row.get("runid").getStringValue());
				job.setUserid(row.get("userid").getStringValue());
				job.setStatus(row.get("status").getStringValue());
				job.setTotalrecs(row.get("totalrecs").getStringValue());
				job.setRecssofar(row.get("recssofar").getStringValue());
				joblist.add(job);
			});			

			ObjectMapper objectMapper = new ObjectMapper();
			output = objectMapper.writeValueAsString(joblist);

		} catch (InterruptedException | JsonProcessingException ex) {

			String response = String.format("/jobs error: %s",ex.getMessage());

			log.error(response);
			return ResponseEntity.internalServerError().body(response);
		}

		return ResponseEntity.ok(output);
	}
	
	@PostMapping(value = "/bulk", produces = "application/json")
	public ResponseEntity<String> runBulkRequest(@Valid @RequestBody BulkRequestContainer bulkRequestContainer,
			@RequestParam(required = false, defaultValue = "5") @Min(1) @Max(100) String limitperaddress,
			@RequestParam(required = false) @Pattern(regexp = "^[^*,]+$", message = "{class.val.message}") String classificationfilter,
			@RequestParam(required = false, defaultValue = "true") @Pattern(regexp = "^(true|false)$", message = "{historical.val.message}") String historical,
			@RequestParam(required = false, defaultValue = "5") @Min(1) @Max(100) String matchthreshold,
			@RequestParam(required = false, defaultValue = "false") @Pattern(regexp = "^(true|false)$", message = "{verbose.val.message}") String verbose,
			@RequestParam(required = false, defaultValue = "${aims.current-epoch}") @Epoch(message = "{epoch.val.message}") String epoch,
			@RequestParam(required = false, defaultValue = "false") @Pattern(regexp = "^(true|false)$", message = "{excludeengland.val.message}") String excludeengland,
			@RequestParam(required = false, defaultValue = "false") @Pattern(regexp = "^(true|false)$", message = "{excludescotland.val.message}") String excludescotland,
			@RequestParam(required = false, defaultValue = "false") @Pattern(regexp = "^(true|false)$", message = "{excludewales.val.message}") String excludewales,
			@RequestParam(required = false, defaultValue = "false") @Pattern(regexp = "^(true|false)$", message = "{excludenorthernireland.val.message}") String excludenorthernireland,
			@RequestHeader Map<String, String> headersIn) {

		// set the bulk parameters object using the valid input parameters
		BulkRequestParams bulkRequestParams = new BulkRequestParams(limitperaddress, classificationfilter, historical,
				matchthreshold, verbose, epoch, excludeengland, excludescotland, excludewales, excludenorthernireland);

		/*
		 * We are using a single Dataset for the bulk service which makes gathering info
		 * on each bulk run easier. We need a more robust method of naming the tables
		 * for each bulk run. Should probably use UUID.randomUUID() or similar. Don't
		 * need to look up the latest ID used if using UUID.
		 */
		Long jobId = 0L;
		BulkRequestContainer bcont;

		try {
			bcont = bulkRequestContainer;
			int recs = bcont.getAddresses().length;
			long newKey = 0;

			for (FieldValueList row : QueryFuncs.runQuery(MAX_QUERY, bigquery)) {
				for (FieldValue val : row) {
					newKey = val.getLongValue() + 1;
					log.info(String.format("newkey:%d", newKey));
				}
			}

			// Pass on username and api key headers from CA Gateway
			HttpHeaders headers = new HttpHeaders();
			String userName = headersIn.getOrDefault("user","Anon");
			headers.set("user",userName);
			headers.setAuthorization(headersIn.getOrDefault("Authorization","None"));

			// Create new Job record
			String tableName = "bulkinfo";
			Map<String, Object> row1Data = new HashMap<>();
			row1Data.put("runid", newKey);
			row1Data.put("userid", userName);
			row1Data.put("status", "waiting");
			row1Data.put("totalrecs", recs);
			row1Data.put("recssofar", 0);
			TableId tableId = TableId.of(datasetName, tableName);

			String response = QueryFuncs.InsertRow(bigquery, tableId, row1Data);
			log.debug(response);

			tableName = "results" + newKey;
			jobId = newKey;
			Schema schema = Schema.of(
					Field.of("id", StandardSQLTypeName.INT64),
					Field.of("inputaddress", StandardSQLTypeName.STRING),
					Field.of("response", StandardSQLTypeName.STRING));
			QueryFuncs.createTable(bigquery, datasetName, tableName, schema);

			cloudTaskService.createTasks(jobId, bcont.getAddresses(), bulkRequestParams, headers);

		} catch (InterruptedException | IOException ex) {
			
			String response = String.format("/bulk error: %s", ex.getMessage());
			log.error(response);
			return ResponseEntity.internalServerError().body(response);
		}

		return ResponseEntity.ok(new ObjectMapper().createObjectNode().put("jobId", String.valueOf(jobId)).toString());
	}

	@GetMapping(value = "/bulk-progress/{jobid}", produces = "application/json")
	public ResponseEntity<String> getBulkRequestProgress(
			@PathVariable(required = true, name = "jobid") @NotBlank(message="{jobid.val.message}") String jobid) {

		String output;
		
		try {
			Job job = new Job();
			
			QueryFuncs.runQuery(String.format(JOB_QUERY, jobid), bigquery).forEach(row -> {
				job.setRunid(row.get("runid").getStringValue());
				job.setUserid(row.get("userid").getStringValue());
				job.setStatus(row.get("status").getStringValue());
				job.setTotalrecs(row.get("totalrecs").getStringValue());
				job.setRecssofar(row.get("recssofar").getStringValue());
			});

			ObjectMapper objectMapper = new ObjectMapper();
			output = objectMapper.writeValueAsString(job);

		} catch (InterruptedException | JsonProcessingException ex) {

			String response = String.format("/bulk-progress/%s error: %s", jobid, ex.getMessage());

			log.error(response);
			return ResponseEntity.internalServerError().body(response);
		}

		return ResponseEntity.ok(output);
	}
	
	@GetMapping(value = "/bulk-result/{jobid}", produces = "application/json")
	public @ResponseBody ResponseEntity<String> getBulkResults(
			@PathVariable(required = true, name = "jobid") @NotBlank(message="{jobid.val.message}") String jobid) {
		
		/*
		 * This is not going to be very speedy with large result sets.
		 * Need another export mechanism.
		 * https://cloud.google.com/bigquery/docs/exporting-data#java
		 * Can be exported to JSON.
		 */
		ArrayList<Result> rlist = new ArrayList<Result>();
		ResultContainer rcont = new ResultContainer();
		String output;
		ObjectMapper objectMapper = new ObjectMapper();

		try {

			for (FieldValueList row : QueryFuncs.runQuery(String.format(RESULT_QUERY, jobid), bigquery)) {
				Result nextResult = new Result();
				nextResult.setId(row.get("id").getStringValue());
				nextResult.setInputaddress(row.get("inputaddress").getStringValue());
				String jsonString = row.get("response").getStringValue();
				Map<String, Object> jsonMap = new HashMap<String, Object>();
				jsonMap = objectMapper.readValue(jsonString, Map.class);
				nextResult.setResponse(jsonMap);
				rlist.add(nextResult);
			}

			rcont.setResults(rlist);
			rcont.setJobid(jobid);
			rcont.setStatus(HttpStatus.OK.toString());

			output = objectMapper.writeValueAsString(rcont);

		} catch (InterruptedException | JsonProcessingException ex) {
			String response = String.format("/bulk-result/%s error: %s", jobid, ex.getMessage());

			log.error(response);
			return ResponseEntity.internalServerError().body(response);
		}

		return ResponseEntity.ok(output);
	}
}
