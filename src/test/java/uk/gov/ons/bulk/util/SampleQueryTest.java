package uk.gov.ons.bulk.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;

import com.google.cloud.bigquery.BigQuery;
import org.apache.commons.codec.binary.Hex;

import com.google.cloud.bigquery.TableResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

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

//	public void main(String[] args) throws InterruptedException, IOException, NoSuchAlgorithmException {
@Test
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
				.append(" WHERE runid = %s;").toString();

		//String[] queries = {"SELECT count(*) FROM `bigquery-public-data.baseball.games_post_wide` LIMIT 1000",
		//		"SELECT count(*) FROM `bigquery-public-data.austin_crime.crime` LIMIT 1000"};

		String[] queries = {JOBS_QUERY};

		Properties prop = Toolbox.getInstance().getPropertyFile();
		
		
		for( String query : queries) {
			
			TableResult tableResults = Toolbox.getInstance().runQuery(query, bigquery);

			Iterator iter = tableResults.getValues().iterator();

			ArrayList<Object> fieldResults = new ArrayList<Object>();
			while (iter.hasNext())
			{
				fieldResults.add(iter.next());
			}
			
			String key = Toolbox.getInstance().convertToMd5(query);
			
			prop.setProperty(key, Toolbox.getInstance().serializeToBase64((fieldResults)));
		}
		
		
		FileOutputStream fr = new FileOutputStream("./src/test/resources/query.properties");
		prop.store(fr, null);
		
		fr.close();

	}

	
}
