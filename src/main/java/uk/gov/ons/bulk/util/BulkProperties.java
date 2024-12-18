package uk.gov.ons.bulk.util;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Properties;

public interface BulkProperties {
  static String getYamlProperty(String property) {
    YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
    yamlFactory.setResources(new ClassPathResource("application.yml"));
    Properties properties = yamlFactory.getObject();

    assert properties != null : "Could not load properties from application.yml";

    String propertyValue = properties.getProperty(property);

    assert propertyValue != null : "Property '" + property + "' not found in application.yml";

    return propertyValue;
  }
}
