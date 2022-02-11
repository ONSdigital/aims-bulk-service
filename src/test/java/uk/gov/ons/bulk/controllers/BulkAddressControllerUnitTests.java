package uk.gov.ons.bulk.controllers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
//import org.junit.Test;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.BeforeAll;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import uk.gov.ons.bulk.util.Toolbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
//import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

//import com.in28minutes.springboot.model.Course;
//import com.in28minutes.springboot.service.StudentService;

@RunWith(SpringRunner.class)
//@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@WebMvcTest(value = BulkAddressController.class)
@AutoConfigureMockMvc
//@EnableWebMvc
//@Configuration
//@WithMockUser
//@TestExecutionListeners()
public class BulkAddressControllerUnitTests {

    @Autowired
    BigQuery bigquery;

    @Value("${spring.cloud.gcp.project-id}")
    private String projectId;

    @Value("${spring.cloud.gcp.bigquery.dataset-name}")
    private String datasetName;

    @Value("${aims.bigquery.info-table}")
    private String infoTable;

    @Autowired(required = false)
    private MockMvc mockMvc;

    @Mock
    private Toolbox utils;

    Properties queryReponse = new Properties();

    String queryReponseRef = "query.properties";


    //@BeforeAll
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



//    @MockBean
//    private StudentService studentService;
//
//    Course mockCourse = new Course("Course1", "Spring", "10Steps",
//            Arrays.asList("Learn Maven", "Import Project", "First Example",
//                    "Second Example"));
//
//    String exampleCourseJson = "{\"name\":\"Spring\",\"description\":\"10Steps\",\"steps\":[\"Learn Maven\",\"Import Project\",\"First Example\",\"Second Example\"]}";

    @Test
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

        this.setUp();

//        Mockito.when(
//                studentService.retrieveCourse(Mockito.anyString(),
//                        Mockito.anyString())).thenReturn(mockCourse);

        RequestBuilder requestBuilder = MockMvcRequestBuilders.get(
                "/jobs").accept(
                MediaType.APPLICATION_JSON);

        MvcResult result = mockMvc.perform(requestBuilder).andReturn();

        System.out.println(result.getResponse());
        String expected = "{id:Course1,name:Spring,description:10Steps}";

        assertEquals(expected,"hello");

        // {"id":"Course1","name":"Spring","description":"10 Steps, 25 Examples and 10K Students","steps":["Learn Maven","Import Project","First Example","Second Example"]}

//        JSONAssert.assertEquals(expected, result.getResponse()
//                .getContentAsString(), false);
    }

//    @Test
//    public void createStudentCourse() throws Exception {
//        Course mockCourse = new Course("1", "Smallest Number", "1",
//                Arrays.asList("1", "2", "3", "4"));
//
//        // studentService.addCourse to respond back with mockCourse
//        Mockito.when(
//                studentService.addCourse(Mockito.anyString(),
//                        Mockito.any(Course.class))).thenReturn(mockCourse);
//
//        // Send course as body to /students/Student1/courses
//        RequestBuilder requestBuilder = MockMvcRequestBuilders
//                .post("/students/Student1/courses")
//                .accept(MediaType.APPLICATION_JSON).content(exampleCourseJson)
//                .contentType(MediaType.APPLICATION_JSON);
//
//        MvcResult result = mockMvc.perform(requestBuilder).andReturn();
//
//        MockHttpServletResponse response = result.getResponse();
//
//        assertEquals(HttpStatus.CREATED.value(), response.getStatus());
//
//        assertEquals("http://localhost/students/Student1/courses/1",
//                response.getHeader(HttpHeaders.LOCATION));
//
//    }

}


