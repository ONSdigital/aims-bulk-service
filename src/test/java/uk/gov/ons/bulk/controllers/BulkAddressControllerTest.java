package uk.gov.ons.bulk.controllers;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(Lifecycle.PER_CLASS)
public class BulkAddressControllerTest {
	
    @Autowired
    private WebTestClient client;
	
	private final ElasticsearchContainer elastic;
	private static MockWebServer mockBackEnd;
    
	public BulkAddressControllerTest() throws IOException {
		
		/*
		 *  Service is properly tested in AddressServiceTest.
		 *  ES and MockWebServer required to stop exceptions.
		 */
		elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.9.3");
		elastic.start();

		System.setProperty("spring.elasticsearch.rest.uris",
				elastic.getContainerIpAddress() + ":" + elastic.getFirstMappedPort());
		System.setProperty("spring.data.elasticsearch.client.reactive.endpoints",
				elastic.getContainerIpAddress() + ":" + elastic.getFirstMappedPort());
		
		mockBackEnd = new MockWebServer();
        mockBackEnd.start();
      	    
      	System.setProperty("aims.tokeniser.uri", String.format("http://localhost:%s", mockBackEnd.getPort()));
      	
      	mockBackEnd.enqueue(new MockResponse()
      	      .setBody("")
      	      .addHeader("Content-Type", "application/json"));
	}	
	
	@AfterAll
	public void tear() throws IOException {
		
		elastic.stop();
		elastic.close();
		mockBackEnd.shutdown();
	}
	
	@Test
	void testIndex() {
		client.get().uri("/")
	        .exchange()
	        .expectStatus().isOk();
	}
	
	@Test 
	void testUploadAuxAddresses() {
		
		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("file", new ClassPathResource("aux-addresses-test.csv")).contentType(MediaType.MULTIPART_FORM_DATA);
				
		// Returns page showing addresses that were attempted to load to ES and bad addresses.
		client.post().uri("/upload-csv-aux-file")
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(String.class)
			.consumeWith(response -> {
				// Does the response contain the 2 UPRN values uploaded?
				assertTrue(response.toString().contains("1234567891011"));
				assertTrue(response.toString().contains("1234567891012"));			
			});
	}	
	
	@Test 
	void testUploadUnitAddresses() {
		
		MultipartBodyBuilder multipartBodyBuilder = new MultipartBodyBuilder();
		multipartBodyBuilder.part("file", new ClassPathResource("unit-addresses-test.csv")).contentType(MediaType.MULTIPART_FORM_DATA);
				
		// Returns page showing addresses that were attempted to load to ES and bad addresses.
		client.post().uri("/upload-csv-unit-file")
			.contentType(MediaType.MULTIPART_FORM_DATA)
			.body(BodyInserters.fromMultipartData(multipartBodyBuilder.build()))
			.exchange()
			.expectStatus().isOk()
			.expectBody(String.class)
			.consumeWith(response -> {
				// Does the response contain the 2 UPRN values uploaded?
				assertTrue(response.toString().contains("8881000006833"));
				assertTrue(response.toString().contains("8881000008109"));			
			});
	}	
	
	
}
