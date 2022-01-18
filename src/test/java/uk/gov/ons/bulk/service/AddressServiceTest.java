package uk.gov.ons.bulk.service;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.cloud.gcp.pubsub.support.AcknowledgeablePubsubMessage;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.elasticsearch.ElasticsearchContainer;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import lombok.extern.slf4j.Slf4j;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import uk.gov.ons.bulk.entities.Address;
import uk.gov.ons.bulk.entities.AuxAddress;
import uk.gov.ons.bulk.entities.HybridAddressFat;
import uk.gov.ons.bulk.entities.HybridAddressSkinny;
import uk.gov.ons.bulk.entities.InputAddress;
import uk.gov.ons.bulk.entities.Lpi;
import uk.gov.ons.bulk.entities.LpiSkinny;
import uk.gov.ons.bulk.entities.Message;
import uk.gov.ons.bulk.entities.Tokens;
import uk.gov.ons.bulk.entities.UnitAddress;
import uk.gov.ons.bulk.json.TokeniserResponse;
import uk.gov.ons.bulk.repository.fat.AddressRepository;
import uk.gov.ons.bulk.repository.fat.HybridAddressFatRepository;
import uk.gov.ons.bulk.repository.skinny.HybridAddressSkinnyRepository;
import uk.gov.ons.bulk.util.BulkAddressConstants.CountryCode;
import uk.gov.ons.bulk.util.ValidatedAddress;

@Slf4j
@SpringBootTest()
@ExtendWith(SpringExtension.class)
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
@ActiveProfiles("test")
class AddressServiceTest {

	@Autowired
	private AddressService addressService;

	@Autowired
	private AddressRepository repository;
	@Autowired
	private HybridAddressFatRepository fatRepository;
	@Autowired
	private HybridAddressSkinnyRepository skinnyRepository;

	private final ElasticsearchContainer elastic;

	private static MockWebServer mockBackEnd;

	@Autowired
	private PubSubTemplate template;

	private String organisationName = "ACME FLOWERS LTD";
	private Short buildingNumber = 78;
	private Short buildingNumber5 = 89;
	private String buildingName = "CHESTERFIELD LODGE";
	private String buildingName2 = "205B NASH HOUSE";
	private String buildingName3 = "17 WILLOW PONTOON NORTHAMPTON MARINA";
	private String buildingName4 = "GASHOLDERS BUILDING 1 LEWIS";
	private String buildingName5 = "ABBOTTS WHARF MOORINGS";
	private String buildingName6 = "BERTH 42 POPLAR DOCK MARINA";
	private String subBuildingName = "FLAT C";
	private String subBuildingName4 = "APARTMENT 10-64";
	private String subBuildingName5 = "BERTH 3";
	private Short paoStartNumber = 78;
	private Short paoStartNumber4 = 1;
	private Short paoStartNumber5 = 89;
	private Short paoStartNumber6 = 42;
	private String saoStartSuffix = "C";
	private Short saoStartNumber4 = 10;
	private Short saoStartNumber5 = 3;
	private Short saoEndNumber4 = 64;
	private Short paoStartNumber2 = 205;
	private String saoStartSuffix2 = "B";
	private Short paoStartNumber3 = 17;
	private String addressLine1 = "ACME FLOWERS LTD";
	private String addressLine2 = "FLAT C CHESTERFIELD LODGE";
	private String addressLine3 = "78 BEULAH HILL";
	private String addressLine21 = "205b Nash House Old Oak Lane";
	private String addressLine22 = "";
	private String addressLine23 = "";
	private String addressLine31 = "17 Willow Pontoon Northampton Marina Victoria Promenade";
	private String addressLine32 = "";
	private String addressLine33 = "";
	private String addressLine41 = "Apartment 10-64 Gasholders Building";
	private String addressLine42 = "1 Lewis Cubitt Square";
	private String addressLine43 = "";
	private String addressLine51 = "Berth 3";
	private String addressLine52 = "Abbotts Wharf Moorings";
	private String addressLine53 = "89 Stainsby Road";
	private String addressLine61 = "Berth 42";
	private String addressLine62 = "Poplar Dock Marina";
	private String addressLine63 = "Boardwalk Place";
	private String streetName = "BEULAH HILL";
	private String streetName2 = "OLD OAK LANE";
	private String streetName3 = "VICTORIA PROMENADE";
	private String streetName4 = "CUBITT SQUARE";
	private String streetName5 = "STAINSBY ROAD";
	private String streetName6 = "BOARDWALK PLACE";
	private String locality = "WINDLEYBURY";
	private String townName = "LONDON";
	private String postcode = "SE19 3EX";
	private String postcodeIn = "3EX";
	private String postcodeOut = "SE19";
	private String postcode2 = "GU16 6DG";
	private String postcodeIn2 = "6DG";
	private String postcodeOut2 = "GU16";
	private String postcode3 = "GU17 7DG";
	private String postcodeIn3 = "7DG";
	private String postcodeOut3 = "GU17";
	private String postcode4 = "GU16 6DG";
	private String postcodeIn4 = "6DG";
	private String postcodeOut4 = "GU16";
	private String postcode5 = "GU16 6DR";
	private String postcodeIn5 = "6DR";
	private String postcodeOut5 = "GU16";
	private String addressLevel = "U";
	private String latitude = "55.55";
	private String longitude = "-1.23";
	private String latitude2 = "51.5290049";
	private String longitude2 = "-0.2503676";
	private String latitude3 = "52.232804";
	private String longitude3 = "-0.8894609";
	private Long uprn = 99L;
	private Long uprn2 = 100061542998L;
	private Long uprn3 = 100061542999L;
	private Long uprn4 = 999L;
	private Long uprn5 = 9998L;
	private Long uprn6 = 1234567891013L;
	private String classificationCode = "RD03";
	private String classificationCode2 = "RD06";
	private String classificationCode3 = "RD07";
	private String censusAddressType = "HH";
	private String censusEstabType = "Household";
	private CountryCode countryCode = CountryCode.E;
	private CountryCode countryCode2 = CountryCode.W;
	private Long censusEstabUprn = 808L;

