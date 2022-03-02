package uk.gov.ons.bulk.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;

import lombok.Data;
import uk.gov.ons.bulk.entities.BulkRequest;

@Service
public class CloudTaskService {

	@Value("${aims.cloud-functions.create-cloud-task-function}")
	private String createTaskFunction;
	
	/**
	 * Send individual address to GCP Cloud Function for matching.
	 * This won't work locally as it requires a service account.
	 * Service account has Cloud Functions Invoker role but must also authenticate. 
	 * 
	 * @param jobId the id for this job
	 * @param id    input address id
	 * @param input the address to match
	 *  @throws IOException
	 */
	@Async
	public void createTask(String jobId, String id, String input) throws IOException {

		BulkJobRequest bjr = new BulkJobRequest(jobId, id, input);
		GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();
		
		if (!(credentials instanceof IdTokenProvider)) {
			throw new IllegalArgumentException("Credentials are not an instance of IdTokenProvider.");
		}
		
		IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder()
				.setIdTokenProvider((IdTokenProvider) credentials).setTargetAudience(createTaskFunction).build();

		GenericUrl genericUrl = new GenericUrl(createTaskFunction);
		HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(tokenCredential);
		HttpTransport transport = new NetHttpTransport();
		HttpContent content = new JsonHttpContent(new JacksonFactory(), bjr.getJob());

		HttpRequest request = transport.createRequestFactory(adapter).buildPostRequest(genericUrl, content);
		request.execute();
	}
	
	@Async
	public void createTasks(Long jobId, BulkRequest[] addresses) throws IOException {
		
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
			BulkJobRequest bjr = new BulkJobRequest(String.valueOf(jobId), addresses[i].getId(), addresses[i].getAddress());
			
			HttpContent content = new JsonHttpContent(new JacksonFactory(), bjr.getJob());
			HttpRequest request = transport.createRequestFactory(adapter).buildPostRequest(genericUrl, content);
			request.execute();
		}
	}
	
	public @Data class BulkJobRequest {
		private Map<String, String> job;

		public BulkJobRequest(String jobId, String id, String address) {
			super();
			job = new HashMap<String, String>();
			job.put("jobId", jobId);
			job.put("id", id);
			job.put("address", address);
		}
	}
	
}
