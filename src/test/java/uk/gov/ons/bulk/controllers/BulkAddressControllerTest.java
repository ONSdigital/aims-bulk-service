package uk.gov.ons.bulk.controllers;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.ui.Model;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import uk.gov.ons.bulk.util.Toolbox;

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


	@Mock
	private Toolbox utils;

	Properties queryReponse = new Properties();

	String queryReponseRef = "query.properties";


	@BeforeAll
	public void setUp() throws Exception {

		InputStream is = getClass().getClassLoader().getResourceAsStream(queryReponseRef);

		if (is != null) {
			queryReponse.load(is);
		} else {
			throw new FileNotFoundException("Query Property file not in classpath");
		}


		MockitoAnnotations.initMocks(this);

		when(utils.runQuery(anyString(),eq(bigquery))).thenAnswer(new Answer<ArrayList<FieldValueList>>() {
			public ArrayList<FieldValueList> answer(InvocationOnMock invocation) throws Throwable {

				Object[] args = invocation.getArguments();

				return getResponse((String) args[0]);
			}
		});


	}

	public ArrayList<FieldValueList> getResponse(String query) throws ClassNotFoundException, IOException, NoSuchAlgorithmException {


		String key = Toolbox.getInstance().convertToMd5(query);

		String result = (String) queryReponse.get(key);

		System.out.println(result);

		ArrayList<FieldValueList> fields = (ArrayList<FieldValueList>) Toolbox.getInstance().deserializeFromBase64(result);

		if(query != null)
			return fields;
		return null;

	}
	
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
		String peek = client.get().uri("/jobs").exchange().expectBody().returnResult().toString();
		client.get().uri("/jobs")
				.exchange()
				.expectStatus().isOk();
	}
}