	private List<Address.Lpi> testAddressLpi = List.of(new Address.Lpi());
	private List<Address.Nisra> testAddressNisra = List.of(new Address.Nisra());

	private Tokens tokens = new Tokens.TokensBuilder().organisationName(organisationName)
			.subBuildingName(subBuildingName).buildingName(buildingName).buildingNumber(buildingNumber)
			.paoStartNumber(paoStartNumber).saoStartSuffix(saoStartSuffix).saoStartNumber(null) // Ignored - doesn't
																								// appear in index
			.streetName(streetName).townName(townName).addressLevel(addressLevel).uprn(uprn).latitude(latitude)
			.longitude(longitude).addressLine1(addressLine1).addressLine2(addressLine2).addressLine3(addressLine3)
			.postcode(postcode).build();

	private String postcodeStreetTown = (postcode + "_" + streetName + "_" + townName)
			.replace(".","")
			.replace("'","");

	private Address address = new Address(uprn, postcodeIn, postcodeOut, classificationCode, censusAddressType,
			censusEstabType, censusEstabUprn, countryCode, postcode, postcodeStreetTown, tokens.getAddressAll(), tokens,
			testAddressLpi, testAddressNisra);

	private Tokens tokens2 = new Tokens.TokensBuilder().buildingName(buildingName2).paoStartNumber(paoStartNumber2)
			.saoStartSuffix(saoStartSuffix2).streetName(streetName2).locality(locality).townName(townName)
			.addressLevel(addressLevel).uprn(uprn2).latitude(latitude2).longitude(longitude2)
			.addressLine1(addressLine21).addressLine2(addressLine22).addressLine3(addressLine23).postcode(postcode2)
			.build();

	private String postcodeStreetTown2 = (postcode2 + "_" + streetName2 + "_" + locality)
			.replace(".","")
			.replace("'","");

	private Address address2 = new Address(uprn2, postcodeIn2, postcodeOut2, classificationCode2, censusAddressType,
			censusEstabType, censusEstabUprn, countryCode, postcode2, postcodeStreetTown2, tokens2.getAddressAll(),
			tokens2, testAddressLpi, testAddressNisra);

	private Tokens tokens3 = new Tokens.TokensBuilder().buildingName(buildingName3).paoStartNumber(paoStartNumber3)
			.streetName(streetName3).locality(locality).townName(townName).addressLevel(addressLevel).uprn(uprn3)
			.latitude(latitude3).longitude(longitude3).addressLine1(addressLine31).addressLine2(addressLine32)
			.addressLine3(addressLine33).postcode(postcode3).build();

	private String postcodeStreetTown3 = (postcode3 + "_" + streetName3 + "_" + locality)
			.replace(".","")
			.replace("'","");

	private Address address3 = new Address(uprn3, postcodeIn3, postcodeOut3, classificationCode3, censusAddressType,
			censusEstabType, censusEstabUprn, countryCode, postcode3, postcodeStreetTown3, tokens3.getAddressAll(),
			tokens3, testAddressLpi, testAddressNisra);

	private Tokens tokens4 = new Tokens.TokensBuilder().organisationName("").departmentName("").subBuildingName("")
			.buildingName(buildingName4).subBuildingName(subBuildingName4).paoStartSuffix("")
			.paoStartNumber(paoStartNumber4).paoEndSuffix("").saoStartNumber(saoStartNumber4).saoStartSuffix("")
			.saoEndNumber(saoEndNumber4).saoEndSuffix("").streetName(streetName4).locality(locality).townName("")
			.addressLevel(addressLevel).uprn(uprn4).addressLine1(addressLine41).addressLine2(addressLine42)
			.addressLine3(addressLine43).postcode(postcode4).build();

	private String postcodeStreetTown4 = (postcode4 + "_" + streetName4 + "_" + locality)
			.replace(".","")
			.replace("'","");

	private Address address4 = new Address(uprn4, postcodeIn4, postcodeOut4, classificationCode, censusAddressType,
			censusEstabType, censusEstabUprn, countryCode, postcode4, postcodeStreetTown4, tokens4.getAddressAll(),
			tokens4, testAddressLpi, testAddressNisra);

	private Tokens tokens5 = new Tokens.TokensBuilder().organisationName(organisationName).departmentName("")
			.buildingName(buildingName5).subBuildingName(subBuildingName5).buildingNumber(buildingNumber5)
			.paoStartSuffix("").paoStartNumber(paoStartNumber5).paoEndSuffix("").saoStartNumber(saoStartNumber5)
			.saoStartSuffix("").saoEndSuffix("").streetName(streetName5).locality("").townName(townName)
			.addressLevel(addressLevel).uprn(uprn5).latitude(latitude).longitude(longitude).addressLine1(addressLine51)
			.addressLine2(addressLine52).addressLine3(addressLine53).postcode(postcode5).build();

	private String postcodeStreetTown5 = (postcode5 + "_" + streetName5 + "_" + townName)
			.replace(".","")
			.replace("'","");

	private Address address5 = new Address(uprn5, postcodeIn5, postcodeOut5, classificationCode, censusAddressType,
			censusEstabType, censusEstabUprn, countryCode, postcode5, postcodeStreetTown5, tokens5.getAddressAll(),
			tokens5, testAddressLpi, testAddressNisra);

	private Tokens tokens6 = new Tokens.TokensBuilder().organisationName("").departmentName("")
			.buildingName(buildingName6).subBuildingName("").paoStartSuffix("").paoStartNumber(paoStartNumber6)
			.paoEndSuffix("").saoStartSuffix("").saoEndSuffix("").streetName(streetName6).locality("")
			.townName(townName).addressLevel(addressLevel).uprn(uprn6).addressLine1(addressLine61)
			.addressLine2(addressLine62).addressLine3(addressLine63).postcode(postcode5).build();

	private String postcodeStreetTown6 = (postcode5 + "_" + streetName6 + "_" + townName)
			.replace(".","")
			.replace("'","");

	private Address address6 = new Address(uprn6, postcodeIn5, postcodeOut5, classificationCode3, censusAddressType,
			censusEstabType, censusEstabUprn, countryCode2, postcode5, postcodeStreetTown6, tokens6.getAddressAll(),
			tokens6, testAddressLpi, testAddressNisra);
	
