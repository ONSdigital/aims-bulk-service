package uk.gov.ons.bulk.service;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.reactive.function.client.WebClient;

import com.opencsv.CSVWriter;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import uk.gov.ons.bulk.entities.Address;
import uk.gov.ons.bulk.entities.AuxAddress;
import uk.gov.ons.bulk.entities.HybridAddressFat;
import uk.gov.ons.bulk.entities.HybridAddressSkinny;
import uk.gov.ons.bulk.entities.InputAddress;
import uk.gov.ons.bulk.entities.UnitAddress;
import uk.gov.ons.bulk.exception.BulkAddressException;
import uk.gov.ons.bulk.exception.BulkAddressRuntimeException;
import uk.gov.ons.bulk.json.TokeniserResponse;
import uk.gov.ons.bulk.repository.fat.AddressRepository;
import uk.gov.ons.bulk.repository.fat.HybridAddressFatRepository;
import uk.gov.ons.bulk.repository.skinny.HybridAddressSkinnyRepository;
import uk.gov.ons.bulk.util.AddressMapper;
import uk.gov.ons.bulk.util.HybridAddressFatMapper;
import uk.gov.ons.bulk.util.HybridAddressSkinnyMapper;
import uk.gov.ons.bulk.util.ValidatedAddress;

@Slf4j
@Service
@Validated
public class AddressService {

	@Autowired
	private AddressRepository addressRepository;
	
	@Autowired
	private HybridAddressFatRepository hybridAddressFatRepository;
	
	@Autowired
	private HybridAddressSkinnyRepository hybridAddressSkinnyRepository;

	private final WebClient webClient;

	@Value("${aims.tokeniser.path}")
	private String path;

	@Value("gs://${aims.gcp.bucket}/")
	private String gcsBucket;

	@Autowired
	private ResourceLoader resourceLoader;

	private static final String datePattern = "yyyyMMdd_HHmmss";
	private DateTimeFormatter dateTimeFormater = DateTimeFormatter.ofPattern(datePattern);

	@Autowired
	public AddressService(RestHighLevelClient client, ResourceLoader resourceLoader, 
			@Value("${aims.elasticsearch.index.aux.name}") String indexName,
			@Value("${aims.tokeniser.uri}") String tokeniserEndpoint,
			@Value("${aims.elasticsearch.cluster.fat-enabled}") boolean fatClusterEnabled,
			WebClient.Builder webClientBuilder) {
		
		this.webClient = webClientBuilder.clientConnector((ClientHttpConnector)new ReactorClientHttpConnector(HttpClient.create()
				.wiretap(true))).baseUrl(tokeniserEndpoint).build();
		
		if (fatClusterEnabled) {
			GetIndexRequest request = new GetIndexRequest(indexName);

			try {
				if (!client.indices().exists(request, RequestOptions.DEFAULT)) {
					try (Reader mappingReader = new InputStreamReader(
							resourceLoader.getResource("classpath:mappings.json").getInputStream(),
							Charset.forName("UTF-8"));
							Reader settingsReader = new InputStreamReader(
									resourceLoader.getResource("classpath:settings.json").getInputStream(),
									Charset.forName("UTF-8"))) {

						CreateIndexRequest createRequest = new CreateIndexRequest(indexName);

						createRequest.settings(FileCopyUtils.copyToString(settingsReader), XContentType.JSON);
						createRequest.mapping(FileCopyUtils.copyToString(mappingReader), XContentType.JSON);
						CreateIndexResponse createIndexResponse = client.indices().create(createRequest,
								RequestOptions.DEFAULT);

						if (!createIndexResponse.isAcknowledged()) {
							log.error(String.format("Can not create index %s", indexName));
							throw new BulkAddressRuntimeException(String.format("Can not create index %s", indexName));
						}

					} catch (IOException ioe) {
						log.error(String.format("Can not create index %s", indexName), ioe);
						throw new BulkAddressRuntimeException(String.format("Can not create index %s", indexName), ioe);
					}
				}
			} catch (IOException ioe) {
				log.error(String.format("Can not create index %s", indexName), ioe);
				throw new BulkAddressRuntimeException(String.format("Can not create index %s", indexName), ioe);
			}
		}
	}

	public Mono<Address> createAddress(Address address) {
		return addressRepository.save(address);

	}

	public Flux<Address> createAddresses(List<Address> addresses) {
		return addressRepository.saveAll(addresses);
	}

	public Flux<Address> createAuxAddressesFromCsv(List<ValidatedAddress<AuxAddress>> addresses) {	
		return Flux.fromIterable(addresses).limitRate(20)
				.flatMap(validatedAddress -> addressRepository.saveAll(buildAddress(validatedAddress.getAddress())))
				.doOnError(ex -> Flux.just("Error: " + ex.getMessage()));
	}

