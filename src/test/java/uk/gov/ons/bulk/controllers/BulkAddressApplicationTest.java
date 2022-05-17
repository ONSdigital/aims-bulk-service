package uk.gov.ons.bulk.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import com.google.api.client.http.HttpHeaders;
import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.Field;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.Schema;
import com.google.cloud.bigquery.StandardSQLTypeName;
import com.google.cloud.bigquery.TableId;

import uk.gov.ons.bulk.entities.BulkRequest;
import uk.gov.ons.bulk.entities.BulkRequestContainer;
import uk.gov.ons.bulk.service.CloudTaskService;
import uk.gov.ons.bulk.util.QueryFuncs;
import uk.gov.ons.bulk.util.Toolbox;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BulkAddressApplicationTest {

    @Autowired
    BigQuery bigquery;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.bigquery.dataset-name}")
    private String datasetName;

    @Value("${aims.bigquery.info-table}")
    private String infoTable;

    @Value("${aims.cloud-functions.create-cloud-task-function}")
    private String createTaskFunction;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CloudTaskService cloudTaskService;

    Properties queryReponse = new Properties();
    String queryReponseRef = "query.properties";

    private String BASE_DATASET_QUERY;
    private String INFO_TABLE_QUERY;
    private String JOBS_QUERY;
    private String JOB_QUERY;
    private String RESULT_QUERY;
    private String MAX_QUERY;
    
    MockedStatic<QueryFuncs> theMock;
        
    @PostConstruct
    public void postConstruct() {

         BASE_DATASET_QUERY = new StringBuilder()
                .append("SELECT * FROM ")
                .append(projectId)
                .append(".")
                .append(datasetName).toString();

         INFO_TABLE_QUERY = new StringBuilder()
                .append(BASE_DATASET_QUERY)
                .append(".")
                .append(infoTable).toString();

         JOBS_QUERY = new StringBuilder()
                .append(INFO_TABLE_QUERY)
                .append(";").toString();

         JOB_QUERY = new StringBuilder()
                .append(INFO_TABLE_QUERY)
                .append(" WHERE runid = 14;").toString();

         RESULT_QUERY = new StringBuilder()
                .append(BASE_DATASET_QUERY)
                .append(".")
                .append("results14;").toString();

         MAX_QUERY = new StringBuilder()
                .append("SELECT MAX(runid) FROM ")
                .append(projectId)
                .append(".")
                .append(datasetName)
                .append(".")
                .append(infoTable).toString();
         
         // use mockstatic to make it use cached queries
         theMock = Mockito.mockStatic(QueryFuncs.class);
    }
    
    @BeforeAll
    public void setUp() throws Exception {

        // load up cached responses
        InputStream is = getClass().getClassLoader().getResourceAsStream(queryReponseRef);

        if (is != null) {
            queryReponse.load(is);
        } else {
            throw new FileNotFoundException("Query Property file not in classpath");
        }        
    }

    public ArrayList<FieldValueList> getResponse(String query) throws ClassNotFoundException, IOException, NoSuchAlgorithmException {

        String key = Toolbox.getInstance().convertToMd5(query);
        String result = (String) queryReponse.get(key);
        ArrayList<FieldValueList> fields = (ArrayList<FieldValueList>) Toolbox.getInstance().deserializeFromBase64(result);

        if(query != null)
            return fields;
        return null;
    }
    
    private static Stream<Arguments> addJobIds() {
        return Stream.of(
                Arguments.of("14"));
    }
    
    private static Stream<BulkRequestContainer> bulkRequestObject() {
    	
		BulkRequest address1 = new BulkRequest();
		address1.setId("1");
		address1.setAddress("4 Gate Reach Exeter EX2 6GA");
		BulkRequest address2 = new BulkRequest();
		address2.setId("2");
		address2.setAddress("Costa Coffee, 12 Bedford Street, Exeter");
		BulkRequestContainer bulkRequestContainer = new BulkRequestContainer();
		bulkRequestContainer.setAddresses(new BulkRequest[] { address1, address2 });
		
		return Stream.of(bulkRequestContainer);
    }

    public String getOK() {return "OK";}

	@Test
	public void testGetBulkRequestProgress() throws Exception {

		// use mockstatic to make it use cached queries
		theMock.when(() -> QueryFuncs.runQuery(JOBS_QUERY, bigquery)).thenReturn(getResponse(JOBS_QUERY));

		mockMvc.perform(MockMvcRequestBuilders.get("/jobs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.[0]runid", Is.is("90")))
				.andExpect(jsonPath("$.[5]userid", Is.is("bigqueryboy")))
				.andExpect(jsonPath("$.[4]status", Is.is("waiting")))
				.andExpect(jsonPath("$.[0]totalrecs", Is.is("99")))
				.andExpect(jsonPath("$.[3]recssofar", Is.is("0")))
				.andExpect(jsonPath("$.[12]startdate", is(nullValue())))
				.andExpect(jsonPath("$.[14]enddate", is(nullValue())))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@ParameterizedTest
	@MethodSource("addJobIds")
	public void testGetBulkRequestProgress(@PathVariable(required = true, name = "jobid") String jobid)
			throws Exception {

		// use mockstatic to make it use cached queries
		theMock.when(() -> QueryFuncs.runQuery(String.format(JOB_QUERY, jobid), bigquery))
				.thenReturn(getResponse(String.format(JOB_QUERY, jobid)));

		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-progress/" + jobid)).andExpect(status().isOk())
				.andExpect(jsonPath("$.runid", Is.is("14")))
				.andExpect(jsonPath("$.userid", Is.is("bigqueryboy")))
				.andExpect(jsonPath("$.status", Is.is("waiting")))
				.andExpect(jsonPath("$.totalrecs", Is.is("2")))
				.andExpect(jsonPath("$.recssofar", Is.is("0")))
				.andExpect(jsonPath("$.startdate", is(nullValue())))
				.andExpect(jsonPath("$.enddate", is(nullValue())))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@ParameterizedTest
	@MethodSource("addJobIds")
	public void getBulkResults(@PathVariable(required = true, name = "jobid") String jobid) throws Exception {

		theMock.when(() -> QueryFuncs.runQuery(String.format(RESULT_QUERY, jobid), bigquery))
				.thenReturn(getResponse(String.format(RESULT_QUERY, jobid)));

		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-result/" + jobid)).andExpect(status().isOk())
				.andExpect(jsonPath("$.results[1].response.response.addresses[0].classificationCode", Is.is("CR07")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));

	}

    @ParameterizedTest
    @MethodSource("bulkRequestObject")
    public void runBulkRequest(@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

            Schema schema = Schema.of(
                    Field.of("id", StandardSQLTypeName.INT64),
                    Field.of("inputaddress", StandardSQLTypeName.STRING),
                    Field.of("response", StandardSQLTypeName.STRING));

            theMock.when(() -> QueryFuncs.runQuery(MAX_QUERY,bigquery))
                    .thenReturn(getResponse(MAX_QUERY));

            long newKey = 0;
            for (FieldValueList row : getResponse(MAX_QUERY)) {
                for (FieldValue val : row) {
                    newKey = val.getLongValue() + 1;
                    System.out.println((String.format("newkey:%d", newKey)));
                }
            }

            String tableName = "results" + newKey;

            theMock.when(() -> QueryFuncs.createTable(bigquery, datasetName, tableName, schema))
                    .thenAnswer((Answer<Void>) invocation -> null);

            String iTableName = "bulkinfo";
            Map<String, Object> row1Data = new HashMap<>();
            row1Data.put("runid", newKey);
            row1Data.put("userid", "bigqueryboy");
            row1Data.put("status", "waiting");
            row1Data.put("totalrecs", 99);
            row1Data.put("recssofar", 0);
            TableId tableId = TableId.of(datasetName, iTableName);

            theMock.when(() -> QueryFuncs.InsertRow(bigquery, tableId, row1Data))
                    .thenReturn(getOK());

            BulkRequest testBulkRequest1 = new BulkRequest();
            testBulkRequest1.setId("1");
            testBulkRequest1.setAddress("4 Gate Reach Exeter EX2 6GA");
            BulkRequest testBulkRequest2 = new BulkRequest();
            testBulkRequest2.setId("2");
            testBulkRequest2.setAddress("Costa Coffee, 12 Bedford Street, Exeter");
            BulkRequest[] bulkRequests = {testBulkRequest1, testBulkRequest2};

		    HttpHeaders headers = new HttpHeaders();
		    headers.set("user","bigqueryboy");

            doNothing().when(cloudTaskService).createTasks(newKey, bulkRequests, null,headers);
            
    		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
    				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
    				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
    				.andExpect(jsonPath("$.jobId", Is.is("102")))
    				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
	@Test
	public void bulkProgressNoJobId() throws Exception {
		
		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-progress/ ")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("jobid: jobid is mandatory")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("jobid: jobid is mandatory"))))		
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void bulkResultNoJobId() throws Exception {
		
		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-result/ ")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("jobid: jobid is mandatory")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("jobid: jobid is mandatory"))))		
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void bulkPostRequestInvalidBulkRequestNoAddresses() throws Exception {
		
		BulkRequestContainer bulkRequestContainer = new BulkRequestContainer();

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("'addresses': rejected value [null]")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem("addresses: addresses is mandatory")))		
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void bulkPostRequestInvalidBulkRequestEmptyAddresses() throws Exception {

		BulkRequestContainer bulkRequestContainer = new BulkRequestContainer();
		bulkRequestContainer.setAddresses(new BulkRequest[] {});

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("'addresses': rejected value [{}]")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem("addresses: addresses cannot be empty")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
    
	@Test
	public void bulkPostRequestInvalidBulkRequestMissingId() throws Exception {

		BulkRequest address1 = new BulkRequest();
		address1.setId("");
		address1.setAddress("4 Gate Reach Exeter EX2 6GA");
		BulkRequest address2 = new BulkRequest();
		address2.setId("2");
		address2.setAddress("Costa Coffee, 12 Bedford Street, Exeter");
		BulkRequestContainer bulkRequestContainer = new BulkRequestContainer();
		bulkRequestContainer.setAddresses(new BulkRequest[] { address1, address2 });

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("addresses[0].id': rejected value []")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem("addresses[0].id: id is mandatory")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void bulkPostRequestInvalidBulkRequestMissingAddress() throws Exception {

		BulkRequest address1 = new BulkRequest();
		address1.setId("1");
		address1.setAddress("4 Gate Reach Exeter EX2 6GA");
		BulkRequest address2 = new BulkRequest();
		address2.setId("2");
		address2.setAddress("");
		BulkRequestContainer bulkRequestContainer = new BulkRequestContainer();
		bulkRequestContainer.setAddresses(new BulkRequest[] { address1, address2 });

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("addresses[1].address': rejected value []")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem("addresses[1].address: address is mandatory")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongUpperLimit(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("limitperaddress", "101")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("limitperaddress: must be less than or equal to 100")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("limitperaddress: must be less than or equal to 100"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongLowerLimit(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("limitperaddress", "0")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("limitperaddress: must be greater than or equal to 1")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("limitperaddress: must be greater than or equal to 1"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongClassification(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("classificationfilter", ",")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("classificationfilter may not contain a list and/or a wildcard")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("classificationfilter may not contain a list and/or a wildcard"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongHistorical(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("historical", "xyz")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("historical must be true or false")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("historical must be true or false"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongUpperMatch(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("matchthreshold", "101")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("matchthreshold: must be less than or equal to 100")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("matchthreshold: must be less than or equal to 100"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongLowerMatch(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("matchthreshold", "0")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("matchthreshold: must be greater than or equal to 1")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("matchthreshold: must be greater than or equal to 1"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongVerbose(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("verbose", "xyz")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("verbose must be true or false")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("verbose must be true or false"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongEpoch(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("epoch", "100")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("epoch must be one of 89, 87, 80, 39")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("epoch must be one of 89, 87, 80, 39"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongExcludeEng(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("excludeengland", "xyz")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("excludeengland must be true or false")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("excludeengland must be true or false"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongExcludeScot(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("excludescotland", "xyz")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("excludescotland must be true or false")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("excludescotland must be true or false"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongExcludeWales(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("excludewales", "xyz")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("excludewales must be true or false")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("excludewales must be true or false"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterWrongExcludeNI(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("excludenorthernireland", "xyz")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("excludenorthernireland must be true or false")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("excludenorthernireland must be true or false"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("bulkRequestObject")
	public void bulkPostRequestInvalidBulkRequestParameterMultipleWrong(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {
		
		String classError = "runBulkRequest.classificationfilter: classificationfilter may not contain a list and/or a wildcard";
		String epochError = "runBulkRequest.epoch: epoch must be one of 89, 87, 80, 39";
		String excludeenglandError = "runBulkRequest.excludeengland: excludeengland must be true or false";
		String excludenorthernirelandError = "runBulkRequest.excludenorthernireland: excludenorthernireland must be true or false";
		
		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.param("classificationfilter", "RD50,RD49,*")
				.param("epoch", "100")
				.param("excludeengland", "xyz")
				.param("excludenorthernireland", "xyz")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString(classError)))
				.andExpect(jsonPath("$.message", containsString(epochError)))
				.andExpect(jsonPath("$.message", containsString(excludeenglandError)))
				.andExpect(jsonPath("$.message", containsString(excludenorthernirelandError)))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(4)))
				.andExpect(jsonPath("$.errors", hasItem("uk.gov.ons.bulk.controllers.BulkAddressController " + classError)))
				.andExpect(jsonPath("$.errors", hasItem("uk.gov.ons.bulk.controllers.BulkAddressController " + epochError)))
				.andExpect(jsonPath("$.errors", hasItem("uk.gov.ons.bulk.controllers.BulkAddressController " + excludeenglandError)))
				.andExpect(jsonPath("$.errors", hasItem("uk.gov.ons.bulk.controllers.BulkAddressController " + excludenorthernirelandError)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
}
