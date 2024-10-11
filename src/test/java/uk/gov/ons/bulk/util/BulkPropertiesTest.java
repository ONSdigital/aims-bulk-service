package uk.gov.ons.bulk.util;

import org.junit.Test;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BulkPropertiesTest implements BulkProperties{

  @Test
  public void missingProperty() {
    Throwable exception = assertThrows(AssertionError.class, () -> {
      BulkProperties.getYamlProperty("not.here");
    });
    assertTrue(exception.getMessage().contains("Property: not.here - not found!"));
  }

  @Test
  public void existingProperty() {
    assertEquals("aims-bulk-service", BulkProperties.getYamlProperty("spring.application.name"));
  }

  @Test
  public void failingTest() {
    assertEquals("aims-FAILEDvice", BulkProperties.getYamlProperty("spring.application.name"));
  }
}