	private List<Address> addresses = Arrays.asList(address2, address3);
	private List<ValidatedAddress<UnitAddress>> validatedAddresses;
	private TokeniserResponse mockTokeniserUnitResponse1;
	private TokeniserResponse mockTokeniserUnitResponse2;
	private Lpi fatLpi1;
	private Lpi fatLpi2;
	private LpiSkinny skinnyLpi1;
	private LpiSkinny skinnyLpi2;
	private HybridAddressFat hybridAddressFat1;
	private HybridAddressFat hybridAddressFat2;
	private HybridAddressSkinny hybridAddressSkinny1;
	private HybridAddressSkinny hybridAddressSkinny2;
	
	public AddressServiceTest() throws IOException {

		elastic = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:7.9.3");
		elastic.start();

		System.setProperty("spring.elasticsearch.rest.uris",
				elastic.getContainerIpAddress() + ":" + elastic.getFirstMappedPort());
		System.setProperty("spring.data.elasticsearch.client.reactive.endpoints",
				elastic.getContainerIpAddress() + ":" + elastic.getFirstMappedPort());

		mockBackEnd = new MockWebServer();
		mockBackEnd.start();

		System.setProperty("aims.tokeniser.uri", String.format("http://localhost:%s", mockBackEnd.getPort()));
	}

