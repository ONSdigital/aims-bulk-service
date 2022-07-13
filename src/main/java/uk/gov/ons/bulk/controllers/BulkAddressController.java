package uk.gov.ons.bulk.controllers;

import static uk.gov.ons.bulk.util.BulkServiceConstants.BIG_QUERY_TABLE_PREFIX;
import static uk.gov.ons.bulk.util.BulkServiceConstants.Status.IP;
import static uk.gov.ons.bulk.util.BulkServiceConstants.Status.RR;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.api.client.http.HttpHeaders;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.BulkInfo;
import uk.gov.ons.bulk.entities.BulkInfoList;
import uk.gov.ons.bulk.entities.BulkRequestContainer;
import uk.gov.ons.bulk.entities.BulkRequestParams;
import uk.gov.ons.bulk.exception.BulkAddressException;
import uk.gov.ons.bulk.service.BulkStatusService;
import uk.gov.ons.bulk.service.CloudTaskService;
import uk.gov.ons.bulk.service.DownloadService;
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

	@Autowired
	private DownloadService downloadService;

	@Value("${aims.project-number}")
	private String projectNumber;

	@GetMapping(value = "/jobs", produces = "application/json")
	public ResponseEntity<String> getBulkRequestProgress(
			@RequestParam(required = false, defaultValue = "") String userid,
			@RequestParam(required = false, defaultValue = "") @Pattern(regexp = "^(|in-progress|processing-finished|results-ready)$", message = "{status.val.message}") String status) {

		String output;
		String chosenStatus = status;

		List<BulkInfo> jobsList = bulkStatusService.getJobs(userid, chosenStatus);
		BulkInfoList jobs = new BulkInfoList(jobsList);
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
				.setSerializationInclusion(Include.NON_NULL);

		try {
			output = objectMapper.writeValueAsString(jobs);
		} catch (JsonProcessingException ex) {
			String response = String.format("/jobs error: %s", ex.getMessage());
			log.error(response);
			return ResponseEntity.internalServerError()
					.body(new ObjectMapper().createObjectNode().put("error", response).toString());
		}

		return ResponseEntity.ok(output);
	}

	@PostMapping(value = "/bulk", produces = "application/json")
	public ResponseEntity<String> runBulkRequest(@Valid @RequestBody BulkRequestContainer bulkRequestContainer,
			@RequestParam(required = false, defaultValue = "5") @Min(1) @Max(100) String limitperaddress,
			@RequestParam(required = false) @Pattern(regexp = "^[^*,]+$", message = "{class.val.message}") String classificationfilter,
			@RequestParam(required = false, defaultValue = "true") @Pattern(regexp = "^(true|false)$", message = "{historical.val.message}") String historical,
			@RequestParam(required = false, defaultValue = "10") @Min(1) @Max(100) String matchthreshold,
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
		String userName = headersIn.getOrDefault("user", "Anon");
		headers.set("user", userName);
		headers.setAuthorization(headersIn.getOrDefault("Authorization", "None"));

		BulkRequestContainer bcont = bulkRequestContainer;
		long recs = bcont.getAddresses().length;

		BulkInfo bulkInfo = new BulkInfo(userName, IP.getStatus(), recs, 0);

		long newKey = bulkStatusService.saveJob(bulkInfo);

		try {
			String tableName = BIG_QUERY_TABLE_PREFIX + newKey;

			Schema schema = Schema.of(
					Field.of("id", StandardSQLTypeName.INT64),
					Field.of("inputaddress", StandardSQLTypeName.STRING),
					Field.of("response", StandardSQLTypeName.STRING));
			QueryFuncs.createTable(bigquery, datasetName, tableName, schema);

			cloudTaskService.createTasks(newKey, bcont.getAddresses(), recs, bulkRequestParams, headers);
		} catch (IOException ex) {
			String response = String.format("/bulk error: %s", ex.getMessage());
			log.error(response);
			return ResponseEntity.internalServerError()
					.body(new ObjectMapper().createObjectNode().put("error", response).toString());
		}

		return ResponseEntity.ok(new ObjectMapper().createObjectNode().put("jobId", String.valueOf(newKey)).toString());
	}

	@GetMapping(value = "/bulk-progress/{jobid}", produces = "application/json")
	public ResponseEntity<String> getBulkRequestProgress(
			@PathVariable(required = true, name = "jobid") @Pattern(regexp = "^[0-9]+$", message = "{jobid.val.message}") String jobid) {

		String output;

		List<BulkInfo> bulkInfos = bulkStatusService.queryJob(Long.parseLong(jobid));
		if (bulkInfos.size() == 0) {
			String response = String.format("Job ID %s not found on the system", jobid);
			log.info(response);
			return ResponseEntity.badRequest()
					.body(new ObjectMapper().createObjectNode().put("error", response).toString());
		}
		BulkInfo bulkInfo = bulkInfos.get(0);

		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
				.setSerializationInclusion(Include.NON_NULL);

		try {
			output = objectMapper.writeValueAsString(bulkInfo);
		} catch (JsonProcessingException e) {
			String response = String.format("/bulk-progress/%s error: %s", jobid, e.getMessage());
			log.error(response);
			return ResponseEntity.internalServerError()
					.body(new ObjectMapper().createObjectNode().put("error", response).toString());
		}

		return ResponseEntity.ok(output);
	}

	@GetMapping(value = "/bulk-result/{jobid}", produces = "application/json")
	public ResponseEntity<String> getBulkResults(
			@PathVariable(required = true, name = "jobid") @Pattern(regexp = "^[0-9]+$", message = "{jobid.val.message}") String jobId) {

		String filename = String.format("%s%s.csv.gz", BIG_QUERY_TABLE_PREFIX, jobId);

		// Does the jobId exist?
		List<BulkInfo> bulkInfos = bulkStatusService.queryJob(Long.parseLong(jobId));
		if (bulkInfos.size() == 0) {
			String response = String.format("Job ID %s not found on the system", jobId);
			log.info(response);
			return ResponseEntity.badRequest()
					.body(new ObjectMapper().createObjectNode().put("error", response).toString());
		}

		// Is the jobId downloadable? Check the status.
		if (bulkInfos.get(0).getStatus().equals(RR.getStatus())) {
			String signedUrl;
			try {
				signedUrl = downloadService.getSignedUrl(jobId, filename);
				return ResponseEntity.ok(new ObjectMapper().createObjectNode().put("file", filename)
						.put("signedUrl", signedUrl).toString());
			} catch (IOException | BulkAddressException ex) {
				String response = String.format("/bulk-result/%s error: %s", jobId, ex.getMessage());
				log.error(response);
				return ResponseEntity.internalServerError()
						.body(new ObjectMapper().createObjectNode().put("error", response).toString());
			}
		} else {
			String response = String.format("Job ID %s is not currently downloadable", jobId);
			log.info(response);
			return ResponseEntity.badRequest()
					.body(new ObjectMapper().createObjectNode().put("error", response).toString());
		}
	}
}
