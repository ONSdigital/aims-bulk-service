package uk.gov.ons.bulk.controllers;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.TestInstance;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.ons.bulk.util.QueryFuncs;
import uk.gov.ons.bulk.util.Toolbox;

import javax.annotation.PostConstruct;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@AutoConfigureMockMvc
public class BulkAddressApplicationTest {

    @Autowired
    BigQuery bigquery;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.bigquery.dataset-name}")
    private String datasetName;

    @Value("${aims.bigquery.info-table}")
    private String infoTable;

    @Autowired
    private MockMvc mockMvc;

//    @Mock
//    private QueryFuncs utils = QueryFuncs.getInstance();

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


        InputStream is = getClass().getClassLoader().getResourceAsStream(queryReponseRef);

        if (is != null) {
            queryReponse.load(is);
        } else {
            throw new FileNotFoundException("Query Property file not in classpath");
        }

//        try (MockedStatic<QueryFuncs> theMock = Mockito.mockStatic(QueryFuncs.class)) {
//
//            theMock.when(() -> QueryFuncs.runQuery(JOBS_QUERY,bigquery))
//            .thenReturn(getResponse(JOBS_QUERY));
//         //   assertThat(Buddy.name()).isEqualTo("Rafael");
//        }


        MockitoAnnotations.initMocks(this);


      //  when(utils.runQuery(anyString(),any(BigQuery.class))).thenAnswer(new Answer<ArrayList<FieldValueList>>() {
//            when(QueryFuncs.runQuery(any(),any())).thenAnswer(new Answer<ArrayList<FieldValueList>>() {
//            public ArrayList<FieldValueList> answer(InvocationOnMock invocation) throws Throwable {
//
//                Object[] args = invocation.getArguments();
//
//                return getResponse((String) args[0]);
//            }
//        });


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

    @Test
    public void testGetBulkRequestProgress() throws Exception {

        try (MockedStatic<QueryFuncs> theMock = Mockito.mockStatic(QueryFuncs.class)) {

            theMock.when(() -> QueryFuncs.runQuery(JOBS_QUERY,bigquery))
                    .thenReturn(getResponse(JOBS_QUERY));
            //   assertThat(Buddy.name()).isEqualTo("Rafael");


        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(
                "/jobs").accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        System.out.println(result.getResponse());
        String expected = "hello";

        assertEquals(expected,"hello");
        }
    }


    @Test
    public void shouldReturnDefaultMessage() throws Exception {
        this.mockMvc.perform(get("/")).andDo(print()).andExpect(status().isOk())
                .andExpect(content().string(containsString("bulk")));
    }
}