	@BeforeAll
	public void beforeAll() throws Exception {
		
		/*
		 * This test data is used for the skinny and fat unit-address tests
		 */

		Reader reader = new BufferedReader(new FileReader(new File("src/test/resources/unit-addresses-test.csv")));

		CsvToBean<UnitAddress> csvToBean = new CsvToBeanBuilder<UnitAddress>(reader).withType(UnitAddress.class)
				.withIgnoreLeadingWhiteSpace(true).withIgnoreEmptyLine(true).withSeparator('|').build();

		validatedAddresses = csvToBean.parse().stream().map(address -> new ValidatedAddress<UnitAddress>(address))
				.collect(Collectors.toList());

		mockTokeniserUnitResponse1 = new TokeniserResponse();
		mockTokeniserUnitResponse1.setOrganisationName("UNIVERSITY OF BIRMINGHAM");
		mockTokeniserUnitResponse1.setDepartmentName("");
		mockTokeniserUnitResponse1.setSubBuildingName("ROOM 5 BLOCK 6 FLAT 49");
		mockTokeniserUnitResponse1.setBuildingName("MAPLE BANK THE VALE VILLAGE");
		mockTokeniserUnitResponse1.setBuildingNumber("");
		mockTokeniserUnitResponse1.setPaoStartNumber("");
		mockTokeniserUnitResponse1.setPaoStartSuffix("");
		mockTokeniserUnitResponse1.setPaoEndNumber("");
		mockTokeniserUnitResponse1.setPaoEndSuffix("");
		mockTokeniserUnitResponse1.setSaoStartNumber("5");
		mockTokeniserUnitResponse1.setSaoStartSuffix("");
		mockTokeniserUnitResponse1.setSaoEndNumber("");
		mockTokeniserUnitResponse1.setSaoEndSuffix("");
		mockTokeniserUnitResponse1.setStreetName("CHURCH ROAD");
		mockTokeniserUnitResponse1.setLocality("");
		mockTokeniserUnitResponse1.setTownName("BIRMINGHAM");
		mockTokeniserUnitResponse1.setPostcode("B15 3TE");
		mockTokeniserUnitResponse1.setPostcodeIn("3TE");
		mockTokeniserUnitResponse1.setPostcodeOut("B15");

		mockTokeniserUnitResponse2 = new TokeniserResponse();
		mockTokeniserUnitResponse2.setOrganisationName("UNIVERSITY OF CENTRAL LANCASHIRE");
		mockTokeniserUnitResponse2.setDepartmentName("");
		mockTokeniserUnitResponse2.setSubBuildingName("ROOM 1 FLAT 02");
		mockTokeniserUnitResponse2.setBuildingName("DERWENT HALL PRESTON CAMPUS");
		mockTokeniserUnitResponse2.setBuildingNumber("5");
		mockTokeniserUnitResponse2.setPaoStartNumber("5");
		mockTokeniserUnitResponse2.setPaoStartSuffix("");
		mockTokeniserUnitResponse2.setPaoEndNumber("");
		mockTokeniserUnitResponse2.setPaoEndSuffix("");
		mockTokeniserUnitResponse2.setSaoStartNumber("1");
		mockTokeniserUnitResponse2.setSaoStartSuffix("");
		mockTokeniserUnitResponse2.setSaoEndNumber("");
		mockTokeniserUnitResponse2.setSaoEndSuffix("");
		mockTokeniserUnitResponse2.setStreetName("VICTORIA STREET");
		mockTokeniserUnitResponse2.setLocality("");
		mockTokeniserUnitResponse2.setTownName("PRESTON");
		mockTokeniserUnitResponse2.setPostcode("PR1 7QR");
		mockTokeniserUnitResponse2.setPostcodeIn("7QR");
		mockTokeniserUnitResponse2.setPostcodeOut("PR1");

		fatLpi1 = new Lpi.LpiBuilder().organisationName(mockTokeniserUnitResponse1.getOrganisationName())
				.organisation(validatedAddresses.get(0).getAddress().getOrganisationName()).departmentName(mockTokeniserUnitResponse1.getDepartmentName())
				.subBuildingName(mockTokeniserUnitResponse1.getSubBuildingName())
				.buildingName(mockTokeniserUnitResponse1.getBuildingName())
				.buildingNumber(!mockTokeniserUnitResponse1.getBuildingNumber().isEmpty()
						? Short.valueOf(mockTokeniserUnitResponse1.getBuildingNumber())
						: null)
				.streetName(mockTokeniserUnitResponse1.getStreetName()).locality(mockTokeniserUnitResponse1.getLocality())
				.townName(mockTokeniserUnitResponse1.getTownName())
				.townNameUnitAddress(validatedAddresses.get(0).getAddress().getTownName())
				.postcode(mockTokeniserUnitResponse1.getPostcode())
				.easting(Float.valueOf(validatedAddresses.get(0).getAddress().getBngEasting()))
				.northing(Float.valueOf(validatedAddresses.get(0).getAddress().getBngNorthing()))
				.latitude(validatedAddresses.get(0).getAddress().getLatitude())
				.longitude(validatedAddresses.get(0).getAddress().getLongitude())
				.paoStartNumber(!mockTokeniserUnitResponse1.getPaoStartNumber().isEmpty()
						? Short.valueOf(mockTokeniserUnitResponse1.getPaoStartNumber())
						: null)
				.paoStartSuffix(mockTokeniserUnitResponse1.getPaoStartSuffix())
				.postcodeLocator(mockTokeniserUnitResponse1.getPostcode())
				.saoStartNumber(Short.valueOf(mockTokeniserUnitResponse1.getSaoStartNumber()))
				.streetDescriptor(mockTokeniserUnitResponse1.getStreetName())
				.uprn(Long.valueOf(validatedAddresses.get(0).getAddress().getUprn()))
				.addressLine1(validatedAddresses.get(0).getAddress().getAddressLine1())
				.addressLine2(validatedAddresses.get(0).getAddress().getAddressLine2())
				.addressLine3(validatedAddresses.get(0).getAddress().getAddressLine3())
				.paoEndNumber(!mockTokeniserUnitResponse1.getPaoEndNumber().isEmpty()
						? Short.parseShort(mockTokeniserUnitResponse1.getPaoEndNumber())
						: null)
				.paoEndSuffix(mockTokeniserUnitResponse1.getPaoEndSuffix())
				.saoEndNumber(!mockTokeniserUnitResponse1.getSaoEndNumber().isEmpty()
						? Short.parseShort(mockTokeniserUnitResponse1.getSaoEndNumber())
						: null)
				.saoEndSuffix(mockTokeniserUnitResponse1.getSaoEndSuffix())
				.saoStartSuffix(mockTokeniserUnitResponse1.getSaoStartSuffix())
				.lpiLogicalStatus((byte) 1)
				.language("ENG")
				.build();

		fatLpi2 = new Lpi.LpiBuilder().organisationName(mockTokeniserUnitResponse2.getOrganisationName())
				.organisation(validatedAddresses.get(1).getAddress().getOrganisationName()).departmentName(mockTokeniserUnitResponse2.getDepartmentName())
				.subBuildingName(mockTokeniserUnitResponse2.getSubBuildingName())
				.buildingName(mockTokeniserUnitResponse2.getBuildingName())
				.buildingNumber(Short.valueOf(mockTokeniserUnitResponse2.getBuildingNumber()))
				.streetName(mockTokeniserUnitResponse2.getStreetName()).locality(mockTokeniserUnitResponse2.getLocality())
				.townName(mockTokeniserUnitResponse2.getTownName())
				.townNameUnitAddress(validatedAddresses.get(1).getAddress().getTownName())
				.postcode(mockTokeniserUnitResponse2.getPostcode())
				.easting(Float.valueOf(validatedAddresses.get(1).getAddress().getBngEasting()))
				.northing(Float.valueOf(validatedAddresses.get(1).getAddress().getBngNorthing()))
				.latitude(validatedAddresses.get(1).getAddress().getLatitude())
				.longitude(validatedAddresses.get(1).getAddress().getLongitude())
				.paoStartNumber(Short.valueOf(mockTokeniserUnitResponse2.getPaoStartNumber()))
				.paoStartSuffix(mockTokeniserUnitResponse2.getPaoStartSuffix())
				.postcodeLocator(mockTokeniserUnitResponse2.getPostcode())
				.saoStartNumber(Short.valueOf(mockTokeniserUnitResponse2.getSaoStartNumber()))
				.streetDescriptor(mockTokeniserUnitResponse2.getStreetName())
				.uprn(Long.valueOf(validatedAddresses.get(1).getAddress().getUprn()))
				.addressLine1(validatedAddresses.get(1).getAddress().getAddressLine1())
				.addressLine2(validatedAddresses.get(1).getAddress().getAddressLine2())
				.addressLine3(validatedAddresses.get(1).getAddress().getAddressLine3())
				.paoEndNumber(!mockTokeniserUnitResponse2.getPaoEndNumber().isEmpty()
						? Short.parseShort(mockTokeniserUnitResponse2.getPaoEndNumber())
						: null)
				.paoEndSuffix(mockTokeniserUnitResponse2.getPaoEndSuffix())
				.saoEndNumber(!mockTokeniserUnitResponse2.getSaoEndNumber().isEmpty()
						? Short.parseShort(mockTokeniserUnitResponse2.getSaoEndNumber())
						: null)
				.saoEndSuffix(mockTokeniserUnitResponse2.getSaoEndSuffix())
				.saoStartSuffix(mockTokeniserUnitResponse2.getSaoStartSuffix())
				.lpiLogicalStatus((byte) 1)
				.language("ENG")
				.build();

		String fatPostcodeStreetTown1 = (
					mockTokeniserUnitResponse1.getPostcode() + "_" +
					mockTokeniserUnitResponse1.getStreetName() + "_" +
					mockTokeniserUnitResponse1.getTownName() )
				.replace(".","")
				.replace("'","");

		hybridAddressFat1 = new HybridAddressFat(Long.valueOf(validatedAddresses.get(0).getAddress().getUprn()),
				List.of(fatLpi1), validatedAddresses.get(0).getAddress().getAbpCode(),
				validatedAddresses.get(0).getAddress().getAddressType(),
				validatedAddresses.get(0).getAddress().getEstabType(),
				validatedAddresses.get(0).getAddress().getPostcode(), CountryCode.E,
				fatPostcodeStreetTown1,
				mockTokeniserUnitResponse1.getTownName());

		String fatPostcodeStreetTown2 = (
					mockTokeniserUnitResponse2.getPostcode() + "_" +
					mockTokeniserUnitResponse2.getStreetName() + "_" +
					mockTokeniserUnitResponse2.getTownName() )
				.replace(".","")
				.replace("'","");

		hybridAddressFat2 = new HybridAddressFat(Long.valueOf(validatedAddresses.get(1).getAddress().getUprn()),
				List.of(fatLpi2), validatedAddresses.get(1).getAddress().getAbpCode(),
				validatedAddresses.get(1).getAddress().getAddressType(),
				validatedAddresses.get(1).getAddress().getEstabType(),
				validatedAddresses.get(1).getAddress().getPostcode(), CountryCode.E,
				fatPostcodeStreetTown2,
				mockTokeniserUnitResponse2.getTownName());
		
		skinnyLpi1 = new LpiSkinny.LpiSkinnyBuilder()
				.organisation(validatedAddresses.get(0).getAddress().getOrganisationName())
				.organisationName(mockTokeniserUnitResponse1.getOrganisationName())
				.departmentName(mockTokeniserUnitResponse1.getDepartmentName())
				.subBuildingName(mockTokeniserUnitResponse1.getSubBuildingName())
				.buildingName(mockTokeniserUnitResponse1.getBuildingName())
				.buildingNumber(!mockTokeniserUnitResponse1.getBuildingNumber().isEmpty()
						? Short.valueOf(mockTokeniserUnitResponse1.getBuildingNumber())
						: null)
				.streetName(mockTokeniserUnitResponse1.getStreetName())
				.locality(mockTokeniserUnitResponse1.getLocality())
				.townName(mockTokeniserUnitResponse1.getTownName())
				.townNameUnitAddress(validatedAddresses.get(0).getAddress().getTownName())
				.postcode(mockTokeniserUnitResponse1.getPostcode())
				.easting(Float.valueOf(validatedAddresses.get(0).getAddress().getBngEasting()))
				.northing(Float.valueOf(validatedAddresses.get(0).getAddress().getBngNorthing()))
				.latitude(validatedAddresses.get(0).getAddress().getLatitude())
				.longitude(validatedAddresses.get(0).getAddress().getLongitude())
				.paoStartNumber(!mockTokeniserUnitResponse1.getPaoStartNumber().isEmpty()
						? Short.valueOf(mockTokeniserUnitResponse1.getPaoStartNumber())
						: null)
				.paoStartSuffix(mockTokeniserUnitResponse1.getPaoStartSuffix())
				.postcodeLocator(mockTokeniserUnitResponse1.getPostcode())
				.saoStartNumber(Short.valueOf(mockTokeniserUnitResponse1.getSaoStartNumber()))
				.streetDescriptor(mockTokeniserUnitResponse1.getStreetName())
				.uprn(Long.valueOf(validatedAddresses.get(0).getAddress().getUprn()))
				.addressLine1(validatedAddresses.get(0).getAddress().getAddressLine1())
				.addressLine2(validatedAddresses.get(0).getAddress().getAddressLine2())
				.addressLine3(validatedAddresses.get(0).getAddress().getAddressLine3())
				.lpiLogicalStatus((byte) 1)
				.language("ENG")
				.build();

		skinnyLpi2 = new LpiSkinny.LpiSkinnyBuilder()
				.organisation(validatedAddresses.get(1).getAddress().getOrganisationName())
				.organisationName(mockTokeniserUnitResponse2.getOrganisationName())
				.departmentName(mockTokeniserUnitResponse2.getDepartmentName())
				.subBuildingName(mockTokeniserUnitResponse2.getSubBuildingName())
				.buildingName(mockTokeniserUnitResponse2.getBuildingName())
				.buildingNumber(Short.valueOf(mockTokeniserUnitResponse2.getBuildingNumber()))
				.streetName(mockTokeniserUnitResponse2.getStreetName())
				.locality(mockTokeniserUnitResponse2.getLocality())
				.townName(mockTokeniserUnitResponse2.getTownName())
				.townNameUnitAddress(validatedAddresses.get(1).getAddress().getTownName())
				.postcode(mockTokeniserUnitResponse2.getPostcode())
				.easting(Float.valueOf(validatedAddresses.get(1).getAddress().getBngEasting()))
				.northing(Float.valueOf(validatedAddresses.get(1).getAddress().getBngNorthing()))
				.latitude(validatedAddresses.get(1).getAddress().getLatitude())
				.longitude(validatedAddresses.get(1).getAddress().getLongitude())
				.paoStartNumber(Short.valueOf(mockTokeniserUnitResponse2.getPaoStartNumber()))
				.paoStartSuffix(mockTokeniserUnitResponse2.getPaoStartSuffix())
				.postcodeLocator(mockTokeniserUnitResponse2.getPostcode())
				.saoStartNumber(Short.valueOf(mockTokeniserUnitResponse2.getSaoStartNumber()))
				.streetDescriptor(mockTokeniserUnitResponse2.getStreetName())
				.uprn(Long.valueOf(validatedAddresses.get(1).getAddress().getUprn()))
				.addressLine1(validatedAddresses.get(1).getAddress().getAddressLine1())
				.addressLine2(validatedAddresses.get(1).getAddress().getAddressLine2())
				.addressLine3(validatedAddresses.get(1).getAddress().getAddressLine3())
				.lpiLogicalStatus((byte) 1)
				.language("ENG")
				.build();

		String skinnyPostcodeStreetTown1 = (
					mockTokeniserUnitResponse1.getPostcode() + "_" +
					mockTokeniserUnitResponse1.getStreetName() + "_" +
					mockTokeniserUnitResponse1.getTownName() )
				.replace(".","")
				.replace("'","");

		hybridAddressSkinny1 = new HybridAddressSkinny(Long.valueOf(validatedAddresses.get(0).getAddress().getUprn()),
				List.of(skinnyLpi1), validatedAddresses.get(0).getAddress().getAbpCode(),
				validatedAddresses.get(0).getAddress().getAddressType(),
				validatedAddresses.get(0).getAddress().getEstabType(),
				validatedAddresses.get(0).getAddress().getPostcode(), CountryCode.E,
				skinnyPostcodeStreetTown1,
				mockTokeniserUnitResponse1.getTownName());

		String skinnyPostcodeStreetTown2 = (
					mockTokeniserUnitResponse2.getPostcode() + "_" +
					mockTokeniserUnitResponse2.getStreetName() + "_" +
					mockTokeniserUnitResponse2.getTownName() )
				.replace(".","")
				.replace("'","");

		hybridAddressSkinny2 = new HybridAddressSkinny(Long.valueOf(validatedAddresses.get(1).getAddress().getUprn()),
				List.of(skinnyLpi2), validatedAddresses.get(1).getAddress().getAbpCode(),
				validatedAddresses.get(1).getAddress().getAddressType(),
				validatedAddresses.get(1).getAddress().getEstabType(),
				validatedAddresses.get(1).getAddress().getPostcode(), CountryCode.E,
				skinnyPostcodeStreetTown2,
				mockTokeniserUnitResponse2.getTownName());
	}
	
