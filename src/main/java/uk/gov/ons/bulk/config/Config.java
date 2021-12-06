package uk.gov.ons.bulk.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.reactive.ReactiveElasticsearchClient;
import org.springframework.data.elasticsearch.client.reactive.ReactiveRestClients;
import org.springframework.data.elasticsearch.repository.config.EnableReactiveElasticsearchRepositories;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

import lombok.Getter;

@Configuration
@EnableReactiveElasticsearchRepositories
public class Config {
	
	@Value("${spring.elasticsearch.rest.uris}")
	private String elasticSearchEndpoint;
	
	@Getter
	@Value("${aims.elasticsearch.index.aux.name}")
	private String auxIndexName;
	
	@Getter
	@Value("${aims.elasticsearch.index.fat.name}")
	private String fatIndexName;
	
	@Getter
	@Value("${aims.elasticsearch.index.skinny.name}")
	private String skinnyIndexName;
	
	@Value("${aims.elasticsearch.client.socket-timeout}")
	private long elasticSocketTimeout;
	
	@Value("${aims.elasticsearch.client.connect-timeout}")
	private long elasticConnectTimeout;
	
	@Bean
	ReactiveElasticsearchClient client() {

		ClientConfiguration clientConfiguration = ClientConfiguration.builder().connectedTo(elasticSearchEndpoint)
				.withConnectTimeout(elasticConnectTimeout).withSocketTimeout(elasticSocketTimeout)
				.withWebClientConfigurer(webClient -> {
					ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
							.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(-1)).build();
					return webClient.mutate().exchangeStrategies(exchangeStrategies).build();
				}).build();

		return ReactiveRestClients.create(clientConfiguration);
	}
}