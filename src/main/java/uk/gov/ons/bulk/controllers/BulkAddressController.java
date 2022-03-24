package uk.gov.ons.bulk.controllers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableId;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.*;
import uk.gov.ons.bulk.service.CloudTaskService;
import uk.gov.ons.bulk.util.QueryFuncs;
import uk.gov.ons.bulk.util.ValidateFuncs;

@Slf4j
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

	@GetMapping(value = "/")
	@ResponseStatus(HttpStatus.OK)
	public String index() {
		return "OK";
	}

	@GetMapping(value = "/jobs")
	public String getBulkRequestProgress(Model model) {

		try {
			ArrayList<Job> joblist = new ArrayList<Job>();
			for (FieldValueList row : QueryFuncs.runQuery(JOBS_QUERY,bigquery)) {
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
	public String runBulkRequest(@RequestBody String addressesJson,
								 @RequestParam(required = false, defaultValue = "5") int limitperaddress,
								 @RequestParam(required = false) String classificationfilter,
								 @RequestParam(required = false, defaultValue = "true") boolean historical,
								 @RequestParam(required = false, defaultValue = "10")  int matchthreshold,
								 @RequestParam(required = false, defaultValue = "false") boolean verbose,
								 @RequestParam(required = false, defaultValue = "current") String epoch,
								 @RequestParam(required = false, defaultValue = "false") boolean excludeengland,
								 @RequestParam(required = false, defaultValue = "false") boolean excludenorthernireland,
								 @RequestParam(required = false, defaultValue = "false") boolean excludescotland,
								 @RequestParam(required = false, defaultValue = "false") boolean excludewales
								 ) {

		BulkRequestParams bulkRequestParams = new BulkRequestParams();
		bulkRequestParams.setLimit(Integer.toString(limitperaddress));
		bulkRequestParams.setMatchthreshold(Integer.toString(matchthreshold));
		if (classificationfilter != null) bulkRequestParams.setClassificationfilter(classificationfilter);
		bulkRequestParams.setEpoch(epoch);
		bulkRequestParams.setHistorical(Boolean.toString(historical));
		bulkRequestParams.setVerbose(Boolean.toString(verbose));
		if (excludeengland) bulkRequestParams.setEboost("0");
		if (excludenorthernireland) bulkRequestParams.setNboost("0");
		if (excludescotland) bulkRequestParams.setSboost("0");
		if (excludewales) bulkRequestParams.setWboost("0");

		BulkRequestParamsErrors brps = ValidateFuncs.validateBulkParams(bulkRequestParams);
		String validationResult = brps.toString();

		/*
		 * We are using a single Dataset for the bulk service which makes gathering info
		 * on each bulk run easier. We need a more robust method of naming the tables
		 * for each bulk run. Should probably use UUID.randomUUID() or similar. Don't
		 * need to look up the latest ID used if using UUID.
		 */

		// Create dataset UUID
		Long jobId = 0L;
		BulkRequestContainer bcont;
		ObjectMapper objectMapper = new ObjectMapper();

		try {
			bcont = objectMapper.readValue(addressesJson, BulkRequestContainer.class);
			int recs = bcont.getAddresses().length;
			long newKey = 0;
			
			for (FieldValueList row : QueryFuncs.runQuery(MAX_QUERY, bigquery)) {
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
			
			String response = QueryFuncs.InsertRow(bigquery, tableId, row1Data);
			log.debug(response);

			tableName = "results" + newKey;
			jobId = newKey;
			Schema schema = Schema.of(
					Field.of("id", StandardSQLTypeName.INT64),
					Field.of("inputaddress", StandardSQLTypeName.STRING),
					Field.of("response", StandardSQLTypeName.STRING));
			QueryFuncs.createTable(bigquery, datasetName, tableName, schema);
	
			cloudTaskService.createTasks(jobId, bcont.getAddresses(), bulkRequestParams);

		} catch (InterruptedException | IOException e) {
			log.error(String.format("Error in /bulk endpoint: ", e.getMessage()));
		}

		return new ObjectMapper().createObjectNode().put("jobId", String.valueOf(jobId)).toString();
	}

	@GetMapping(value = "/bulk-progress/{jobid}")
	public String getBulkRequestProgress(@PathVariable(required = true, name = "jobid") String jobid, Model model) {

		try {

			ArrayList<Job> joblist = new ArrayList<Job>();
			for (FieldValueList row : QueryFuncs.runQuery(String.format(JOB_QUERY, jobid),bigquery)) {
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
			for (FieldValueList row : QueryFuncs.runQuery(String.format(RESULT_QUERY, jobid),bigquery)) {
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
}