	@AfterAll
	public void tear() throws IOException {

		elastic.stop();
		elastic.close();
		mockBackEnd.shutdown();
	}

	@Test
	@Order(value = 1)
	public void testCreateAddress() {

		StepVerifier.create(addressService.createAddress(address)).assertNext(response -> {
			log.info(response.toString());
			assertNotNull(response);
			assertEquals(address.getUprn(), response.getUprn());
			assertEquals(address.getTokens().getAddressAll(), response.getTokens().getAddressAll());
		}).verifyComplete();
	}

	@Test
	@Order(value = 2)
	public void testCreateAddresses() {

		StepVerifier.create(addressService.createAddresses(addresses)).expectNextSequence(addresses).verifyComplete();
	}

	@Test
	@Order(value = 3)
	public void testCreateAddressesFromCsv() throws Exception {

		AuxAddress csvAddress = new AuxAddress();
		csvAddress.setUprn("999");
		csvAddress.setRegion("E12000009");
		csvAddress.setAddressLine1("Apartment 10-64 Gasholders Building");
		csvAddress.setAddressLine2("1 Lewis Cubitt Square");
		csvAddress.setAddressLine3("");
		csvAddress.setTownName("Windleybury");
		csvAddress.setPostcode("GU166DG");
		csvAddress.setAbpCode(classificationCode);
		csvAddress.setEstabType(censusEstabType);
		csvAddress.setAddressType(censusAddressType);
		csvAddress.setAddressLevel(addressLevel);
		csvAddress.setEstabUprn("808");

		TokeniserResponse mockTokeniserResponse = new TokeniserResponse();
		mockTokeniserResponse.setOrganisationName("");
		mockTokeniserResponse.setDepartmentName("");
		mockTokeniserResponse.setSubBuildingName("APARTMENT 10-64");
		mockTokeniserResponse.setBuildingName("GASHOLDERS BUILDING 1 LEWIS");
		mockTokeniserResponse.setBuildingNumber("");
		mockTokeniserResponse.setPaoStartNumber("1");
		mockTokeniserResponse.setPaoStartSuffix("");
		mockTokeniserResponse.setPaoEndNumber("");
		mockTokeniserResponse.setPaoEndSuffix("");
		mockTokeniserResponse.setSaoStartNumber("10");
		mockTokeniserResponse.setSaoStartSuffix("");
		mockTokeniserResponse.setSaoEndNumber("64");
		mockTokeniserResponse.setSaoEndSuffix("");
		mockTokeniserResponse.setStreetName("CUBITT SQUARE");
		mockTokeniserResponse.setLocality("WINDLEYBURY");
		mockTokeniserResponse.setTownName("");
		mockTokeniserResponse.setPostcode("GU16 6DG");
		mockTokeniserResponse.setPostcodeIn("6DG");
		mockTokeniserResponse.setPostcodeOut("GU16");

		AuxAddress csvAddress2 = new AuxAddress();
		csvAddress2.setUprn("1234567891013");
		csvAddress2.setRegion("W99999999");
		csvAddress2.setAddressLine1("Berth 42");
		csvAddress2.setAddressLine2("Poplar Dock Marina");
		csvAddress2.setAddressLine3("Boardwalk Place");
		csvAddress2.setTownName("London");
		csvAddress2.setPostcode("GU166DR");
		csvAddress2.setAbpCode(classificationCode3);
		csvAddress2.setEstabType(censusEstabType);
		csvAddress2.setAddressType(censusAddressType);
		csvAddress2.setAddressLevel(addressLevel);
		csvAddress2.setEstabUprn("808");

		TokeniserResponse mockTokeniserResponse2 = new TokeniserResponse();
		mockTokeniserResponse2.setOrganisationName("");
		mockTokeniserResponse2.setDepartmentName("");
		mockTokeniserResponse2.setSubBuildingName("");
		mockTokeniserResponse2.setBuildingName("BERTH 42 POPLAR DOCK MARINA");
		mockTokeniserResponse2.setBuildingNumber("");
		mockTokeniserResponse2.setPaoStartNumber("42");
		mockTokeniserResponse2.setPaoStartSuffix("");
		mockTokeniserResponse2.setPaoEndNumber("");
		mockTokeniserResponse2.setPaoEndSuffix("");
		mockTokeniserResponse2.setSaoStartNumber("");
		mockTokeniserResponse2.setSaoStartSuffix("");
		mockTokeniserResponse2.setSaoEndNumber("");
		mockTokeniserResponse2.setSaoEndSuffix("");
		mockTokeniserResponse2.setStreetName("BOARDWALK PLACE");
		mockTokeniserResponse2.setLocality("");
		mockTokeniserResponse2.setTownName("LONDON");
		mockTokeniserResponse2.setPostcode("GU16 6DR");
		mockTokeniserResponse2.setPostcodeIn("6DR");
		mockTokeniserResponse2.setPostcodeOut("GU16");

		Dispatcher mDispatcher = new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				if (request.getPath().contains("GU166DG")) {
					try {
						return new MockResponse().setBody(new ObjectMapper().writeValueAsString(mockTokeniserResponse))
								.addHeader("Content-Type", "application/json");
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				}
				if (request.getPath().contains("GU166DR")) {
					try {
						return new MockResponse().setBody(new ObjectMapper().writeValueAsString(mockTokeniserResponse2))
								.addHeader("Content-Type", "application/json");
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				}
				return new MockResponse().setResponseCode(404);
			}
		};

		mockBackEnd.setDispatcher(mDispatcher);

		Flux<Address> addresses = addressService.createAuxAddressesFromCsv(Arrays
				.asList(new ValidatedAddress<AuxAddress>(csvAddress), new ValidatedAddress<AuxAddress>(csvAddress2)));

		Set<Address> expectedAddresses = new HashSet<>(Arrays.asList(address4, address6));

		StepVerifier.create(addresses).recordWith(HashSet::new).thenConsumeWhile(x -> true)
				.consumeRecordedWith(actualAddresses -> {
					assertEquals(2, actualAddresses.size());
					assertEquals(expectedAddresses, actualAddresses);
				}).verifyComplete();

		// Check ES
		List<String> ids = Arrays.asList("999", "1234567891013");

		StepVerifier.create(repository.findAllById(ids)).recordWith(HashSet::new).thenConsumeWhile(x -> true)
				.consumeRecordedWith(actualAddresses -> {
					assertEquals(2, actualAddresses.size());

					actualAddresses.forEach(address -> {
						assertTrue(ids.contains(String.valueOf(address.getUprn())));
					});
				}).verifyComplete();
	}

