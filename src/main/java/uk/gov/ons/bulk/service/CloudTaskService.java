package uk.gov.ons.bulk.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.BulkRequest;
import uk.gov.ons.bulk.entities.BulkRequestParams;

@Service
@Slf4j
public class CloudTaskService {

	@Value("${aims.cloud-functions.create-cloud-task-function}")
	private String createTaskFunction;
	
	@Value("${aims.report-frequency}")
	private double reportFrequency; 

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
	public void createTasks(long jobId, BulkRequest[] addresses, long totalAddresses, BulkRequestParams bulkRequestParams, HttpHeaders headers) throws IOException {
		
		GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
		
		if (!(credentials instanceof IdTokenProvider)) {
			throw new IllegalArgumentException("Credentials are not an instance of IdTokenProvider.");
		}
		
		IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder()
				.setIdTokenProvider((IdTokenProvider) credentials).setTargetAudience(createTaskFunction).build();

		GenericUrl genericUrl = new GenericUrl(createTaskFunction);
		HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(tokenCredential);
		HttpTransport transport = new NetHttpTransport();		
		
		List<Integer> reportAddresses = reportAddresses(addresses.length);
		reportAddresses.add(addresses.length);
		
		for (int i = 0; i < addresses.length; i++) {	
					
			if (reportAddresses.contains(i + 1)) {
				log.debug("Reporting: " + (i + 1));
			}
			
			BulkJobRequest bjr = new BulkJobRequest(String.valueOf(jobId), addresses[i].getId(), addresses[i].getAddress(), 
					String.valueOf(i + 1), String.valueOf(totalAddresses), 
					String.valueOf(reportAddresses.contains(i + 1)), bulkRequestParams);
			
			HttpContent content = new JsonHttpContent(new GsonFactory(), bjr.getJob());
			HttpRequest request = transport.createRequestFactory(adapter).buildPostRequest(genericUrl, content);
			request.setHeaders(headers);
			request.execute();
		}
	}
	
	private List<Integer> reportAddresses(double length) {
		
		double x = reportFrequency / 100;
		List<Integer> reportAddresses = new ArrayList<Integer>();
		
		do {
			reportAddresses.add((int)Math.round(length * x));
			x = x + (reportFrequency / 100);
		} while(x < 1);
		
		return reportAddresses;
	}
	
	public @Data class BulkJobRequest {
		private Map<String, String> job;

		public BulkJobRequest(String jobId, String id, String address, String addressNumber, String totalAddresses, String report, BulkRequestParams bulkRequestParams) {
			super();
			job = new HashMap<String, String>();
			job.put("jobId", jobId);
			job.put("id", id);
			job.put("address", address);
			job.put("item", addressNumber);
			job.put("total", totalAddresses);
			job.put("report", report);
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
