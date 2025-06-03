package uk.gov.ons.bulk.service;

import static uk.gov.ons.bulk.util.BulkServiceConstants.BIG_QUERY_TABLE_PREFIX;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.exception.BulkAddressException;
import uk.gov.ons.bulk.validator.DownloadURL;

@Slf4j
@Service
public class DownloadService {

	@Value("${aims.project-number}")
	private String projectNumber;

	@Value("${aims.cloud-functions.create-signed-url-function}")
	private String createSignedUrlFunction;
	
	@Autowired
	private ResourceLoader resourceLoader;

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

		SignedUrlResponse signedUrlResponse;

		try {
			// process the HTTP response object
			signedUrlResponse = new ObjectMapper().readValue(response.getContent(), SignedUrlResponse.class);
			
			if (signedUrlResponse == null || signedUrlResponse.getSignedUrl().length() == 0) {
				throw new BulkAddressException("Signed URL is empty");
			}
		} finally {
			response.disconnect();
		}

		return signedUrlResponse.getSignedUrl();
	}

	public @Data class SignedUrlRequest {
		private Map<String, String> request;

		public SignedUrlRequest(String bucketName, String fileName) {
			request = new HashMap<String, String>();
			request.put("bucketName", bucketName);
			request.put("fileName", fileName);
		}
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	@Schema(type="string", example="https://storage.googleapis.com/results_8_179270555351/results_42.csv.gz?...")
	public static @Data class SignedUrlResponse {
		@DownloadURL
		private String signedUrl;
	}
	
	public InputStream getResultFile(String jobId, String filename) throws IOException {
		String gcsResultsBucket = String.format("%s%s_%s", BIG_QUERY_TABLE_PREFIX, jobId, projectNumber);
		String gcsFullFilePath = String.format("gs://%s/%s", gcsResultsBucket, filename);

		String gcsRegex = "^gs://results_(\\\\d+)_(\\\\d{12})/results_\\\\1\\\\.csv\\\\.gz$";

		Pattern pattern = Pattern.compile(gcsRegex);
		Matcher matcher = pattern.matcher(gcsFullFilePath);

		if (!matcher.matches()) {
				throw new IllegalArgumentException(
					"must be a valid GCS download URL like so:\n" +
					"gs://results_<digits>_<12digits>/results_<same digits>.csv.gz \n" +
					" Actually provided: " + gcsFullFilePath
				);
		}

		log.debug("gcsResultsBucket: " + gcsResultsBucket);
		log.debug("gcsFullFilePath: " + gcsFullFilePath);

		Resource gcsFile = resourceLoader.getResource(gcsFullFilePath);
		return gcsFile.getInputStream();
	}
}