	@Test
	@Order(value = 4)
	public void testCreateAddressesFromMsg() throws Exception {

		InputAddress pubSubAddress = new InputAddress();
		pubSubAddress.setUprn("9998");
		pubSubAddress.setRegion("E");
		pubSubAddress.setAddressLine1("Berth 3");
		pubSubAddress.setAddressLine2("Abbotts Wharf Moorings");
		pubSubAddress.setAddressLine3("89 Stainsby Road");
		pubSubAddress.setTownName("London");
		pubSubAddress.setPostcode("GU166DR");
		pubSubAddress.setEstabType(censusEstabType);
		pubSubAddress.setAddressType(censusAddressType);
		pubSubAddress.setAddressLevel(addressLevel);
		pubSubAddress.setLatitude(latitude);
		pubSubAddress.setLongitude(longitude);
		pubSubAddress.setEstabUprn(String.valueOf(censusEstabUprn));
		pubSubAddress.setAbpCode(classificationCode);
		pubSubAddress.setOrganisationName(organisationName);

		TokeniserResponse mockTokeniserResponse = new TokeniserResponse();
		mockTokeniserResponse.setOrganisationName("ACME FLOWERS LTD");
		mockTokeniserResponse.setDepartmentName("");
		mockTokeniserResponse.setSubBuildingName("BERTH 3");
		mockTokeniserResponse.setBuildingName("ABBOTTS WHARF MOORINGS");
		mockTokeniserResponse.setBuildingNumber("89");
		mockTokeniserResponse.setPaoStartNumber("89");
		mockTokeniserResponse.setPaoStartSuffix("");
		mockTokeniserResponse.setPaoEndNumber("");
		mockTokeniserResponse.setPaoEndSuffix("");
		mockTokeniserResponse.setSaoStartNumber("3");
		mockTokeniserResponse.setSaoStartSuffix("");
		mockTokeniserResponse.setSaoEndNumber("");
		mockTokeniserResponse.setSaoEndSuffix("");
		mockTokeniserResponse.setStreetName("STAINSBY ROAD");
		mockTokeniserResponse.setLocality("");
		mockTokeniserResponse.setTownName("LONDON");
		mockTokeniserResponse.setPostcode("GU16 6DR");
		mockTokeniserResponse.setPostcodeIn("6DR");
		mockTokeniserResponse.setPostcodeOut("GU16");

		Dispatcher mDispatcher = new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {
				if (request.getPath().contains("GU166DR")) {
					try {
						return new MockResponse().setBody(new ObjectMapper().writeValueAsString(mockTokeniserResponse))
								.addHeader("Content-Type", "application/json");
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				}
				return new MockResponse().setResponseCode(404);
			}
		};

		mockBackEnd.setDispatcher(mDispatcher);

		Mono<Address> insertAddress = addressService.createAddressFromMsg(pubSubAddress);

		StepVerifier.create(insertAddress).assertNext(address -> {
			assertEquals(address5, address);
		}).expectNextCount(0).verifyComplete();

		// ES should have the new record
		StepVerifier.create(repository.findById("9998")).assertNext(address -> {
			assertNotNull(address);
			assertEquals(address.getUprn(), 9998L);
		}).expectNextCount(0).verifyComplete();
	}

