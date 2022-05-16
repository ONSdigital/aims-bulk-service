package uk.gov.ons.bulk.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.google.api.client.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;

import lombok.Data;
import uk.gov.ons.bulk.entities.BulkRequest;
import uk.gov.ons.bulk.entities.BulkRequestParams;

@Service
public class CloudTaskService {

	@Value("${aims.cloud-functions.create-cloud-task-function}")
	private String createTaskFunction;
	
	/**
	 * Send each address to GCP Cloud Function for matching.
	 * Done asynchronously so as not to block the client response.
	 * This won't work locally as it requires a service account.
	 * Service account has Cloud Functions Invoker role but must also authenticate. 
	 * 
	 * 
	 * @param jobId
	 * @param addresses
	 * @throws IOException
	 */	
	@Async
	public void createTasks(Long jobId, BulkRequest[] addresses, BulkRequestParams bulkRequestParams, HttpHeaders headers) throws IOException {
		
		GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
		
		if (!(credentials instanceof IdTokenProvider)) {
			throw new IllegalArgumentException("Credentials are not an instance of IdTokenProvider.");
		}
		
		IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder()
				.setIdTokenProvider((IdTokenProvider) credentials).setTargetAudience(createTaskFunction).build();

		GenericUrl genericUrl = new GenericUrl(createTaskFunction);
		HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(tokenCredential);
		HttpTransport transport = new NetHttpTransport();		
		
		for (int i = 0; i < addresses.length; i++) {	
			BulkJobRequest bjr = new BulkJobRequest(String.valueOf(jobId), addresses[i].getId(), addresses[i].getAddress(), bulkRequestParams);
			
			HttpContent content = new JsonHttpContent(new JacksonFactory(), bjr.getJob());
			HttpRequest request = transport.createRequestFactory(adapter).buildPostRequest(genericUrl, content);
		    request.setHeaders(headers);
			request.execute();
		}
	}
	
	public @Data class BulkJobRequest {
		private Map<String, String> job;

		public BulkJobRequest(String jobId, String id, String address, BulkRequestParams bulkRequestParams) {
			super();
			job = new HashMap<String, String>();
			job.put("jobId", jobId);
			job.put("id", id);
			job.put("address", address);
			job.put("limitperaddress", bulkRequestParams.getLimitperaddress());
			job.put("classificationfilter", bulkRequestParams.getClassificationfilter());
			job.put("historical", bulkRequestParams.getHistorical());
			job.put("matchthreshold", bulkRequestParams.getMatchthreshold());
			job.put("verbose", bulkRequestParams.getVerbose());
			job.put("epoch", bulkRequestParams.getEpoch());
			job.put("eboost", bulkRequestParams.getEboost());
			job.put("sboost", bulkRequestParams.getSboost());
			job.put("wboost", bulkRequestParams.getWboost());
			job.put("nboost", bulkRequestParams.getNboost());
		}
	}
}
