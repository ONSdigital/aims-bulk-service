package uk.gov.ons.bulk.service;

import static uk.gov.ons.bulk.util.BulkServiceConstants.BIG_QUERY_TABLE_PREFIX;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.IdTokenCredentials;
import com.google.auth.oauth2.IdTokenProvider;

import lombok.Data;
import uk.gov.ons.bulk.exception.BulkAddressException;

@Service
public class DownloadService {

	@Value("${aims.project-number}")
	private String projectNumber;

	@Value("${aims.cloud-functions.create-signed-url-function}")
	private String createSignedUrlFunction;

	public String getSignedUrl(String jobId, String filename) throws IOException, BulkAddressException {

		String gcsResultsBucket = String.format("%s%s_%s", BIG_QUERY_TABLE_PREFIX, jobId, projectNumber);

		GoogleCredentials credentials = GoogleCredentials.getApplicationDefault();

		if (!(credentials instanceof IdTokenProvider)) {
			throw new IllegalArgumentException("Credentials are not an instance of IdTokenProvider.");
		}

		IdTokenCredentials tokenCredential = IdTokenCredentials.newBuilder()
				.setIdTokenProvider((IdTokenProvider) credentials).setTargetAudience(createSignedUrlFunction).build();

		GenericUrl genericUrl = new GenericUrl(createSignedUrlFunction);
		HttpCredentialsAdapter adapter = new HttpCredentialsAdapter(tokenCredential);
		HttpTransport transport = new NetHttpTransport();
		SignedUrlRequest signedUrlRequest = new SignedUrlRequest(gcsResultsBucket, filename);

		HttpContent content = new JsonHttpContent(new GsonFactory(), signedUrlRequest.getRequest());
		HttpRequest request = transport.createRequestFactory(adapter).buildPostRequest(genericUrl, content);
		HttpResponse response = request.execute();

		String signedUrlResponse = "";

		try {
			// process the HTTP response object
			signedUrlResponse = new ObjectMapper().readValue(response.getContent(), String.class);
			
			if (signedUrlResponse.length() == 0) {
				throw new BulkAddressException("Signed URL is empty");
			}
		} finally {
			response.disconnect();
		}

		return signedUrlResponse;
	}

	public @Data class SignedUrlRequest {
		private Map<String, String> request;

		public SignedUrlRequest(String bucketName, String fileName) {
			request = new HashMap<String, String>();
			request.put("bucketName", bucketName);
			request.put("fileName", fileName);
		}
	}
}