	@Test
	@Order(value = 5)
	public void testFindByAddressContaining() {

		// There should only be 1 address
		StepVerifier.create(repository.findByTokensAddressAllContaining("PONTOON")).assertNext(address -> {
			log.info(address.toString());
			assertThat(address.getTokens().getAddressAll(), containsString("PONTOON"));
		}).expectNextCount(0).verifyComplete();
	}

	@Test
	@Order(value = 6)
	public void testFindAddressById() {

		StepVerifier.create(repository.findById("99")).assertNext(address -> {
			assertNotNull(address);
			assertEquals(address.getUprn(), 99L);
		}).expectNextCount(0).verifyComplete();
	}

	@Test
	@Order(value = 7)
	public void testAddressUpdate() {

		Address addressUpdate = address;
		addressUpdate.setClassificationCode(classificationCode2);

		// Should have one record with uprn=99 classification code change from RD03 ->
		// RD06
		// Before update
		StepVerifier.create(repository.findById("99")).assertNext(address -> {
			assertNotNull(address);
			assertEquals(address.getUprn(), 99L);
			assertEquals(address.getClassificationCode(), classificationCode);
		}).expectNextCount(0).verifyComplete();

		// Updated as has same uprn
		StepVerifier.create(addressService.createAddress(addressUpdate)).assertNext(response -> {
			log.info(response.toString());
			assertNotNull(response);
			assertEquals(address.getUprn(), response.getUprn());
			assertEquals(address.getTokens().getAddressAll(), response.getTokens().getAddressAll());
		}).verifyComplete();

		// After update
		StepVerifier.create(repository.findById("99")).assertNext(address -> {
			assertNotNull(address);
			assertEquals(address.getUprn(), 99L);
			assertEquals(address.getClassificationCode(), classificationCode2);
		}).expectNextCount(0).verifyComplete();
	}

