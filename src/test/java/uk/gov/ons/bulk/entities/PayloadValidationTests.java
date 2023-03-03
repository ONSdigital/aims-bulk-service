package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.File;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

import uk.gov.ons.bulk.validator.Epoch;

@SpringBootTest()
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ImportAutoConfiguration(MessageSourceAutoConfiguration.class)
@ActiveProfiles("test")
public class PayloadValidationTests {

    @Value("${aims.current-epoch}")
    private String currentEpoch;

    @Test
    public void testPayloadValidatorHappy() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        NewIdsJobPayload testPayload = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-happy.json"),
                NewIdsJobPayload.class);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<NewIdsJobPayload>> violations = validator.validate(testPayload);
        StringBuilder validationErrorMessages = new StringBuilder("");
        for (ConstraintViolation<NewIdsJobPayload> violation : violations) {
            System.out.println(violation.getMessage());
            validationErrorMessages.append(violation.getMessage());
            validationErrorMessages.append("\n");
        }

        String expectedMsg = "";
        String actualMessage = validationErrorMessages.toString();

         assertEquals(expectedMsg, actualMessage);
    }

    @Test
    public void testPayloadValidatorSad() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        NewIdsJobPayload testPayload = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-sad.json"),
                NewIdsJobPayload.class);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<NewIdsJobPayload>> violations = validator.validate(testPayload);
        StringBuilder validationErrorMessages = new StringBuilder("");
        for (ConstraintViolation<NewIdsJobPayload> violation : violations) {
            System.out.println(violation.getMessage());
            validationErrorMessages.append(violation.getMessage());
            validationErrorMessages.append("\n");
        }

        String expectedMsg = "historical must be true or false\n";
        String actualMessage = validationErrorMessages.toString();

        assertEquals(expectedMsg, actualMessage);
    }

    @Test
    public void testPayloadValidatorDefaults() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        NewIdsJobPayload testPayload = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-defaults.json"),
                NewIdsJobPayload.class);

        System.out.println("epoch = " + currentEpoch);

        testPayload.setEpoch(currentEpoch);

        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        Validator validator = factory.getValidator();
        Set<ConstraintViolation<NewIdsJobPayload>> violations = validator.validate(testPayload);
        StringBuilder validationErrorMessages = new StringBuilder("");
        for (ConstraintViolation<NewIdsJobPayload> violation : violations) {
            System.out.println(violation.getMessage());
            validationErrorMessages.append(violation.getMessage());
            validationErrorMessages.append("\n");
        }

        System.out.println("addressLimit = " + testPayload.getAddressLimit() +
                " matchThreshold = " + testPayload.getQualityMatchThreshold() +
                " epoch = " + testPayload.getEpoch() +
                " historical = " + testPayload.getHistorical());

        String expectedMsg = "";
        String actualMessage = validationErrorMessages.toString();

        assertEquals(expectedMsg, actualMessage);
    }


}
