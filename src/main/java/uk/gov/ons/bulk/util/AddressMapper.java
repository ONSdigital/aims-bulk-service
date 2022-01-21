//package uk.gov.ons.bulk.util;
//
//import uk.gov.ons.bulk.entities.Address;
//import uk.gov.ons.bulk.entities.InputAddress;
//import uk.gov.ons.bulk.entities.Tokens;
//import uk.gov.ons.bulk.json.TokeniserResponse;
//import uk.gov.ons.bulk.util.BulkAddressConstants.CountryCode;
//
//import java.util.List;
//
//public final class AddressMapper {
//
//	public static Address from(InputAddress inputAddress, TokeniserResponse tokeniserResponse) throws NumberFormatException
//	{
//		Tokens tokens = new Tokens.TokensBuilder()
//				.organisationName(tokeniserResponse.getOrganisationName())
//				.departmentName(tokeniserResponse.getDepartmentName())
//				.subBuildingName(tokeniserResponse.getSubBuildingName())
//				.buildingName(tokeniserResponse.getBuildingName())
//				.buildingNumber(!tokeniserResponse.getBuildingNumber().isEmpty() ? Short.parseShort(tokeniserResponse.getBuildingNumber()) : null)
//				.paoStartNumber(!tokeniserResponse.getPaoStartNumber().isEmpty() ? Short.parseShort(tokeniserResponse.getPaoStartNumber()) : null)
//				.paoStartSuffix(tokeniserResponse.getPaoStartSuffix())
//				.paoEndNumber(!tokeniserResponse.getPaoEndNumber().isEmpty() ? Short.parseShort(tokeniserResponse.getPaoEndNumber()) : null)
//				.paoEndSuffix(tokeniserResponse.getPaoEndSuffix())
//				.saoStartNumber(!tokeniserResponse.getSaoStartNumber().isEmpty() ? Short.parseShort(tokeniserResponse.getSaoStartNumber()) : null)
//				.saoStartSuffix(tokeniserResponse.getSaoStartSuffix())
//				.saoEndNumber(!tokeniserResponse.getSaoEndNumber().isEmpty() ? Short.parseShort(tokeniserResponse.getSaoEndNumber()) : null)
//				.saoEndSuffix(tokeniserResponse.getSaoEndSuffix())
//				.streetName(tokeniserResponse.getStreetName())
//				.locality(tokeniserResponse.getLocality())
//				.townName(tokeniserResponse.getTownName())
//				.postcode(tokeniserResponse.getPostcode())
//				.addressLevel(inputAddress.getAddressLevel())
//				.uprn(!inputAddress.getUprn().isEmpty() ? Long.parseLong(inputAddress.getUprn()): null)
//				.latitude(inputAddress.getLatitude())
//				.longitude(inputAddress.getLongitude())
//				.addressLine1(inputAddress.getAddressLine1())
//				.addressLine2(inputAddress.getAddressLine2())
//				.addressLine3(inputAddress.getAddressLine3())
//				.build();
//		
//		CountryCode countryCode;
//		
//		switch (inputAddress.getRegion().charAt(0)) {
//		case 'E':
//			countryCode = CountryCode.E;
//			break;
//		case 'W':
//			countryCode = CountryCode.W;
//			break;
//		case 'N':
//			countryCode = CountryCode.N;
//			break;
//		case 'S':
//			countryCode = CountryCode.W;
//			break;
//		case 'L':
//			countryCode = CountryCode.L;
//			break;
//		case 'M':
//			countryCode = CountryCode.M;
//			break;
//		case 'J':
//			countryCode = CountryCode.J;
//			break;
//		default:
//			countryCode = null;	
//		}
//
//		String postcodeStreetTown = (
//					tokens.getPostcode() + "_" +
//					tokens.getStreetName() + "_" +
//					(!tokens.getLocality().isBlank() ? tokens.getLocality() : tokens.getTownName()))
//				.replace(".","")
//				.replace("'","");
//
//		Address address = new Address(
//				inputAddress.getUprn() != null && !inputAddress.getUprn().isEmpty() ? Long.parseLong(inputAddress.getUprn()): null, 
//				tokeniserResponse.getPostcodeIn(),
//				tokeniserResponse.getPostcodeOut(),
//				inputAddress.getAbpCode(),
//				inputAddress.getAddressType(),
//				inputAddress.getEstabType(),
//				inputAddress.getEstabUprn() != null && !inputAddress.getEstabUprn().isEmpty() ? Long.parseLong(inputAddress.getEstabUprn()): null,
//				countryCode,
//				tokeniserResponse.getPostcode(),
//				postcodeStreetTown,
//				tokens.getAddressAll(),
//				tokens,
//				List.of(new Address.Lpi()),
//				List.of(new Address.Nisra()));
//		
//		return address;
//	}
//}
