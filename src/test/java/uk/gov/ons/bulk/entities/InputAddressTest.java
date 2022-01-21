//package uk.gov.ons.bulk.entities;
//
//import static org.junit.jupiter.api.Assertions.assertEquals;
//
//import org.junit.jupiter.api.Test;
//
//class InputAddressTest {
//	
//	private String addressLine1 = "ACME FLOWERS LTD";
//	private String addressLine2 = "FLAT C CHESTERFIELD LODGE";
//	private String addressLine3 = "78 BEULAH HILL";
//	private String townName = "LONDON";
//	private String postcode = "SE19 3EX";
//	private String expectedAddressAll = "ACME FLOWERS LTD FLAT C CHESTERFIELD LODGE 78 BEULAH HILL LONDON SE19 3EX";
//
//	private String addressLine21 = "205b Nash House Old Oak Lane";
//	private String addressLine22 = "";
//	private String addressLine23 = "";
//	private String postcode2 = "GU16 6DG";
//	private String expectedAddressAll2 = "205b Nash House Old Oak Lane LONDON GU16 6DG";
//	
//	@Test
//	void testInputAddressCreation() {
//		
//		InputAddress inputAddress1 = new InputAddress();
//		
//		inputAddress1.setAddressLine1(addressLine1);
//		inputAddress1.setAddressLine2(addressLine2);
//		inputAddress1.setAddressLine3(addressLine3);
//		inputAddress1.setTownName(townName);
//		inputAddress1.setPostcode(postcode);
//		
//		assertEquals(expectedAddressAll, inputAddress1.getAddressAll());
//		
//		InputAddress inputAddress2 = new InputAddress();
//		
//		inputAddress2.setAddressLine1(addressLine21);
//		inputAddress2.setAddressLine2(addressLine22);
//		inputAddress2.setAddressLine3(addressLine23);
//		inputAddress2.setTownName(townName);
//		inputAddress2.setPostcode(postcode2);
//		
//		assertEquals(expectedAddressAll2, inputAddress2.getAddressAll());
//	}
//
//}
