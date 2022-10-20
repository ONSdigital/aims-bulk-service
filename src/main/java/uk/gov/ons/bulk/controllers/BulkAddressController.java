package uk.gov.ons.bulk.controllers;

import static uk.gov.ons.bulk.util.BulkServiceConstants.BIG_QUERY_TABLE_PREFIX;
import static uk.gov.ons.bulk.util.BulkServiceConstants.Status.IP;
import static uk.gov.ons.bulk.util.BulkServiceConstants.Status.RE;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

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
import com.google.cloud.bigquery.StandardSQLTypeName;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.BulkInfo;
import uk.gov.ons.bulk.entities.BulkRequestContainer;
import uk.gov.ons.bulk.entities.BulkRequestParams;
import uk.gov.ons.bulk.entities.IdsBulkInfo;
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

	@Operation(summary = "Get a list of bulk jobs on the system, can be filtered by user or status")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Jobs list returned OK (can be empty)",
					content = { @Content(mediaType = "application/json",
							schema = @Schema(implementation = BulkInfo.class)) }),
			@ApiResponse(responseCode = "400", description = "Invalid parameter supplied",
					content = @Content),
			@ApiResponse(responseCode = "401", description = "JWT missing or invalid",
					content = @Content) })
	@GetMapping(value = "/jobs", produces = "application/json")
	public ResponseEntity<String> getBulkRequestProgress(
			@Parameter(description="userid to filter jobs")
			@RequestParam(required = false, defaultValue = "") String userid,
			@Parameter(description="status to filter jobs")
			@RequestParam(required = false, defaultValue = "") @Pattern(regexp = "^(|in-progress|processing-finished|results-ready|results-exported)$", message = "{status.val.message}") String status) {

		List<BulkInfo> jobsList = bulkStatusService.getJobs(userid, status);
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
				.setSerializationInclusion(Include.NON_NULL);

		return ResponseEntity.ok(objectMapper.createObjectNode().set("jobs", objectMapper.valueToTree(jobsList)).toString());
	}

	@Operation(summary = "Submit a bulk matching job to the system")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Job id returned to indicate successful submission",
					content = { @Content(mediaType = "application/json",
							schema = @Schema(type="string", example="jobId = 42")) }),
			@ApiResponse(responseCode = "400", description = "Invalid parameter supplied, such as non-existent job id",
					content = @Content),
			@ApiResponse(responseCode = "401", description = "JWT missing or invalid",
					content = @Content) })
	@PostMapping(value = "/bulk", produces = "application/json")
	public ResponseEntity<String> runBulkRequest(@Valid @RequestBody BulkRequestContainer bulkRequestContainer,
			@Parameter(description = "Maximum number of results per input address")
			@RequestParam(required = false, defaultValue = "5") @Min(1) @Max(100) String limitperaddress,
			@Parameter(description = "Classification code single value, list or pattern to filter results")
			@RequestParam(required = false) @Pattern(regexp = "^[^*,]+$", message = "{class.val.message}") String classificationfilter,
			@Parameter(description = "Include historical records true or false")
			@RequestParam(required = false, defaultValue = "true") @Pattern(regexp = "^(true|false)$", message = "{historical.val.message}") String historical,
			@Parameter(description = "Minimum confidence score for results to be accepted")
			@RequestParam(required = false, defaultValue = "10") @Min(0) @Max(100) String matchthreshold,
			@Parameter(description = "Output the full address details for each result true or false")
			@RequestParam(required = false, defaultValue = "false") @Pattern(regexp = "^(true|false)$", message = "{verbose.val.message}") String verbose,
			@Parameter(description = "Epoch number e.g. 95, default is latest available")
			@RequestParam(required = false, defaultValue = "${aims.current-epoch}") @Epoch(message = "{epoch.val.message}") String epoch,
			@Parameter(description = "Flag to exclude results from England")
			@RequestParam(required = false, defaultValue = "false") @Pattern(regexp = "^(true|false)$", message = "{excludeengland.val.message}") String excludeengland,
			@Parameter(description = "Flag to exclude results from Scotland")
			@RequestParam(required = false, defaultValue = "false") @Pattern(regexp = "^(true|false)$", message = "{excludescotland.val.message}") String excludescotland,
			@Parameter(description = "Flag to exclude results from Wales")
			@RequestParam(required = false, defaultValue = "false") @Pattern(regexp = "^(true|false)$", message = "{excludewales.val.message}") String excludewales,
			@Parameter(description = "Flag to exclude results from Northern Ireland")
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

			com.google.cloud.bigquery.Schema schema = com.google.cloud.bigquery.Schema.of(
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

	@Operation(summary = "Return the status table values for a named job")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Job status fields for one job returned",
					content = { @Content(mediaType = "application/json",
							schema = @Schema(implementation = BulkInfo.class)) }),
			@ApiResponse(responseCode = "400", description = "Invalid parameter supplied, such as non-existant job id",
					content = @Content),
			@ApiResponse(responseCode = "401", description = "JWT missing or invalid",
					content = @Content) })
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

	@Operation(summary = "Return the results for a job as a signed URL")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "signed url or message indicating download not available",
					content = { @Content(mediaType = "application/json",
							schema = @Schema(implementation = DownloadService.SignedUrlResponse.class)) }),
			@ApiResponse(responseCode = "400", description = "Job not ready for download or id not on system",
					content = @Content),
			@ApiResponse(responseCode = "401", description = "JWT missing or invalid",
					content = @Content) })
	@GetMapping(value = "/bulk-result/{jobid}", produces = "application/json")
	public ResponseEntity<String> getBulkResults(
			@Parameter(description = "Numeric id of job")
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
		if (bulkInfos.get(0).getStatus().equals(RE.getStatus())) {
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
	@Operation(description="IDS version of progress endpoint", hidden = true)
	@GetMapping(value = "/ids/bulk-progress/{idsjobid}", produces = "application/json")
	public ResponseEntity<String> getIdsBulkRequestProgress(
			@PathVariable(required = true, name = "idsjobid") @NotBlank(message = "{idsjobid.val.message}") String idsjobid) {

		String output;

		List<IdsBulkInfo> idsBulkInfos = bulkStatusService.getIdsJob(idsjobid);
		if (idsBulkInfos.size() == 0) {
			String response = String.format("IDS Job ID %s not found on the system", idsjobid);
			log.info(response);
			return ResponseEntity.badRequest()
					.body(new ObjectMapper().createObjectNode().put("error", response).toString());
		}
		IdsBulkInfo idsBulkInfo = idsBulkInfos.get(0);

		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
				.setSerializationInclusion(Include.NON_NULL);

		try {
			output = objectMapper.writeValueAsString(idsBulkInfo);
		} catch (JsonProcessingException e) {
			String response = String.format("/ids/bulk-progress/%s error: %s", idsjobid, e.getMessage());
			log.error(response);
			return ResponseEntity.internalServerError()
					.body(new ObjectMapper().createObjectNode().put("error", response).toString());
		}

		return ResponseEntity.ok(output);
	}
	

	@Operation(description="IDS version of jobs list", hidden = true)
	@GetMapping(value = "/ids/jobs", produces = "application/json")
	public ResponseEntity<String> getIdsBulkRequestProgress(
			@RequestParam(required = false, defaultValue = "") String userid,
			@RequestParam(required = false, defaultValue = "") @Pattern(regexp = "^(|in-progress|processing-finished|results-ready|results-deleted)$", message = "{status.ids.val.message}") String status) {

		List<IdsBulkInfo> jobsList = bulkStatusService.getIdsJobs(userid, status);
		ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule())
				.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
				.setSerializationInclusion(Include.NON_NULL);

		return ResponseEntity.ok(objectMapper.createObjectNode().set("jobs", objectMapper.valueToTree(jobsList)).toString());
	}
}
