package uk.gov.ons.bulk.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BulkPropertiesTest implements BulkProperties{

  @Test
  void missingProperty() {
    Throwable exception = assertThrows(AssertionError.class, () -> {
      BulkProperties.getYamlProperty("not.here");
    });
    assertTrue(exception.getMessage().contains("Property 'not.here' not found in application.yml"));
  }

  @Test
  void existingProperty() {
    assertEquals("aims-bulk-service", BulkProperties.getYamlProperty("spring.application.name"));
  }
}
