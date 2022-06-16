package uk.gov.ons.bulk.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.api.client.http.HttpHeaders;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.BulkInfo;
import uk.gov.ons.bulk.entities.BulkRequestContainer;
import uk.gov.ons.bulk.entities.BulkRequestParams;
import uk.gov.ons.bulk.entities.Job;
import uk.gov.ons.bulk.entities.Result;
import uk.gov.ons.bulk.entities.ResultContainer;
import uk.gov.ons.bulk.service.BulkStatusService;
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
	
	@Autowired
	private CloudTaskService cloudTaskService;
	
	@Autowired
	private BulkStatusService bulkStatusService;

	private String BASE_DATASET_QUERY;
	private String JOBS_QUERY;
	private String RESULT_QUERY;
	
	@PostConstruct
	public void postConstruct() {

		BASE_DATASET_QUERY = new StringBuilder()
				.append("SELECT * FROM ")
				.append(projectId)
				.append(".")
				.append(datasetName).toString();

		RESULT_QUERY = new StringBuilder()
				.append(BASE_DATASET_QUERY)
				.append(".")
				.append("results%s;").toString();
	}

	@GetMapping(value = "/jobs", produces = "application/json")
	public ResponseEntity<String> getBulkRequestProgress(
			@RequestParam(required = false, defaultValue = "") String userid,
			@RequestParam(required = false, defaultValue = "") @Pattern(regexp = "^(|in-progress|finished)$", message = "{status.val.message}") String status
	) {

		String output;
		String chosenStatus = status;

		List<BulkInfo> jobsList = bulkStatusService.getJobs(userid,chosenStatus);
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).setSerializationInclusion(Include.NON_NULL);

		try {
			output = objectMapper.writeValueAsString(jobsList);

		} catch (JsonProcessingException ex) {

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

		// Pass on username and api key headers from CA Gateway
		HttpHeaders headers = new HttpHeaders();
		String userName = headersIn.getOrDefault("user","Anon");
		headers.set("user", userName);
		headers.setAuthorization(headersIn.getOrDefault("Authorization","None"));
		
		BulkRequestContainer bcont = bulkRequestContainer;
		long recs = bcont.getAddresses().length;
		
		BulkInfo bulkInfo = new BulkInfo(userName, "in-progress", recs, 0);
		
		long newKey = bulkStatusService.saveJob(bulkInfo);

		try {
			String tableName = "results_" + newKey;

			Schema schema = Schema.of(
					Field.of("id", StandardSQLTypeName.INT64),
					Field.of("inputaddress", StandardSQLTypeName.STRING),
					Field.of("response", StandardSQLTypeName.STRING));
			QueryFuncs.createTable(bigquery, datasetName, tableName, schema);

			cloudTaskService.createTasks(newKey, bcont.getAddresses(), recs, bulkRequestParams, headers);
		} catch (IOException ex) {
			
			String response = String.format("/bulk error: %s", ex.getMessage());
			log.error(response);
			return ResponseEntity.internalServerError().body(response);
		}

		return ResponseEntity.ok(new ObjectMapper().createObjectNode().put("jobId", String.valueOf(newKey)).toString());
	}

	@GetMapping(value = "/bulk-progress/{jobid}", produces = "application/json")
	public ResponseEntity<String> getBulkRequestProgress(
			@PathVariable(required = true, name = "jobid") @NotBlank(message="{jobid.val.message}") String jobid) {

		String output;

		List<BulkInfo> bulkInfos = bulkStatusService.queryJob(Long.parseLong(jobid));
		if (bulkInfos.size() == 0) {
			return ResponseEntity.badRequest().body("Job ID " + jobid + " not found on the system");
		}
		BulkInfo bulkInfo = bulkInfos.get(0);

		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
	            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false).setSerializationInclusion(Include.NON_NULL);
		
		try {
			output = objectMapper.writeValueAsString(bulkInfo);
		} catch (JsonProcessingException e) {
			String response = String.format("/bulk-progress/%s error: %s", jobid, e.getMessage());
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
