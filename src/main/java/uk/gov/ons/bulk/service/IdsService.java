package uk.gov.ons.bulk.service;

import static uk.gov.ons.bulk.util.BulkServiceConstants.BIG_QUERY_IDS_TABLE_PREFIX;
import static uk.gov.ons.bulk.util.BulkServiceConstants.Status.IP;
import static uk.gov.ons.bulk.util.BulkServiceConstants.Status.RD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.JobException;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableId;
import com.google.cloud.bigquery.TableResult;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.BulkRequestParams;
import uk.gov.ons.bulk.entities.IdsBulkInfo;
import uk.gov.ons.bulk.entities.IdsRequest;
import uk.gov.ons.bulk.entities.NewIdsJobMessage;
import uk.gov.ons.bulk.entities.Payload;
import uk.gov.ons.bulk.exception.BulkAddressException;
import uk.gov.ons.bulk.util.QueryFuncs;

@Service
@Slf4j
public class IdsService {
	
	@Autowired
	private BigQuery bigQuery;
	
	@Autowired
	private BulkStatusService bulkStatusService;
	
	@Autowired
	private CloudTaskService cloudTaskService;
	
	@Value("${ids.cloud.gcp.bigquery.dataset-name}")
	private String idsDatasetName;
	
	@Value("${spring.cloud.gcp.project-id}")
	private String projectId;
	
	@Value("${aims.epochs}")
	private String epochs;
	
	private String QUERY_IDS_DATASET_TABLE = "SELECT * FROM %s.%s.%s";
	
	public void createTasks(NewIdsJobMessage newIdsJobMessage) {

		TableResult results;
		
		try {
			
			List<IdsRequest> idsRequests = new ArrayList<IdsRequest>();
			
			QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(
					String.format(QUERY_IDS_DATASET_TABLE, projectId, newIdsJobMessage.getPayload().getBigQueryDataset(), newIdsJobMessage.getPayload().getBigQueryTable())).build();
			
			// How many rows can this method handle?		
			results = bigQuery.query(queryConfig);
			
			// Create a status row for this IDS job
			IdsBulkInfo idsBulkInfo = new IdsBulkInfo(newIdsJobMessage.getPayload().getIdsJobId(), 
													  newIdsJobMessage.getPayload().getIdsUserId(),
													  IP.getStatus(), 
													  results.getTotalRows(), 
													  0,
													  newIdsJobMessage.isTest());
			long newKey = bulkStatusService.saveIdsJob(idsBulkInfo);
			
			// Create a results table in AIMS BigQuery for this IDS job
			String tableName = BIG_QUERY_IDS_TABLE_PREFIX + newKey;

			Schema schema = Schema.of(
					Field.of("id", StandardSQLTypeName.STRING),
					Field.of("inputaddress", StandardSQLTypeName.STRING),
					Field.of("response", StandardSQLTypeName.STRING));
			QueryFuncs.createTable(bigQuery, idsDatasetName, tableName, schema);

			results.iterateAll().forEach(row -> {
				// Create the tasks
				idsRequests.add(new IdsRequest(row.get("id").getStringValue(), row.get("inputaddress").getStringValue()));
			});
			
			// These parameters need to be validated. Probably in the POJO.
			// Some are hardcoded here - how many do we want IDS to be able to set?
			BulkRequestParams bulkRequestParams = new BulkRequestParams(
					newIdsJobMessage.getPayload().getAddressLimit(),
					null,
					newIdsJobMessage.getPayload().getHistorical(),
					newIdsJobMessage.getPayload().getQualityMatchThreshold(),
					"false",
					newIdsJobMessage.getPayload().getEpoch(),
					"", "", "", "","", "", "",
					"false");
			
			String userName = newIdsJobMessage.getPayload().getIdsUserId();
			String topic = "";
			String dataset = "";
			
			cloudTaskService.createIdsTasks(newKey, newIdsJobMessage.getPayload().getIdsJobId(), idsRequests, results.getTotalRows(), bulkRequestParams, userName, topic, dataset);
		} catch (JobException | InterruptedException e) {
			log.error(String.format("Problem querying BigQuery: %s", e.getMessage()));
		} catch (IOException e) {
			log.error(String.format("Problem creating tasks: %s", e.getMessage()));
		}
	}
	
	public void deleteIdsResultTable(Payload payload) throws BulkAddressException {

		List<IdsBulkInfo> idsJob = bulkStatusService.getIdsJob(payload.getIdsJobId());

		if (idsJob == null || idsJob.size() != 1) {
			throw new BulkAddressException(
					String.format("Unable to get details of idsjobid: %s. The job does not exist.", payload.getIdsJobId()));
		} else {

			String tableName = BIG_QUERY_IDS_TABLE_PREFIX + idsJob.get(0).getJobid();

			try {
				boolean success = bigQuery.delete(TableId.of(idsDatasetName, tableName));
				if (success) {
					log.debug("Table deleted successfully");
					// Update the status table to results-deleted
					bulkStatusService.updateStatus(idsJob.get(0).getJobid(), RD);
				} else {
					log.debug("Table was not found");
				}
			} catch (BigQueryException e) {
				log.error(String.format("Table was not deleted: %s", e.getMessage()));
			}
		}
	}
}
