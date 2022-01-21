//package uk.gov.ons.bulk.entities;
//
//import org.springframework.data.annotation.TypeAlias;
//import org.springframework.data.elasticsearch.annotations.Document;
//
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//import uk.gov.ons.bulk.util.BulkAddressConstants.CountryCode;
//
//import java.util.List;
//
//@Document(indexName = "#{@config.fatIndexName}", createIndex = false)
//@TypeAlias("unit-address-fat")
//@EqualsAndHashCode(callSuper = true)
//public @Data class HybridAddressFat extends HybridAddress {
//
//	private final List<Lpi> lpi;
//	
//	public HybridAddressFat(Long uprn, List<Lpi> lpi, String classificationCode, String censusAddressType, String censusEstabType,
//							String postcode, CountryCode countryCode, String postcodeStreetTown, String postTown) {
//		super(uprn, classificationCode, censusAddressType, censusEstabType, postcode, countryCode, postcodeStreetTown, postTown);
//		this.lpi = lpi;
//	}
//}
