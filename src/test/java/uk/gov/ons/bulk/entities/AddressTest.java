//package uk.gov.ons.bulk.entities;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//import org.junit.jupiter.api.Test;
//import org.springframework.data.geo.Point;
//
//import uk.gov.ons.bulk.util.BulkAddressConstants.CountryCode;
//
//import java.util.List;
//
//class AddressTest {
//
//	private String addressAll = "ACME FLOWERS LTD FLAT C CHESTERFIELD LODGE 78 BEULAH HILL LONDON SE19 3EX";
//	private String organisationName = "ACME FLOWERS LTD";
//	private Short buildingNumber = Short.parseShort("78");
//	private String buildingName = "CHESTERFIELD LODGE";
//	private String subBuildingName = "FLAT C";
//	private Short paoStartNumber = Short.parseShort("78");
//	private String saoStartSuffix = "C";
//	private String addressLine1 = "ACME FLOWERS LTD";
//	private String addressLine2 = "FLAT C CHESTERFIELD LODGE";
//	private String addressLine3 = "78 BEULAH HILL";
//	private String streetName = "BEULAH HILL";
//	private String townName = "LONDON";
//	private String postcode = "SE19 3EX";
//	private String postcodeIn = "3EX";
//	private String postcodeOut = "SE19";
//	private String addressLevel = "U";
//	private String latitude = "55.55";
//	private String longitude = "-1.23";
//	private Point location = new Point(55.55, -1.23);
//	private Long uprn = 99L;
//	private String classificationCode = "RD03";
//	private String censusAddressType = "HH";
//	private String censusEstabType = "Household";
//	private Long censusEstabUprn = 808L;
//	private CountryCode countryCode = CountryCode.E;
//	
//	private Tokens tokens = new Tokens.TokensBuilder()
//			.organisationName(organisationName)
//			.subBuildingName(subBuildingName)
//			.buildingName(buildingName)
//			.buildingNumber(buildingNumber)
//			.paoStartNumber(paoStartNumber)
//			.saoStartSuffix(saoStartSuffix)
//			.streetName(streetName)
//			.townName(townName)
//			.addressLevel(addressLevel)
//			.uprn(uprn)
//			.latitude(latitude)
//			.longitude(longitude)
//			.addressLine1(addressLine1)
//			.addressLine2(addressLine2)
//			.addressLine3(addressLine3)
//			.postcode(postcode).build();
//
//	String postcodeStreetTown = (
//				postcode + "_" +
//				streetName + "_" +
//				townName )
//			.replace(".","")
//			.replace("'","");
//
//	private Address address = new Address(uprn, postcodeIn, postcodeOut, classificationCode, censusAddressType, censusEstabType, censusEstabUprn, countryCode, postcode, postcodeStreetTown, tokens.getAddressAll(), tokens, List.of(new Address.Lpi()), List.of(new Address.Nisra()));
//		
//	@Test
//	void testAddressCreation() {
//		
//		assertEquals(addressAll, address.getTokens().getAddressAll());
//		assertEquals(location, address.getTokens().getLocation());
//		
//		// Null Point data		
//		Tokens tokensNullLatLng = new Tokens.TokensBuilder()
//				.latitude(null)
//				.longitude(null)
//				.build();
//		
//		Address addressNullLatLng = address;
//		addressNullLatLng.setTokens(tokensNullLatLng);
//		
//		assertEquals(0.0, addressNullLatLng.getTokens().getLocation().getX());
//		assertEquals(0.0, addressNullLatLng.getTokens().getLocation().getY());
//
//		// Non parseable Point data
//		Tokens tokensCharLatLng = new Tokens.TokensBuilder()
//				.latitude("X")
//				.longitude("Y")
//				.build();
//		
//		Address addressCharLatLng = address;
//		addressCharLatLng.setTokens(tokensCharLatLng);
//		
//		assertEquals(0.0, addressCharLatLng.getTokens().getLocation().getX());
//		assertEquals(0.0, addressCharLatLng.getTokens().getLocation().getY());
//	}
//}