	public Flux<HybridAddressSkinny> createSkinnyUnitAddressesFromCsv(List<ValidatedAddress<UnitAddress>> addresses) {
		return Flux.fromIterable(addresses).limitRate(20)
				.flatMap(validatedAddress -> hybridAddressSkinnyRepository.saveAll(buildHybridAddressSkinny(validatedAddress.getAddress())))
				.doOnError(ex -> Flux.just("Error: " + ex.getMessage()));
	}
	
	public Flux<HybridAddressFat> createFatUnitAddressesFromCsv(List<ValidatedAddress<UnitAddress>> addresses) {	
		
		return Flux.fromIterable(addresses).limitRate(20)
				.flatMap(validatedAddress -> hybridAddressFatRepository.saveAll(buildHybridAddressFat(validatedAddress.getAddress())))
				.doOnError(ex -> Flux.just("Error: " + ex.getMessage()));
	}

	public Mono<Address> createAddressFromMsg(InputAddress pubSubAddress) throws BulkAddressException {

		/*
		 * Can't @Valid the InputAddress for a PubSub msg as it can have legitimate
		 * empty fields e.g. lat/long. Only UPRN is mandatory.
		 */
		if (pubSubAddress.getUprn().isBlank()) {
			throw new BulkAddressException("UPRN is mandatory.");
		}

		return buildAddress(pubSubAddress).flatMap(address -> addressRepository.save(address))
				.doOnError(ex -> Mono.just("Error: " + ex.getMessage()))
				.doOnSuccess(address -> log.debug(String.format("Added address: %s", address)));
	}

	private Mono<Address> buildAddress(InputAddress inputAddress) {

		log.debug(String.format("Input Address: %s", inputAddress.toString()));

		return webClient.get().uri(path, inputAddress.getAddressAll()).retrieve().bodyToMono(TokeniserResponse.class)
				.map(tokeniserResponse -> {
					log.debug(String.format("Tokeniser Response: %s", tokeniserResponse.toString()));
					Address address = AddressMapper.from(inputAddress, tokeniserResponse);
					return address;
				});
	}
	
	private Mono<HybridAddressSkinny> buildHybridAddressSkinny(UnitAddress unitAddress) {

		log.debug(String.format("Input Address: %s", unitAddress.toString()));

		return webClient.get().uri(path, unitAddress.getAddressAll())
				.retrieve()
				.onStatus(HttpStatus::is4xxClientError, response -> 
					Mono.error(new BulkAddressRuntimeException("Client error")))
				.onStatus(HttpStatus::is5xxServerError, response -> 
					Mono.error(new BulkAddressRuntimeException("Server error")))
				.bodyToMono(TokeniserResponse.class)
				.timeout(Duration.ofSeconds(5))
				.map(tokeniserResponse -> {
					log.debug(String.format("Tokeniser Response: %s", tokeniserResponse.toString()));
					HybridAddressSkinny address = HybridAddressSkinnyMapper.from(unitAddress, tokeniserResponse);
					return address;
				});
	}	

	private Mono<HybridAddressFat> buildHybridAddressFat(UnitAddress unitAddress) {

		log.debug(String.format("Input Address: %s", unitAddress.toString()));

		return webClient.get().uri(path, unitAddress.getAddressAll())
				.retrieve()
				.onStatus(HttpStatus::is4xxClientError, response -> 
					Mono.error(new BulkAddressRuntimeException("Client error")))
				.onStatus(HttpStatus::is5xxServerError, response -> 
					Mono.error(new BulkAddressRuntimeException("Server error")))
				.bodyToMono(TokeniserResponse.class)
				.timeout(Duration.ofSeconds(5))
				.map(tokeniserResponse -> {
					log.debug(String.format("Tokeniser Response: %s", tokeniserResponse.toString()));
					HybridAddressFat address = HybridAddressFatMapper.from(unitAddress, tokeniserResponse);
					return address;
				});
	}	
	
	public <T> String writeBadAddressesCsv(List<ValidatedAddress<T>> badAddresses, String fileName) throws Exception {

		// TODO: Make this asynchronous - it could be a large file!
		Resource gcsFile = resourceLoader.getResource(
				String.format("%s%s_%s", gcsBucket, LocalDateTime.now().format((dateTimeFormater)), fileName));

		CSVWriter writer = new CSVWriter(new OutputStreamWriter(((WritableResource) gcsFile).getOutputStream()));

		// Write the header
		writer.writeNext(badAddresses.get(0).getHeader().toArray(new String[0]));
		badAddresses.forEach(address -> writer.writeNext(address.getRow().toArray(new String[0])));
		writer.close();

		return gcsFile.getFilename();
	}
}