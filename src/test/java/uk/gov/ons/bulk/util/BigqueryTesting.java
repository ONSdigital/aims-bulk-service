//package uk.gov.ons.bulk.util;
//
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.ArgumentMatchers.eq;
//import static org.mockito.Mockito.when;
//
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStream;
//import java.security.NoSuchAlgorithmException;
//import java.util.ArrayList;
//import java.util.Properties;
//
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.mockito.Mock;
//import org.mockito.invocation.InvocationOnMock;
//import org.mockito.stubbing.Answer;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.ActiveProfiles;
//
//import com.google.cloud.bigquery.BigQuery;
//import com.google.cloud.bigquery.FieldValueList;
//
//// This class exists ONLY to check the big query test framework is working
//// The actual unit tests are in the appropriate classes
//
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//@ActiveProfiles("test")
//public class BigqueryTesting {
//
//	@Autowired
//	BigQuery bigquery;
//
//	@Value("${spring.cloud.gcp.project-id}")
//	private String projectId;
//
//	@Value("${spring.cloud.gcp.bigquery.dataset-name}")
//	private String datasetName;
//
//	@Value("${aims.bigquery.info-table}")
//	private String infoTable;
//
//	@Mock private Toolbox utils;
//	
//	Properties queryReponse = new Properties();
//	String queryReponseRef = "query.properties";
//
//	@BeforeAll
//	public void setUp() throws Exception {
//
//		InputStream is = getClass().getClassLoader().getResourceAsStream(queryReponseRef);
//
//		if (is != null) {
//			queryReponse.load(is);
//		} else {
//			throw new FileNotFoundException("Query Property file not in classpath");
//		}
//		
//    	when(utils.runQuery(anyString(),eq(bigquery))).thenAnswer(new Answer<ArrayList<FieldValueList>>() {
//		    public ArrayList<FieldValueList> answer(InvocationOnMock invocation) throws Throwable {
//		      
//		    	Object[] args = invocation.getArguments();
//
//		    	return getResponse((String) args[0]);
//		    }
//		  });
//	}
//
//	@Test
//	public void testJobsOutput() throws InterruptedException {
//
//		String BASE_DATASET_QUERY = new StringBuilder()
//				.append("SELECT * FROM ")
//				.append(projectId)
//				.append(".")
//				.append(datasetName).toString();
//
//		String INFO_TABLE_QUERY = new StringBuilder()
//				.append(BASE_DATASET_QUERY)
//				.append(".")
//				.append(infoTable).toString();
//
//		String JOBS_QUERY = new StringBuilder()
//				.append(INFO_TABLE_QUERY)
//				.append(";").toString();
//
//		ArrayList<FieldValueList> tableResults = utils.runQuery(JOBS_QUERY,bigquery);
//		
//		String resultCount = "";
//		
//		for (FieldValueList x : tableResults){
//			
//			resultCount = x.get(0).getStringValue();
//			
//		}
//		assert resultCount.equalsIgnoreCase("101");
//
//	}
//
//
//	public ArrayList<FieldValueList> getResponse(String query) throws ClassNotFoundException, IOException, NoSuchAlgorithmException {
//
//		String key = Toolbox.getInstance().convertToMd5(query);
//		String result = (String) queryReponse.get(key);
//		System.out.println(result);
//
//		ArrayList<FieldValueList> fields = (ArrayList<FieldValueList>) Toolbox.getInstance().deserializeFromBase64(result);
//
//		if(query != null)
//		    return fields;
//		return null;
//		
//	}
//
//}
