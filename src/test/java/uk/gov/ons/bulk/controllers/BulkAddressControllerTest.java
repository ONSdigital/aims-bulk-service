package uk.gov.ons.bulk.controllers;

import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

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
	
	@AfterAll
	public void tear() throws IOException {

		mockBackEnd.shutdown();
	}
	
	@Test
	void testIndex() {
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
		client.get().uri("/jobs")
				.exchange()
				.expectStatus().isOk();
	}
}
