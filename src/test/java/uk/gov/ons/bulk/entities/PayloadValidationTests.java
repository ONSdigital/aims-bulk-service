package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.context.MessageSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import uk.gov.ons.bulk.CustomLocalValidatorFactoryBean;
import uk.gov.ons.bulk.service.BulkStatusService;
import uk.gov.ons.bulk.service.IdsService;
import uk.gov.ons.bulk.validator.EpochValidator;

@SpringBootTest()
@ExtendWith(SpringExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ImportAutoConfiguration(MessageSourceAutoConfiguration.class)
@ActiveProfiles("test")
public class PayloadValidationTests {

    @Value("${aims.current-epoch}")
    private String currentEpoch;

    @Value("${aims.epochs}")
    private String epochs;

    private EpochValidator epochValidator = new EpochValidator();

    private final List<ConstraintValidator<?,?>> customConstraintValidators =
            Collections.singletonList(epochValidator);
    private final ValidatorFactory customValidatorFactory =
            new CustomLocalValidatorFactoryBean(customConstraintValidators);
    private final Validator validator = customValidatorFactory.getValidator();

    @Test
    public void testPayloadValidatorHappy() throws Exception {

        epochValidator.setEpochs(epochs);
        ObjectMapper objectMapper = new ObjectMapper();
        NewIdsJobPayload testPayload = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-happy.json"),
                NewIdsJobPayload.class);

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

        epochValidator.setEpochs(epochs);
        ObjectMapper objectMapper = new ObjectMapper();
        NewIdsJobPayload testPayload = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-sad.json"),
                NewIdsJobPayload.class);

        Set<ConstraintViolation<NewIdsJobPayload>> violations = validator.validate(testPayload);
        StringBuilder validationErrorMessages = new StringBuilder("");
        for (ConstraintViolation<NewIdsJobPayload> violation : violations) {
            System.out.println(violation.getMessage());
            validationErrorMessages.append(violation.getMessage());
            validationErrorMessages.append("\n");
        }

        String expectedMsg = "historical must be true or false\n{epoch.val.message}\n";
        String actualMessage = validationErrorMessages.toString();

        assertEquals(expectedMsg, actualMessage);
    }

    @Test
    public void testPayloadValidatorDefaults() throws Exception {

        epochValidator.setEpochs(epochs);
        ObjectMapper objectMapper = new ObjectMapper();
        NewIdsJobPayload testPayload = objectMapper.readValue(new File("src/test/resources/message-new-ids-payload-defaults.json"),
                NewIdsJobPayload.class);

        testPayload.setEpoch(currentEpoch);

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
