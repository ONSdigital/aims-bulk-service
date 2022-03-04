package uk.gov.ons.bulk.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.cloud.bigquery.*;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import uk.gov.ons.bulk.util.QueryFuncs;
import uk.gov.ons.bulk.util.Toolbox;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

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

    Properties queryReponse = new Properties();
    String queryReponseRef = "query.properties";

    private String BASE_DATASET_QUERY;
    private String INFO_TABLE_QUERY;
    private String JOBS_QUERY;
    private String JOB_QUERY;
    private String RESULT_QUERY;
    private String MAX_QUERY;

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

    public String getOK() {return "OK";}

    @Test
    public void testGetBulkRequestProgress() throws Exception {

  // use mockstatic to make it use cached queries
        try (MockedStatic<QueryFuncs> theMock = Mockito.mockStatic(QueryFuncs.class)) {

            theMock.when(() -> QueryFuncs.runQuery(JOBS_QUERY,bigquery))
                    .thenReturn(getResponse(JOBS_QUERY));

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(
                "/jobs").accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        String expected = "jobstable";
        
        assertTrue(result.getResponse().getContentAsString().contains(expected));
        }
    }


    @ParameterizedTest
    @MethodSource("addJobIds")
    public void testGetBulkRequestProgress(@PathVariable(required = true, name = "jobid") String jobid) throws Exception {

        // use mockstatic to make it use cached queries
        try (MockedStatic<QueryFuncs> theMock = Mockito.mockStatic(QueryFuncs.class)) {

            theMock.when(() -> QueryFuncs.runQuery(String.format(JOB_QUERY, jobid),bigquery))
                    .thenReturn(getResponse(String.format(JOB_QUERY, jobid)));

            RequestBuilder requestBuilder = MockMvcRequestBuilders.get(
                    "/bulk-progress/"+jobid).accept(
                    MediaType.APPLICATION_JSON);

            MvcResult result = mockMvc.perform(requestBuilder).andReturn();

            String expected = "jobstable";
            assertTrue(result.getResponse().getContentAsString().contains(expected));
        }
    }

    private static Stream<Arguments> addJobIds() {
        return Stream.of(
                Arguments.of("14"));
    }

    @ParameterizedTest
    @MethodSource("addJobIds")
    public void getBulkResults(@PathVariable(required = true, name = "jobid") String jobid) throws Exception {
        try (MockedStatic<QueryFuncs> theMock = Mockito.mockStatic(QueryFuncs.class)) {

            theMock.when(() -> QueryFuncs.runQuery(String.format(RESULT_QUERY, jobid),bigquery))
                    .thenReturn(getResponse(String.format(RESULT_QUERY, jobid)));

            RequestBuilder requestBuilder = MockMvcRequestBuilders.get(
                    "/bulk-result/"+jobid).accept(
                    MediaType.APPLICATION_JSON);

            MvcResult result = mockMvc.perform(requestBuilder).andReturn();

            String expected = "CR07";
            assertTrue(result.getResponse().getContentAsString().contains(expected));
        }
    }

    private static Stream<Arguments> addJson() {
        return Stream.of(
                Arguments.of("{\n" +
                        "    \"addresses\":[{\n" +
                        "        \"id\" : \"1\",\n" +
                        "        \"address\": \"4 Gate Reach Exeter EX2 6GA\"\n" +
                        "    },{\n" +
                        "        \"id\" : \"2\",\n" +
                        "        \"address\": \"Costa Coffee, 12 Bedford Street, Exeter\"\n" +
                        "    }]\n" +
                        "}"));
    }

    @ParameterizedTest
    @MethodSource("addJson")
    public void runBulkRequest(@RequestBody String addressesJson) throws Exception {
        try (MockedStatic<QueryFuncs> theMock = Mockito.mockStatic(QueryFuncs.class)) {

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

// Will be service call
//            theMock.when(() -> BulkAddressController.createTask(createTaskFunction,
//                    anyString(),anyString(),anyString()))
//                    .thenAnswer((Answer<Void>) invocation -> null);

            RequestBuilder requestBuilder = MockMvcRequestBuilders.post(
                    "/bulk").accept(
                    MediaType.APPLICATION_JSON).content(addressesJson);

            MvcResult result = mockMvc.perform(requestBuilder).andReturn();

            String expected = "Submitted";
            assertTrue(result.getResponse().getContentAsString().contains(expected));
        }
    }

    @Test
    public void testHomePage() throws Exception {
        this.mockMvc.perform(get("/")).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(containsString("OK")));
    }


}
