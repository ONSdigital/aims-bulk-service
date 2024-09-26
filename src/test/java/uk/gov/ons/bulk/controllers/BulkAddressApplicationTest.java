package uk.gov.ons.bulk.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.hamcrest.core.Is;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;

import uk.gov.ons.bulk.entities.BulkInfo;
import uk.gov.ons.bulk.entities.BulkRequest;
import uk.gov.ons.bulk.entities.BulkRequestContainer;
import uk.gov.ons.bulk.entities.IdsBulkInfo;
import uk.gov.ons.bulk.exception.BulkAddressException;
import uk.gov.ons.bulk.repository.BulkStatusRepository;
import uk.gov.ons.bulk.service.BulkStatusService;
import uk.gov.ons.bulk.service.CloudTaskService;
import uk.gov.ons.bulk.service.DownloadService;
import uk.gov.ons.bulk.util.QueryFuncs;
import uk.gov.ons.bulk.util.Toolbox;

/*
 * .andDo(MockMvcResultHandlers.print()) helps to debug individual tests
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext
public class BulkAddressApplicationTest {

    @Autowired
    BigQuery bigquery;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.bigquery.dataset-name}")
    private String datasetName;

    @Value("${aims.cloud-functions.create-cloud-task-function}")
    private String createTaskFunction;
    
	@Value("${aims.max-records-per-job}")
	private int maxRecordsPerJob;
	
	@Value("${aims.min-records-per-job}")
	private int minRecordsPerJob;

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CloudTaskService cloudTaskService;
    
    @Autowired
    private BulkStatusService bulkStatusService;
    
    @MockBean
    private BulkStatusRepository bulkStatusRepository;
    
    @MockBean
    private DownloadService downloadService;

    Properties queryReponse = new Properties();
    String queryReponseRef = "query.properties";
   
    MockedStatic<QueryFuncs> theMock;
    
    private LocalDateTime now;
        
    @PostConstruct
    public void postConstruct() {        
         // use mockstatic to make it use cached queries
         theMock = Mockito.mockStatic(QueryFuncs.class);
    }
    
    @BeforeAll
    public void setUp() throws Exception {

        // load up cached responses
        InputStream is = getClass().getClassLoader().getResourceAsStream(queryReponseRef);
        now = LocalDateTime.now();

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
    
    private static Stream<Arguments> addIdsJobIds() {
        return Stream.of(
                Arguments.of("ids-job-xyz"));
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
    
    private static Stream<BulkRequestContainer> bulkRequestObjectOverMax() {
    	
		BulkRequest address1 = new BulkRequest();
		address1.setId("1");
		address1.setAddress("4 Gate Reach Exeter EX2 6GA");
		BulkRequest address2 = new BulkRequest();
		address2.setId("2");
		address2.setAddress("Costa Coffee, 12 Bedford Street, Exeter");
		BulkRequest address3 = new BulkRequest();
		address3.setId("3");
		address3.setAddress("4 Gate Reach Exeter EX2 6GA");
		BulkRequest address4 = new BulkRequest();
		address4.setId("4");
		address4.setAddress("Costa Coffee, 12 Bedford Street, Exeter");
		BulkRequest address5 = new BulkRequest();
		address5.setId("5");
		address5.setAddress("4 Gate Reach Exeter EX2 6GA");
		BulkRequest address6 = new BulkRequest();
		address6.setId("6");
		address6.setAddress("Costa Coffee, 12 Bedford Street, Exeter");
		BulkRequestContainer bulkRequestContainer = new BulkRequestContainer();
		bulkRequestContainer.setAddresses(new BulkRequest[] { address1, address2, address3, address4, address5, address6 });
		
		return Stream.of(bulkRequestContainer);
    }
    
    private static Stream<BulkRequestContainer> bulkRequestObjectUnderMin() {
    	
		BulkRequest address1 = new BulkRequest();
		address1.setId("1");
		address1.setAddress("4 Gate Reach Exeter EX2 6GA");
		BulkRequestContainer bulkRequestContainer = new BulkRequestContainer();
		bulkRequestContainer.setAddresses(new BulkRequest[] { address1 });
		
		return Stream.of(bulkRequestContainer);
    }

    public String getOK() {return "OK";}
    
	@ParameterizedTest
	@MethodSource("addJobIds")
	public void testGetBulkRequestProgressInProgress(@PathVariable(required = true, name = "jobid") String jobid)
			throws Exception {

		BulkInfo bulkInfo = new BulkInfo("bob", "in-progress", 107, 45);
        bulkInfo.setJobid(14);
        bulkInfo.setStartdate(now);
		List<BulkInfo> bulkInfos = Arrays.asList(bulkInfo);

        when(bulkStatusRepository.queryJob(Mockito.any(Long.class))).thenReturn(bulkInfos);

		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-progress/" + jobid)).andExpect(status().isOk())
				.andExpect(jsonPath("$.jobid", Is.is(14)))
				.andExpect(jsonPath("$.userid", Is.is("bob")))
				.andExpect(jsonPath("$.status", Is.is("in-progress")))
				.andExpect(jsonPath("$.totalrecs", Is.is(107)))
				.andExpect(jsonPath("$.recssofar", Is.is(45)))
				.andExpect(jsonPath("$.startdate", Is.is(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("addJobIds")
	public void testGetBulkRequestProgressFinished(@PathVariable(required = true, name = "jobid") String jobid)
			throws Exception {

		BulkInfo bulkInfo = new BulkInfo("bob", "finished", 107, 107);
        bulkInfo.setJobid(14);
        bulkInfo.setStartdate(now);
        bulkInfo.setEnddate(now.plusHours(2));
		List<BulkInfo> bulkInfos = Arrays.asList(bulkInfo);

        when(bulkStatusRepository.queryJob(Mockito.any(Long.class))).thenReturn(bulkInfos);

		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-progress/" + jobid)).andExpect(status().isOk())
				.andExpect(jsonPath("$.jobid", Is.is(14)))
				.andExpect(jsonPath("$.userid", Is.is("bob")))
				.andExpect(jsonPath("$.status", Is.is("finished")))
				.andExpect(jsonPath("$.totalrecs", Is.is(107)))
				.andExpect(jsonPath("$.recssofar", Is.is(107)))
				.andExpect(jsonPath("$.startdate", Is.is(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(jsonPath("$.enddate", Is.is(now.plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void bulkProgressNoJobId() throws Exception {
		
		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-progress/ ")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("jobid: jobid is mandatory and must be an integer")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("jobid: jobid is mandatory and must be an integer"))))		
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

    @ParameterizedTest
    @MethodSource("bulkRequestObject")
    public void runBulkRequest(@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

    	long newKey = 102;
            
		BulkInfo bulkInfo = new BulkInfo("bigqueryboy", "in-progress", 2, 0);
        bulkInfo.setJobid(newKey);
        bulkInfo.setStartdate(now);
        
        BulkRequest testBulkRequest1 = new BulkRequest();
        testBulkRequest1.setId("1");
        testBulkRequest1.setAddress("4 Gate Reach Exeter EX2 6GA");
        BulkRequest testBulkRequest2 = new BulkRequest();
        testBulkRequest2.setId("2");
        testBulkRequest2.setAddress("Costa Coffee, 12 Bedford Street, Exeter");
        BulkRequest[] bulkRequests = {testBulkRequest1, testBulkRequest2};
        
	    String userName = "bigqueryboy";

        when(bulkStatusRepository.saveJob(Mockito.any(BulkInfo.class))).thenReturn(102L);
        doNothing().when(cloudTaskService).createTasks(newKey, bulkRequests, 2L, null, userName);
        
		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.jobId", Is.is("102")))
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
	public void bulkPostRequestInvalidBulkRequestParameterWrongPafdefault(
			@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
						.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("pafdefault", "xyz")
						.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("pafdefault must be true or false")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("pafdefault must be true or false"))))
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
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer)).param("matchthreshold", "-1")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("matchthreshold: must be greater than or equal to 0")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("matchthreshold: must be greater than or equal to 0"))))
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
				.andExpect(jsonPath("$.message", containsString("epoch must be one of 109, 108, 107")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("epoch must be one of 109, 108, 107"))))
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
		String epochError = "runBulkRequest.epoch: epoch must be one of 109, 108, 107";
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
	
	@Test
	public void bulkResultGetRequestMissingJobId() throws Exception {
		
		String jobIdError = "getBulkResults.jobId: jobid is mandatory and must be an integer";
		
		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-result/ ")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("jobId: jobid is mandatory and must be an integer")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem("uk.gov.ons.bulk.controllers.BulkAddressController " + jobIdError)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
    public void runBulkResultRequest() throws Exception {

    	String filename = String.format("results_%s.csv.gz", 1);
		BulkInfo bulkInfo = new BulkInfo("mrrobot", "results-exported", 2, 2);
        bulkInfo.setStartdate(now);
        List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	bulkInfos.add(bulkInfo);
        
        when(downloadService.getSignedUrl("1", filename)).thenReturn("https://alink");
        when(bulkStatusService.queryJob(1L)).thenReturn(bulkInfos);
        
		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-result/1")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.file", Is.is(filename)))
				.andExpect(jsonPath("$.signedUrl", Is.is("https://alink")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
	
	@Test
    public void runBulkResultThrowIOException() throws Exception {
    	
    	String filename = String.format("results_%s.csv.gz", 1);
		BulkInfo bulkInfo = new BulkInfo("mrrobot", "results-exported", 2, 2);
        bulkInfo.setStartdate(now);
        List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	bulkInfos.add(bulkInfo);
        
        when(downloadService.getSignedUrl("1", filename)).thenThrow(new IOException("An IO Exception"));
        when(bulkStatusService.queryJob(1L)).thenReturn(bulkInfos);
        
		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-result/1")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.error", containsString("An IO Exception")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
	
	@Test
    public void runBulkResultThrowBulkAddressException() throws Exception {
    	
    	String filename = String.format("results_%s.csv.gz", 1);
		BulkInfo bulkInfo = new BulkInfo("mrrobot", "results-exported", 2, 2);
        bulkInfo.setStartdate(now);
        List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	bulkInfos.add(bulkInfo);
        
        when(downloadService.getSignedUrl("1", filename)).thenThrow(new BulkAddressException("Signed URL is empty"));
        when(bulkStatusService.queryJob(1L)).thenReturn(bulkInfos);
        
		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-result/1")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.error", containsString("Signed URL is empty")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
	@Test
    public void runBulkResultRequestNonExistent() throws Exception {
    	
    	List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
        when(bulkStatusService.queryJob(99L)).thenReturn(bulkInfos);
        
		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-result/99")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error", Is.is(String.format("Job ID %s not found on the system", 99L))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
	@Test
    public void runBulkResultRequestNotDownloadable() throws Exception {

		BulkInfo bulkInfo = new BulkInfo("mrrobot", "processing-finished", 2, 2);
        bulkInfo.setStartdate(now);
        List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	bulkInfos.add(bulkInfo);
        
        when(bulkStatusService.queryJob(1L)).thenReturn(bulkInfos);
        
		mockMvc.perform(MockMvcRequestBuilders.get("/bulk-result/1")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error", Is.is(String.format("Job ID %s is not currently downloadable", 1L))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
    
	@Test
	public void jobsRequestWrongStatus() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/jobs?status=xyz&userid=mrrobot")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("status: status must be in-progress, processing-finished, results-ready, results-exported, failed or blank")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("status: status must be in-progress, processing-finished, results-ready, results-exported, failed or blank"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void jobsRequest() throws Exception {
    	
		long newKey1 = 102;
		long newKey2 = 44;
		BulkInfo bulkInfo1 = new BulkInfo("mrrobot", "processing-finished", 2, 2);
		BulkInfo bulkInfo2 = new BulkInfo("mrrobot", "processing-finished", 10, 10);
        bulkInfo1.setJobid(newKey1);
        bulkInfo2.setJobid(newKey2);
        bulkInfo1.setStartdate(now);
        bulkInfo2.setStartdate(now);
        List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	bulkInfos.add(bulkInfo1);
    	bulkInfos.add(bulkInfo2);
    	        
        when(bulkStatusService.getJobs("mrrobot", "processing-finished")).thenReturn(bulkInfos);

		mockMvc.perform(MockMvcRequestBuilders.get("/jobs?status=processing-finished&userid=mrrobot")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.jobs").isArray()).andExpect(jsonPath("$.jobs", hasSize(2)))
				.andExpect(jsonPath("$.jobs[0].jobid", Is.is(102)))
				.andExpect(jsonPath("$.jobs[0].userid", Is.is("mrrobot")))
				.andExpect(jsonPath("$.jobs[0].status", Is.is("processing-finished")))
				.andExpect(jsonPath("$.jobs[0].totalrecs", Is.is(2)))
				.andExpect(jsonPath("$.jobs[0].recssofar", Is.is(2)))
				.andExpect(jsonPath("$.jobs[0].startdate", Is.is(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(jsonPath("$.jobs[1].jobid", Is.is(44)))
				.andExpect(jsonPath("$.jobs[1].userid", Is.is("mrrobot")))
				.andExpect(jsonPath("$.jobs[1].status", Is.is("processing-finished")))
				.andExpect(jsonPath("$.jobs[1].totalrecs", Is.is(10)))
				.andExpect(jsonPath("$.jobs[1].recssofar", Is.is(10)))
				.andExpect(jsonPath("$.jobs[1].startdate", Is.is(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void idsJobsRequestWrongStatus() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/ids/jobs?status=xyz&userid=mrrobot")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("status: status must be in-progress, processing-finished, results-ready, results-deleted, failed or blank")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("status: status must be in-progress, processing-finished, results-ready, results-deleted, failed or blank"))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void idsJobsRequest() throws Exception {
    	
		long newKey1 = 99;
		long newKey2 = 88;
		IdsBulkInfo idsBulkInfo1 = new IdsBulkInfo("ids-job-xyz", "mrrobot", "processing-finished", 2, 2, false);
		IdsBulkInfo idsBulkInfo2 = new IdsBulkInfo("ids-job-xyzz", "mrrobot", "processing-finished", 5, 5, true);
        idsBulkInfo1.setJobid(newKey1);
        idsBulkInfo2.setJobid(newKey2);
        idsBulkInfo1.setStartdate(now);
        idsBulkInfo2.setStartdate(now);
        List<IdsBulkInfo> idsBulkInfos = new ArrayList<IdsBulkInfo>();
    	idsBulkInfos.add(idsBulkInfo1);
    	idsBulkInfos.add(idsBulkInfo2);
        
        when(bulkStatusService.getIdsJobs("mrrobot", "processing-finished")).thenReturn(idsBulkInfos);

		mockMvc.perform(MockMvcRequestBuilders.get("/ids/jobs?status=processing-finished&userid=mrrobot")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andDo(MockMvcResultHandlers.print())
				.andExpect(jsonPath("$.jobs").isArray()).andExpect(jsonPath("$.jobs", hasSize(2)))
				.andExpect(jsonPath("$.jobs[0].jobid", Is.is(99)))
				.andExpect(jsonPath("$.jobs[0].idsjobid", Is.is("ids-job-xyz")))
				.andExpect(jsonPath("$.jobs[0].userid", Is.is("mrrobot")))
				.andExpect(jsonPath("$.jobs[0].status", Is.is("processing-finished")))
				.andExpect(jsonPath("$.jobs[0].totalrecs", Is.is(2)))
				.andExpect(jsonPath("$.jobs[0].recssofar", Is.is(2)))
				.andExpect(jsonPath("$.jobs[0].startdate", Is.is(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(jsonPath("$.jobs[0].test", Is.is(false)))
				.andExpect(jsonPath("$.jobs[1].jobid", Is.is(88)))
				.andExpect(jsonPath("$.jobs[1].idsjobid", Is.is("ids-job-xyzz")))
				.andExpect(jsonPath("$.jobs[1].userid", Is.is("mrrobot")))
				.andExpect(jsonPath("$.jobs[1].status", Is.is("processing-finished")))
				.andExpect(jsonPath("$.jobs[1].totalrecs", Is.is(5)))
				.andExpect(jsonPath("$.jobs[1].recssofar", Is.is(5)))
				.andExpect(jsonPath("$.jobs[1].startdate", Is.is(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(jsonPath("$.jobs[1].test", Is.is(true)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("addIdsJobIds")
	public void testGetIdsBulkRequestProgressInProgress(@PathVariable(required = true, name = "idsjobid") String idsjobid)
			throws Exception {

		IdsBulkInfo idsBulkInfo = new IdsBulkInfo(idsjobid, "bob", "in-progress", 107, 45, false);
        idsBulkInfo.setJobid(22);
        idsBulkInfo.setStartdate(now);
		List<IdsBulkInfo> idsBulkInfos = Arrays.asList(idsBulkInfo);

        when(bulkStatusRepository.getIdsJob(Mockito.any(String.class))).thenReturn(idsBulkInfos);

		mockMvc.perform(MockMvcRequestBuilders.get("/ids/bulk-progress/" + idsjobid)).andExpect(status().isOk())
				.andExpect(jsonPath("$.jobid", Is.is(22)))
				.andExpect(jsonPath("$.idsjobid", Is.is("ids-job-xyz")))
				.andExpect(jsonPath("$.userid", Is.is("bob")))
				.andExpect(jsonPath("$.status", Is.is("in-progress")))
				.andExpect(jsonPath("$.totalrecs", Is.is(107)))
				.andExpect(jsonPath("$.recssofar", Is.is(45)))
				.andExpect(jsonPath("$.startdate", Is.is(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(jsonPath("$.test", Is.is(false)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@ParameterizedTest
	@MethodSource("addIdsJobIds")
	public void testGetIdsBulkRequestProgressFinished(@PathVariable(required = true, name = "idsjobid") String idsjobid)
			throws Exception {

		IdsBulkInfo idsBulkInfo = new IdsBulkInfo(idsjobid, "bob", "finished", 107, 107, true);
        idsBulkInfo.setJobid(77);
        idsBulkInfo.setStartdate(now);
        idsBulkInfo.setEnddate(now.plusHours(2));
		List<IdsBulkInfo> bulkInfos = Arrays.asList(idsBulkInfo);

        when(bulkStatusRepository.getIdsJob(Mockito.any(String.class))).thenReturn(bulkInfos);

		mockMvc.perform(MockMvcRequestBuilders.get("/ids/bulk-progress/" + idsjobid)).andExpect(status().isOk())
				.andExpect(jsonPath("$.jobid", Is.is(77)))
				.andExpect(jsonPath("$.idsjobid", Is.is("ids-job-xyz")))
				.andExpect(jsonPath("$.userid", Is.is("bob")))
				.andExpect(jsonPath("$.status", Is.is("finished")))
				.andExpect(jsonPath("$.totalrecs", Is.is(107)))
				.andExpect(jsonPath("$.recssofar", Is.is(107)))
				.andExpect(jsonPath("$.startdate", Is.is(now.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(jsonPath("$.enddate", Is.is(now.plusHours(2).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))))
				.andExpect(jsonPath("$.test", Is.is(true)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void idsBulkProgressNoIdsJobId() throws Exception {
		
		mockMvc.perform(MockMvcRequestBuilders.get("/ids/bulk-progress/ ")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("idsjobid: idsjobid is mandatory")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem(containsString("idsjobid: idsjobid is mandatory"))))		
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}

	@Test
	public void testSwagger() throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.get("/api-docs")
						.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.openapi", Is.is("3.0.1")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void resultGetRequestMissingJobId() throws Exception {
		
		String jobIdMissing = "Required request parameter 'jobid' for method parameter type String is not present";
		
		mockMvc.perform(MockMvcRequestBuilders.get("/results")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", Is.is(jobIdMissing)))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem("jobid parameter is missing")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
	public void resultGetRequestInvalidJobId() throws Exception {
		
		String jobIdError = "getResults.jobId: jobid is mandatory and must be an integer";
		
		mockMvc.perform(MockMvcRequestBuilders.get("/results?jobid=xyz")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status", Is.is("BAD_REQUEST")))
				.andExpect(jsonPath("$.message", containsString("jobId: jobid is mandatory and must be an integer")))
				.andExpect(jsonPath("$.errors").isArray()).andExpect(jsonPath("$.errors", hasSize(1)))
				.andExpect(jsonPath("$.errors", hasItem("uk.gov.ons.bulk.controllers.BulkAddressController " + jobIdError)))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
	@Test
    public void runResultRequest() throws Exception {
    	
    	String filename = "results_1.csv.gz";
		BulkInfo bulkInfo = new BulkInfo("mrrobot", "results-exported", 2, 2);
        bulkInfo.setStartdate(now);
        List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	bulkInfos.add(bulkInfo);
    	Path expectedPath = Paths.get("src/test/resources/results_1.csv.gz");
        
        when(downloadService.getResultFile("1", filename)).thenReturn(new FileInputStream(expectedPath.toFile()));
        when(bulkStatusService.queryJob(1L)).thenReturn(bulkInfos);

		MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/results?jobid=1")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(request().asyncStarted())
				.andReturn();
			
		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isOk())
				.andExpect(content().bytes(Files.readAllBytes(expectedPath)))
				.andExpect(header().string("Content-Disposition", "attachment; filename=\"results_1.csv.gz\""))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_OCTET_STREAM_VALUE));
    }
	
	@Test
    public void runResultRequestThrowIOException() throws Exception {
    	
    	String filename = String.format("results_%s.csv.gz", 1);
		BulkInfo bulkInfo = new BulkInfo("mrrobot", "results-exported", 2, 2);
        bulkInfo.setStartdate(now);
        List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	bulkInfos.add(bulkInfo);
        
        when(downloadService.getResultFile("1", filename)).thenThrow(new IOException("An IO Exception"));
        when(bulkStatusService.queryJob(1L)).thenReturn(bulkInfos);	
        
		MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/results?jobid=1")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(request().asyncStarted())
				.andReturn();
		
		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.error", containsString("An IO Exception")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
	@Test
    public void runResultRequestNonExistent() throws Exception {
    	
    	List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
        when(bulkStatusService.queryJob(99L)).thenReturn(bulkInfos);
        
		MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/results?jobid=99&userid=mrrobot")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(request().asyncStarted())
				.andReturn();
		
		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error", Is.is(String.format("Job ID %s not found on the system", 99L))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
	@Test
    public void runResultRequestNotDownloadable() throws Exception {

		BulkInfo bulkInfo = new BulkInfo("mrrobot", "processing-finished", 2, 2);
        bulkInfo.setStartdate(now);
        List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	bulkInfos.add(bulkInfo);
        
        when(bulkStatusService.queryJob(1L)).thenReturn(bulkInfos);
        
		MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/results?jobid=1&userid=mrrobot")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(request().asyncStarted())
				.andReturn();
		
		mockMvc.perform(asyncDispatch(mvcResult))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error", Is.is(String.format("Job ID %s is not currently downloadable", 1L))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
	}
	
    @ParameterizedTest
    @MethodSource("bulkRequestObject")
    public void runBulkRequestNoCapacity(@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {
    	
    	List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	for (long i = 1; i < 11; i++) {
    		BulkInfo bulkInfo = new BulkInfo("mrrobot", "in-progress", 100000, 10000);
    		bulkInfo.setJobid(i);
    		bulkInfos.add(bulkInfo);
    	}
	    
	    when(bulkStatusService.getJobs("", "in-progress")).thenReturn(bulkInfos);       
		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isServiceUnavailable())
		 		.andExpect(jsonPath("$.error", Is.is("/bulk error: Too many jobs. The service is at capacity. Please wait before sending your request. Check the Splunk dashboard.")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
    
    @ParameterizedTest
    @MethodSource("bulkRequestObject")
    public void runBulkRequestMaxJobsButCapacity(@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {
    	
    	List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	for (long i = 1; i < 11; i++) {
    		BulkInfo bulkInfo = new BulkInfo("mrrobot", "in-progress", 4, 2);
    		bulkInfo.setJobid(i);
    		bulkInfos.add(bulkInfo);
    	}
   	
    	long newKey = 102;
        
		BulkInfo bulkInfo = new BulkInfo("bigqueryboy", "in-progress", 2, 0);
        bulkInfo.setJobid(newKey);
        bulkInfo.setStartdate(now);
        
        BulkRequest testBulkRequest1 = new BulkRequest();
        testBulkRequest1.setId("1");
        testBulkRequest1.setAddress("4 Gate Reach Exeter EX2 6GA");
        BulkRequest testBulkRequest2 = new BulkRequest();
        testBulkRequest2.setId("2");
        testBulkRequest2.setAddress("Costa Coffee, 12 Bedford Street, Exeter");
        BulkRequest[] bulkRequests = {testBulkRequest1, testBulkRequest2};
        
	    String userName = "bigqueryboy";
    	    
	    when(bulkStatusService.getJobs("", "in-progress")).thenReturn(bulkInfos);    
	    when(bulkStatusRepository.saveJob(Mockito.any(BulkInfo.class))).thenReturn(102L);
        doNothing().when(cloudTaskService).createTasks(newKey, bulkRequests, 2L, null, userName);
		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.jobId", Is.is("102")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
    @ParameterizedTest
    @MethodSource("bulkRequestObject")
    public void runBulkRequestMaxJobsNoCapacity(@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {
    	
    	List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	for (long i = 1; i < 10; i++) {
    		BulkInfo bulkInfo = new BulkInfo("mrrobot", "in-progress", 5, 2);
    		bulkInfo.setJobid(i);
    		bulkInfos.add(bulkInfo);
    	}
    	
		BulkInfo bulkInfo = new BulkInfo("mrrobot", "in-progress", 4, 1);
		bulkInfo.setJobid(10);
		bulkInfos.add(bulkInfo);
    	    
	    when(bulkStatusService.getJobs("", "in-progress")).thenReturn(bulkInfos);
		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isServiceUnavailable())
		 		.andExpect(jsonPath("$.error", Is.is("/bulk error: Too many jobs. The service is at capacity. Please wait before sending your request. Check the Splunk dashboard.")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
    @ParameterizedTest
    @MethodSource("bulkRequestObject")
    public void runBulkRequestMaxJobsButCapacityEdgeCase(@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {
    	
    	List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	for (long i = 1; i < 10; i++) {
    		BulkInfo bulkInfo = new BulkInfo("mrrobot", "in-progress", 5, 2);
    		bulkInfo.setJobid(i);
    		bulkInfos.add(bulkInfo);
    	}
    	
		BulkInfo bulkInfo = new BulkInfo("mrrobot", "in-progress", 3, 1);
		bulkInfo.setJobid(10);
		bulkInfos.add(bulkInfo);
   	
    	long newKey = 102;
        
        BulkRequest testBulkRequest1 = new BulkRequest();
        testBulkRequest1.setId("1");
        testBulkRequest1.setAddress("4 Gate Reach Exeter EX2 6GA");
        BulkRequest testBulkRequest2 = new BulkRequest();
        testBulkRequest2.setId("2");
        testBulkRequest2.setAddress("Costa Coffee, 12 Bedford Street, Exeter");
        BulkRequest[] bulkRequests = {testBulkRequest1, testBulkRequest2};
        
	    String userName = "bigqueryboy";
    	    
	    when(bulkStatusService.getJobs("", "in-progress")).thenReturn(bulkInfos);    
	    when(bulkStatusRepository.saveJob(Mockito.any(BulkInfo.class))).thenReturn(102L);
        doNothing().when(cloudTaskService).createTasks(newKey, bulkRequests, 2L, null, userName);
		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isOk())
				.andExpect(jsonPath("$.jobId", Is.is("102")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
    @ParameterizedTest
    @MethodSource("bulkRequestObject")
    public void runBulkRequestAvailableJobsNoCapacity(@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {
    	
    	List<BulkInfo> bulkInfos = new ArrayList<BulkInfo>();
    	for (long i = 1; i < 5; i++) {
    		BulkInfo bulkInfo = new BulkInfo("mrrobot", "in-progress", 250000, 10000);
    		bulkInfo.setJobid(i);
    		bulkInfos.add(bulkInfo);
    	}
    	    
	    when(bulkStatusService.getJobs("", "in-progress")).thenReturn(bulkInfos);
		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isServiceUnavailable())
		
				.andDo(MockMvcResultHandlers.print()) 		
				.andExpect(jsonPath("$.error", Is.is("/bulk error: Too many jobs. The service is at capacity. Please wait before sending your request. Check the Splunk dashboard.")))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
    @ParameterizedTest
    @MethodSource("bulkRequestObjectUnderMin")
    public void runBulkRequestJobRecordsUnderMin(@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())		
				.andExpect(jsonPath("$.error", Is.is(String.format("/bulk error: Bulk match records under minimum. The current minimum records per job is: %d. Condider using the UI for bulks this small.", minRecordsPerJob))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
    
    @ParameterizedTest
    @MethodSource("bulkRequestObjectOverMax")
    public void runBulkRequestJobRecordsOverMax(@RequestBody BulkRequestContainer bulkRequestContainer) throws Exception {

		mockMvc.perform(MockMvcRequestBuilders.post("/bulk")
				.content(new ObjectMapper().writeValueAsString(bulkRequestContainer))
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest())		
				.andExpect(jsonPath("$.error", Is.is(String.format("/bulk error: Bulk match records over maximum. The current maximum records per job is: %d.", maxRecordsPerJob))))
				.andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }
}
