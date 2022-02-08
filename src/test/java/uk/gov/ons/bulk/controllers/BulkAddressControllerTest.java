package uk.gov.ons.bulk.controllers;

import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.springframework.web.reactive.function.BodyInserters;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
public class BulkAddressControllerTest {
	
    @Autowired
    private WebTestClient client;
	private static MockWebServer mockBackEnd;
    
	public BulkAddressControllerTest() throws IOException {

		mockBackEnd = new MockWebServer();
        mockBackEnd.start();      	
      	mockBackEnd.enqueue(new MockResponse()
      	      .setBody("")
      	      .addHeader("Content-Type", "application/json"));
	}

	@Autowired
	BigQuery bigquery;

	@Value("${spring.cloud.gcp.project-id}")
	private String projectId;

	@Value("${spring.cloud.gcp.bigquery.dataset-name}")
	private String datasetName;

	@Value("${aims.bigquery.info-table}")
	private String infoTable;


	@AfterAll
	public void tear() throws IOException {

		mockBackEnd.shutdown();
	}
	
	@Test
	void testIndex() {
		String peek = client.get().uri("/").exchange().expectBody().returnResult().toString();
		client.get().uri("/")
	        .exchange()
	        .expectStatus().isOk();
	}
	
	@Test
	void testError() {
		client.get().uri("/error")
		.exchange()
		.expectStatus().isOk()
		.expectBody(String.class)
		.consumeWith(response -> {
			assertTrue(response.toString().contains("Whoops"));
		});
	}

	@Test
	void testGetAllBulkRequestProgress () {
	//	String peek = client.get().uri("/jobs?test=true").exchange().expectBody().returnResult().toString();
		client.get().uri("/jobs?test=true")
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	void testGetBulkRequestProgress () {
//		String peek = client.get().uri("/bulk-progress/14?test=true").exchange().expectBody().returnResult().toString();
		client.get().uri("/bulk-progress/14?test=true")
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	void testGetBulkResults () {
//		String peek = client.get().uri("/bulk-result/14?test=true").exchange().expectBody().returnResult().toString();
		client.get().uri("/bulk-result/14?test=true")
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	void testrunBulkRequest () {
		String postJson = "{"+
				"\"addresses\":[{" +
			"\"id\" : \"1\"," +
					"\"address\": \"4 Gate Reach Exeter EX2 6GA\"" +
		"},{" +
			"\"id\" : \"2\"," +
					"\"address\": \"Costa Coffee, 12 Bedford Street, Exeter\"" +
		"}]}";

		client.post().uri("/bulk?test=true")
				.contentType(MediaType.APPLICATION_JSON)
				.body(BodyInserters.fromValue(postJson))
				.exchange()
				.expectBody(String.class)
				.consumeWith(response -> {
					assertTrue(response.toString().contains("Submitted"));
				});
	}


}
