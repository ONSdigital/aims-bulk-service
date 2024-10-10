package uk.gov.ons.bulk.util;

import org.junit.Test;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;


class BulkPropertiesTest implements BulkProperties{
  @Test
  void missingProperty() {
    Throwable exception = assertThrows(AssertionError.class, () -> {
      BulkProperties.getYamlProperty("not.here");
    });
    assertTrue(exception.getMessage().contains("Property: not.here - not found!"));
  }

  @Test
  void existingProperty() {
    assertEquals("aims-bulk-service", BulkProperties.getYamlProperty("spring.application.name"));
  }

  @Test
  void failingTest() {
    assertEquals("aims-FAILEDvice", BulkProperties.getYamlProperty("spring.application.name"));
  }
}
