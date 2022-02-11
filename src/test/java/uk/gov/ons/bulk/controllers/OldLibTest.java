package uk.gov.ons.bulk.controllers;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import org.junit.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.*;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.client.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.ons.bulk.util.Toolbox;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@RunWith(MockitoJUnitRunner.class)
@WebMvcTest(value = BulkAddressController.class)
public class OldLibTest {

    @Autowired
    BigQuery bigquery;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.bigquery.dataset-name}")
    private String datasetName;

    @Value("${aims.bigquery.info-table}")
    private String infoTable;


    private MockMvc mockMvc;

    @Mock
    private Toolbox utils;

    Properties queryReponse = new Properties();

    String queryReponseRef = "query.properties";

    @Before
    public void setUp() throws Exception {

    //    mockMvc = MockMvcBuilders.standaloneSetup(BulkAddressController.class).build();

        InputStream is = getClass().getClassLoader().getResourceAsStream(queryReponseRef);

        if (is != null) {
            queryReponse.load(is);
        } else {
            throw new FileNotFoundException("Query Property file not in classpath");
        }


        MockitoAnnotations.initMocks(this);


        when(utils.runQuery(anyString(), eq(bigquery))).thenAnswer(new Answer<ArrayList<FieldValueList>>() {
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

        if (query != null)
            return fields;
        return null;

    }


    @Test
    public void testGetBulkRequestProgress() throws Exception {

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(
                "/jobs").accept(
                MediaType.APPLICATION_JSON);

  //      MvcResult result = this.mockMvc.perform(get("/job")).andExpect(status().isOk()).andReturn();

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        System.out.println(result.getResponse());
        String expected = "{id:Course1,name:Spring,description:10Steps}";

        assertEquals(expected, "hello");

        // {"id":"Course1","name":"Spring","description":"10 Steps, 25 Examples and 10K Students","steps":["Learn Maven","Import Project","First Example","Second Example"]}

//        JSONAssert.assertEquals(expected, result.getResponse()
//                .getContentAsString(), false);
    }

}
