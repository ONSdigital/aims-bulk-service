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
//@Document(indexName = "#{@config.skinnyIndexName}", createIndex = false)
//@TypeAlias("unit-address-skinny")
//@EqualsAndHashCode(callSuper = true)
//public @Data class HybridAddressSkinny extends HybridAddress {
//
//	private final List<LpiSkinny> lpi;
//
//	public HybridAddressSkinny(Long uprn, List<LpiSkinny> lpi, String classificationCode, String censusAddressType, String censusEstabType,
//			String postcode, CountryCode countryCode, String postcodeStreetTown, String postTown) {
//		super(uprn, classificationCode, censusAddressType, censusEstabType, postcode, countryCode, postcodeStreetTown, postTown);
//		this.lpi = lpi;
//	}	
//}