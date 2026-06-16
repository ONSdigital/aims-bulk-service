package uk.gov.ons.bulk.config;

import com.google.cloud.bigquery.BigQuery;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration(proxyBeanMethods = false)
@Profile("test")
public class TestBigQueryConfig {

    @Bean
    @Primary
    BigQuery bigQuery() {
        return Mockito.mock(BigQuery.class);
    }
}

