package uk.gov.ons.bulk.entities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TokensTest {
	
	private String organisationName	= "ACME FLOWERS LTD";
	private String subBuildingName= "FLAT C";
	private String buildingName = "CHESTERFIELD LODGE";
	private String buildingNumber = "78";
	private String paoStartNumber = "78";
	private String saoStartSuffix = "C";
	private String streetName = "BEULAH HILL";
	private String locality = "LOCKS HEATH";
	private String townName = "LONDON";
	private String postcode = "SE19 3EX";
	
	private String addressLine1 = "ACME FLOWERS LTD";
	private String addressLine2 = "FLAT C CHESTERFIELD LODGE";
	private String addressLine3 = "78 BEULAH HILL";
	
	private String excpectedAddressAll1 = "ACME FLOWERS LTD FLAT C CHESTERFIELD LODGE 78 BEULAH HILL LOCKS HEATH LONDON SE19 3EX";
	private String excpectedAddressAll2 = "ACME FLOWERS LTD FLAT C CHESTERFIELD LODGE 78 BEULAH HILL LOCKS HEATH SE19 3EX";
	private String excpectedAddressAll3 = "ACME FLOWERS LTD FLAT C CHESTERFIELD LODGE 78 BEULAH HILL LONDON SE19 3EX";
	
	@Test
	void testAddressAllTownOnly() {
		
		Tokens tokens = new Tokens.TokensBuilder()
				.organisationName(organisationName)
				.subBuildingName(subBuildingName)
				.buildingName(buildingName)
				.buildingNumber(Short.valueOf(buildingNumber))
				.paoStartNumber(Short.valueOf(paoStartNumber))
				.saoStartSuffix(saoStartSuffix)
				.streetName(streetName)
				.townName(townName)
				.postcode(postcode)
				.addressLine1(addressLine1)
				.addressLine2(addressLine2)
				.addressLine3(addressLine3)
				.build();
		
		assertEquals(excpectedAddressAll3, tokens.getAddressAll());
	}
	
	@Test
	void testAddressAllLocalityOnly() {
		
		Tokens tokens = new Tokens.TokensBuilder()
				.organisationName(organisationName)
				.subBuildingName(subBuildingName)
				.buildingName(buildingName)
				.buildingNumber(Short.valueOf(buildingNumber))
				.paoStartNumber(Short.valueOf(paoStartNumber))
				.saoStartSuffix(saoStartSuffix)
				.streetName(streetName)
				.locality(locality)
				.postcode(postcode)
				.addressLine1(addressLine1)
				.addressLine2(addressLine2)
				.addressLine3(addressLine3)
				.build();
		
		assertEquals(excpectedAddressAll2, tokens.getAddressAll());
	}
	
	@Test
	void testAddressAllLocalityAndTownName() {
				
		Tokens tokens = new Tokens.TokensBuilder()
				.organisationName(organisationName)
				.subBuildingName(subBuildingName)
				.buildingName(buildingName)
				.buildingNumber(Short.valueOf(buildingNumber))
				.paoStartNumber(Short.valueOf(paoStartNumber))
				.saoStartSuffix(saoStartSuffix)
				.streetName(streetName)
				.locality(locality)
				.townName(townName)
				.postcode(postcode)
				.addressLine1(addressLine1)
				.addressLine2(addressLine2)
				.addressLine3(addressLine3)
				.build();
		
		assertEquals(excpectedAddressAll1, tokens.getAddressAll());
	}

}
