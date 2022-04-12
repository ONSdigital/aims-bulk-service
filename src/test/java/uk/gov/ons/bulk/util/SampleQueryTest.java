package uk.gov.ons.bulk.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;

import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;

// This class exists ONLY to seed the query cache for BigQuery testing

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SampleQueryTest {

	@Autowired
	BigQuery bigquery;

	@Value("${spring.cloud.gcp.project-id}")
	private String projectId;

	@Value("${spring.cloud.gcp.bigquery.dataset-name}")
	private String datasetName;

	@Value("${aims.bigquery.info-table}")
	private String infoTable;

	// Uncomment Test annotation to regenerate query.properties file
//@Test
	public void SampleQueryTest() throws InterruptedException, IOException, NoSuchAlgorithmException {

		String BASE_DATASET_QUERY = new StringBuilder()
				.append("SELECT * FROM ")
				.append(projectId)
				.append(".")
				.append(datasetName).toString();

		String INFO_TABLE_QUERY = new StringBuilder()
				.append(BASE_DATASET_QUERY)
				.append(".")
				.append(infoTable).toString();

		String JOBS_QUERY = new StringBuilder()
				.append(INFO_TABLE_QUERY)
				.append(";").toString();

		String JOB_QUERY = new StringBuilder()
				.append(INFO_TABLE_QUERY)
				.append(" WHERE runid = 14;").toString();

		String RESULT_QUERY = new StringBuilder()
				.append(BASE_DATASET_QUERY)
				.append(".")
				.append("results14;").toString();

		String MAX_QUERY = new StringBuilder()
				.append("SELECT MAX(runid) FROM ")
				.append(projectId)
				.append(".")
				.append(datasetName)
				.append(".")
				.append(infoTable).toString();

		String[] queries = {JOBS_QUERY,JOB_QUERY,RESULT_QUERY,MAX_QUERY};

		Properties prop = Toolbox.getInstance().getPropertyFile();
		
	// get field results part of TableResults object (problem with serializing whole object)
		for( String query : queries) {
			
			ArrayList<FieldValueList> fieldResults = Toolbox.getInstance().runQuery(query, bigquery);

			String key = Toolbox.getInstance().convertToMd5(query);
			
			prop.setProperty(key, Toolbox.getInstance().serializeToBase64((fieldResults)));
		}
		
		
		FileOutputStream fr = new FileOutputStream("./src/test/resources/query.properties");
		prop.store(fr, null);
		
		fr.close();

	}

	
}
