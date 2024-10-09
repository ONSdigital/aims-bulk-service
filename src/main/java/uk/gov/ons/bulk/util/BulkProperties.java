package uk.gov.ons.bulk.util;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import uk.gov.ons.bulk.exception.BulkAddressRuntimeException;

import java.util.Properties;

public interface BulkProperties {
  public static String getYamlProperty(String property) {
    YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
    yamlFactory.setResources(new ClassPathResource("application.yml"));
    Properties properties = yamlFactory.getObject();

    assert properties != null : "Could not load properties from application.yml";

    String value = properties.getProperty(property);

    assert value != null : "Property '" + property + "' not found in application.yml";

    return value;
  }
}
