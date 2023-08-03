package uk.gov.ons.bulk.entities;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.validation.ConstraintValidator;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import uk.gov.ons.bulk.CustomLocalValidatorFactoryBean;
import uk.gov.ons.bulk.validator.EpochValidator;

@SpringBootTest
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
@DirtiesContext
public class PayloadValidationTests {
	
	private static Validator validator;
	private static EpochValidator epochValidator;
	private static ObjectMapper objectMapper;
	private static ValidatorFactory factory;	
	
    @Value("${aims.epochs}")
    private String epochs;
	
	@BeforeAll
    public void setup() {
		epochValidator = new EpochValidator();
		List<ConstraintValidator<?,?>> customConstraintValidators =
	            Collections.singletonList(epochValidator);
		factory = 
	            new CustomLocalValidatorFactoryBean(customConstraintValidators);
		validator = factory.getValidator();
		ReflectionTestUtils.setField(epochValidator, "epochs", epochs);
    	objectMapper = new ObjectMapper();
    }
	
	@AfterAll
	public void tearDown() {
		factory.close();
	}
	
    @Test
    public void testPayloadValidatorHappy() throws Exception {

    	NewIdsJobMessage msg = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-happy.json"),
        		NewIdsJobMessage.class);

        List<String> validationErrorMessages = new ArrayList<>();
		
		validator.validate(msg).forEach(violation -> {
			validationErrorMessages.add(violation.getMessage());
		});

		assertTrue(validationErrorMessages.isEmpty());
    }
	
    @Test
    public void testPayloadValidatorWrongEpoch() throws Exception {

        NewIdsJobMessage msg = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-epoch.json"),
        		NewIdsJobMessage.class);

        List<String> validationErrorMessages = new ArrayList<>();
		
		validator.validate(msg).forEach(violation -> {
			validationErrorMessages.add(violation.getMessage());
		});

        String expectedMsg =  "epoch_number must be one of 101, 99, 97, 95";
        assertEquals(1, validationErrorMessages.size());
        assertTrue(validationErrorMessages.contains(expectedMsg));
    }

    @Test
    public void testPayloadValidatorWrongLimit() throws Exception {

        NewIdsJobMessage msg = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-limit.json"),
        		NewIdsJobMessage.class);
		
		List<String> validationErrorMessages = new ArrayList<>();
		
		validator.validate(msg).forEach(violation -> {
			validationErrorMessages.add(violation.getMessage());
		});

        String expectedMsg =  "address_limit should be an integer between 1 and 100 (1 is default)";
        assertEquals(1, validationErrorMessages.size());
        assertTrue(validationErrorMessages.contains(expectedMsg));
    }
    
    @Test
    public void testPayloadValidatorWrongLimitLow() throws Exception {

    	NewIdsJobMessage msg = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-limit-low.json"),
    			NewIdsJobMessage.class);

    	List<String> validationErrorMessages = new ArrayList<>();
		
		validator.validate(msg).forEach(violation -> {
			validationErrorMessages.add(violation.getMessage());
		});

        String expectedMsg =  "address_limit should be an integer between 1 and 100 (1 is default)";
        assertEquals(1, validationErrorMessages.size());
        assertTrue(validationErrorMessages.contains(expectedMsg));
    }

    @Test
    public void testPayloadValidatorWrongThreshold() throws Exception {

    	NewIdsJobMessage msg = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-threshold.json"),
    			NewIdsJobMessage.class);

		List<String> validationErrorMessages = new ArrayList<>();
		
		validator.validate(msg).forEach(violation -> {
			validationErrorMessages.add(violation.getMessage());
		});

        String expectedMsg =  "quality_match_threshold should be decimal number between 0 and 100 (10 is default)";
        assertEquals(1, validationErrorMessages.size());
        assertTrue(validationErrorMessages.contains(expectedMsg));
    }
    
    @Test
    public void testPayloadValidatorWrongThresholdHigh() throws Exception {

    	NewIdsJobMessage msg = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-threshold-high.json"),
    			NewIdsJobMessage.class);

		List<String> validationErrorMessages = new ArrayList<>();
		
		validator.validate(msg).forEach(violation -> {
			validationErrorMessages.add(violation.getMessage());
		});

        String expectedMsg =  "quality_match_threshold should be decimal number between 0 and 100 (10 is default)";
        assertEquals(1, validationErrorMessages.size());
        assertTrue(validationErrorMessages.contains(expectedMsg));
    }

    @Test
    public void testPayloadValidatorWrongHistorical() throws Exception {

    	NewIdsJobMessage msg = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-historical.json"),
    			NewIdsJobMessage.class);

		List<String> validationErrorMessages = new ArrayList<>();
		
		validator.validate(msg).forEach(violation -> {
			validationErrorMessages.add(violation.getMessage());
		});

        String expectedMsg = "historical_flag must be true or false";
        assertEquals(1, validationErrorMessages.size());
        assertTrue(validationErrorMessages.contains(expectedMsg));
    }
	
    @Test
    public void testPayloadValidatorDefaults() throws Exception {

        NewIdsJobMessage msg = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-defaults.json"),
        		NewIdsJobMessage.class);
        
		List<String> validationErrorMessages = new ArrayList<>();
		
		validator.validate(msg).forEach(violation -> {
			validationErrorMessages.add(violation.getMessage());
		});

		assertTrue(validationErrorMessages.isEmpty());
        assertEquals("1", msg.getPayload().getAddressLimit());
        assertEquals("10", msg.getPayload().getQualityMatchThreshold());
        assertEquals("99", msg.getPayload().getEpoch());
        assertEquals("true", msg.getPayload().getHistorical());
        assertEquals(false, msg.isTest());
    }
    
	@Test
    public void testPayloadValidatorMissingJobId() throws Exception {
		
        NewIdsJobMessage msg = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-missing-job-id.json"),
        		NewIdsJobMessage.class);

        List<String> validationErrorMessages = new ArrayList<>();
        
        validator.validate(msg).forEach(violation -> {
			validationErrorMessages.add(violation.getMessage());
		});

        String expectedMsg =  "ids_job_id must be supplied";
        assertEquals(1, validationErrorMessages.size());
        assertTrue(validationErrorMessages.contains(expectedMsg));
    }
    
    @Test
    public void testPayloadValidatorMsgTest() throws Exception {

        NewIdsJobMessage msg = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-test.json"),
        		NewIdsJobMessage.class);
        
		List<String> validationErrorMessages = new ArrayList<>();
		
		validator.validate(msg).forEach(violation -> {
			validationErrorMessages.add(violation.getMessage());
		});

		assertTrue(validationErrorMessages.isEmpty());
        assertEquals(true, msg.isTest());
    }
    
    @Test
    public void testPayloadValidatorMissingPayloadTest() throws Exception {

        NewIdsJobMessage msg = objectMapper.readValue("{}",	NewIdsJobMessage.class);
        
		List<String> validationErrorMessages = new ArrayList<>();
		
		validator.validate(msg).forEach(violation -> {
			validationErrorMessages.add(violation.getMessage());
		});
		
		String expectedMsg = "payload cannot be empty";

		assertEquals(1, validationErrorMessages.size());
        assertTrue(validationErrorMessages.contains(expectedMsg));
    }
}