	@Test
	@Order(value = 8)
	public void testPubSubMsgGood() throws Exception {

		ObjectMapper objectMapper = new ObjectMapper();
		Message expectedMsg = objectMapper.readValue(new File("src/test/resources/pubsub_good_msg.json"),
				Message.class);

		template.publish("new-address-test", Files.readString(Path.of("src/test/resources/pubsub_good_msg.json")));

		List<AcknowledgeablePubsubMessage> messages = template.pull("new-address-subscription-test", 1, false);
		Message actualMessage = objectMapper.setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
				.readValue(messages.get(0).getPubsubMessage().getData().toByteArray(), Message.class);

		assertEquals(expectedMsg, actualMessage);
	}

	@Test
	@Order(value = 9)
	public void testPubSubMsgBad() throws Exception {

		// Test that a missing mandatory field fails
		ObjectMapper objectMapper = new ObjectMapper();

		template.publish("new-address-test", Files.readString(Path.of("src/test/resources/pubsub_bad_msg.json")));

		List<AcknowledgeablePubsubMessage> messages = template.pull("new-address-subscription-test", 1, false);

		Exception exception = assertThrows(MismatchedInputException.class, () -> {
			objectMapper.setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
					.readValue(messages.get(0).getPubsubMessage().getData().toByteArray(), Message.class);
		});

		String expectedMessage = "Missing required creator property 'uprn'";
		String actualMessage = exception.getMessage();

		assertTrue(actualMessage.contains(expectedMessage));
	}

	@Test
	@Order(value = 10)
	public void testPubSubMsgExtra() throws Exception {

		// Test that extra fields don't cause a problem
		ObjectMapper objectMapper = new ObjectMapper();
		Message expectedMsg = objectMapper.readValue(new File("src/test/resources/pubsub_extra_msg.json"),
				Message.class);

		template.publish("new-address-test", Files.readString(Path.of("src/test/resources/pubsub_extra_msg.json")));

		List<AcknowledgeablePubsubMessage> messages = template.pull("new-address-subscription-test", 1, false);
		Message actualMessage = objectMapper.setDefaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.AS_EMPTY))
				.readValue(messages.get(0).getPubsubMessage().getData().toByteArray(), Message.class);

		assertEquals(expectedMsg, actualMessage);
	}
	
	@Test
	@Order(value = 11)
	public void testCreateFatUnitAddressesFromCsv() {

		Dispatcher mDispatcher = new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {

				if (request.getPath().contains("Birmingham")) {
					try {
						return new MockResponse().setBody(new ObjectMapper().writeValueAsString(mockTokeniserUnitResponse1))
								.addHeader("Content-Type", "application/json");
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				}
				if (request.getPath().contains("Preston")) {
					try {
						return new MockResponse().setBody(new ObjectMapper().writeValueAsString(mockTokeniserUnitResponse2))
								.addHeader("Content-Type", "application/json");
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				}
				return new MockResponse().setResponseCode(404);
			}
		};

		mockBackEnd.setDispatcher(mDispatcher);

		Flux<HybridAddressFat> addresses = addressService.createFatUnitAddressesFromCsv(validatedAddresses);

		List<HybridAddressFat> expectedAddresses = new ArrayList<>(List.of(hybridAddressFat1, hybridAddressFat2));
		expectedAddresses.forEach(address -> log.info(address.toString()));

		StepVerifier.create(addresses).recordWith(ArrayList::new).thenConsumeWhile(x -> true)
				.consumeRecordedWith(actualAddresses -> {
					actualAddresses.forEach(address -> log.info(address.toString()));
					assertEquals(2, actualAddresses.size());
					assertTrue(actualAddresses.containsAll(expectedAddresses));

				}).verifyComplete();

	}

	@Test
	@Order(value = 12)
	public void testCreateSkinnyUnitAddressesFromCsv() {

		Dispatcher mDispatcher = new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest request) {

				if (request.getPath().contains("Birmingham")) {
					try {
						return new MockResponse().setBody(new ObjectMapper().writeValueAsString(mockTokeniserUnitResponse1))
								.addHeader("Content-Type", "application/json");
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				}
				if (request.getPath().contains("Preston")) {
					try {
						return new MockResponse().setBody(new ObjectMapper().writeValueAsString(mockTokeniserUnitResponse2))
								.addHeader("Content-Type", "application/json");
					} catch (JsonProcessingException e) {
						e.printStackTrace();
					}
				}
				return new MockResponse().setResponseCode(404);
			}
		};

		mockBackEnd.setDispatcher(mDispatcher);

		Flux<HybridAddressSkinny> addresses = addressService.createSkinnyUnitAddressesFromCsv(validatedAddresses);

		List<HybridAddressSkinny> expectedAddresses = new ArrayList<>(List.of(hybridAddressSkinny1, hybridAddressSkinny2));
		expectedAddresses.forEach(address -> log.info(address.toString()));

		StepVerifier.create(addresses).recordWith(ArrayList::new).thenConsumeWhile(x -> true)
				.consumeRecordedWith(actualAddresses -> {
					actualAddresses.forEach(address -> log.info(address.toString()));
					assertEquals(2, actualAddresses.size());
					assertTrue(actualAddresses.containsAll(expectedAddresses));

				}).verifyComplete();
	}
	
	@Test
	@Order(value = 13)
	public void testCreateFatUnitAddressesFromES() {

		// Check the Unit Addresses from Test 11 were added to the ES index
		List<HybridAddressFat> expectedAddresses = new ArrayList<>(List.of(hybridAddressFat1, hybridAddressFat2));

		expectedAddresses.stream().map(HybridAddressFat::getUprn).forEach(id -> StepVerifier
				.create(fatRepository.findById(String.valueOf(id))).expectNextCount(1).verifyComplete());

	}

	@Test
	@Order(value = 14)
	public void testCreateSkinnyUnitAddressesFromES() {

		// Check the Unit Addresses from Test 12 were added to the ES index
		List<HybridAddressSkinny> expectedAddresses = new ArrayList<>(List.of(hybridAddressSkinny1, hybridAddressSkinny2));

		expectedAddresses.stream().map(HybridAddressSkinny::getUprn).forEach(id -> StepVerifier
				.create(skinnyRepository.findById(String.valueOf(id))).expectNextCount(1).verifyComplete());

	}
}
