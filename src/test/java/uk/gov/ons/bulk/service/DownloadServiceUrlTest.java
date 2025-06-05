package uk.gov.ons.bulk.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import uk.gov.ons.bulk.service.DownloadService.SignedUrlResponse;

public class DownloadServiceUrlTest {

    private static Validator validator;

    @BeforeAll
    public static void setupValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testDownloadServiceUrlExpectedFormatNoParameters() {

        String goodUrl = "https://storage.googleapis.com/"
        + "results_987_654/"
        + "results_987.csv.gz";

        SignedUrlResponse dto = new SignedUrlResponse();
        dto.setSignedUrl(goodUrl);

        Set<ConstraintViolation<SignedUrlResponse>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), 
            () -> "Expected no constraint violations for valid URL, but found: " + violations);
    }

    @Test
    public void testDownloadServiceUrlExpectedFormatWithParameters() {

        String goodUrl = "https://storage.googleapis.com/"
            + "results_987_654/"
            + "results_987.csv.gz"
            + "?X-Goog-Algorithm=GOOG4-RSA-SHA256"
            + "&X-Goog-Credential=service-account%40project-id.iam.gserviceaccount.com%2F20231001%2Fauto%2Fstorage%2Fgoog4_request"
            + "&X-Goog-Date=20231001T120000Z"
            + "&X-Goog-Expires=3600"; // Bunch of random parameters that shouldn't affect validation

        SignedUrlResponse dto = new SignedUrlResponse();
        dto.setSignedUrl(goodUrl);

        Set<ConstraintViolation<SignedUrlResponse>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty(), 
            () -> "Expected no constraint violations for valid URL, but found: " + violations);
    }
 
    @Test
    public void testDownloadServiceUrlUnexpectedFormatFolder() {
        String badUrl = "https://storage.googleapis.com/"
            + "results_catsAreCool/"
            + "results_987.csv.gz";

        SignedUrlResponse dto = new SignedUrlResponse();
        dto.setSignedUrl(badUrl);

        Set<ConstraintViolation<SignedUrlResponse>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty(),
            () -> "Expected at least one constraint violation for malformed URL, but found none.");
    }

    @Test
    public void testDownloadServiceUrlUnexpectedFormatFile() {
        String badUrl = "https://storage.googleapis.com/"
            + "results_987_654/"
            + "BadFile_987.csv.gz";

        SignedUrlResponse dto = new SignedUrlResponse();
        dto.setSignedUrl(badUrl);

        Set<ConstraintViolation<SignedUrlResponse>> violations = validator.validate(dto);

        assertFalse(violations.isEmpty(),
            () -> "Expected at least one constraint violation for malformed URL, but found none.");
    }

}
