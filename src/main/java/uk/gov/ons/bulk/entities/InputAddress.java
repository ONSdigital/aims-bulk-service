//package uk.gov.ons.bulk.entities;
//
//import java.util.List;
//import java.util.stream.Collectors;
//import java.util.stream.Stream;
//
//import javax.validation.constraints.Max;
//import javax.validation.constraints.Min;
//import javax.validation.constraints.NotBlank;
//
//import com.fasterxml.jackson.annotation.JsonCreator;
//import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
//import com.fasterxml.jackson.annotation.JsonProperty;
//import com.opencsv.bean.CsvBindByName;
//
//import lombok.Data;
//import lombok.NoArgsConstructor;
//
//@JsonIgnoreProperties(ignoreUnknown = true)
//@NoArgsConstructor
//public @Data class InputAddress {
//
//	@CsvBindByName(column = "address_line1")
//	@NotBlank(message = "ADDRESS_LINE1 is mandatory")
//	private String addressLine1;
//	@CsvBindByName(column = "address_line2")
//	private String addressLine2;
//	@CsvBindByName(column = "address_line3")
//	private String addressLine3;
//	@CsvBindByName(column = "town_name")
//	@NotBlank(message = "TOWN_NAME is mandatory")
//	private String townName;
//	@CsvBindByName
//	@NotBlank(message = "POSTCODE is mandatory")
//	private String postcode;
//	@CsvBindByName
//	@NotBlank(message = "REGION is mandatory")
//	private String region;
//	@CsvBindByName(column = "address_type")
//	@NotBlank(message = "ADDRESS_TYPE is mandatory")
//	private String addressType;
//	@CsvBindByName(column = "address_level")
//	@NotBlank(message = "ADDRESS_LEVEL is mandatory")
//	private String addressLevel;
//	@CsvBindByName(column = "estab_type")
//	@NotBlank(message = "ESTAB_TYPE is mandatory")
//	private String estabType;
//	@CsvBindByName
//	@Min(value = -90, message = "LATITUDE cannot be less than -90")
//	@Max(value = 90, message = "LATITUDE cannot be greater than 90")
//	@NotBlank(message = "LATITUDE is mandatory")
//	private String latitude;
//	@CsvBindByName
//	@Min(value = -180, message = "LONGITUDE cannot be less than -180")
//	@Max(value = 180, message = "LONGITUDE cannot be greater than 180")
//	@NotBlank(message = "LONGITUDE is mandatory")
//	private String longitude;
//	@CsvBindByName
//	@NotBlank(message = "UPRN is mandatory")
//	private String uprn;
//	@CsvBindByName(column = "abp_code")
//	@NotBlank(message = "ABP_CODE is mandatory")
//	private String abpCode;
//	@CsvBindByName(column = "organisation_name")
//	private String organisationName;
//	@CsvBindByName(column = "estab_uprn")
//	@NotBlank(message = "ESTAB_UPRN is mandatory")
//	private String estabUprn;
//
//	@JsonCreator
//	public InputAddress(@JsonProperty(value = "addressLine1", required = true) String addressLine1,
//			@JsonProperty(value = "addressLine2", required = true) String addressLine2,
//			@JsonProperty(value = "addressLine3", required = true) String addressLine3,
//			@JsonProperty(value = "townName", required = true) String townName,
//			@JsonProperty(value = "postcode", required = true) String postcode,
//			@JsonProperty(value = "region", required = true) String region,
//			@JsonProperty(value = "addressType", required = true) String addressType,
//			@JsonProperty(value = "addressLevel", required = true) String addressLevel,
//			@JsonProperty(value = "estabType", required = true) String estabType,
//			@JsonProperty(value = "latitude", required = true) String latitude,
//			@JsonProperty(value = "longitude", required = true) String longitude,
//			@JsonProperty(value = "uprn", required = true) String uprn,
//			@JsonProperty(value = "abpCode", required = true) String abpCode,
//			@JsonProperty(value = "organisationName", required = true) String organisationName,
//			@JsonProperty(value = "estabUprn", required = true) String estabUprn) {
//		super();
//		this.addressLine1 = addressLine1;
//		this.addressLine2 = addressLine2;
//		this.addressLine3 = addressLine3;
//		this.townName = townName;
//		this.postcode = postcode;
//		this.region = region;
//		this.addressType = addressType;
//		this.addressLevel = addressLevel;
//		this.estabType = estabType;
//		this.latitude = latitude;
//		this.longitude = longitude;
//		this.uprn = uprn;
//		this.abpCode = abpCode;
//		this.organisationName = organisationName;
//		this.estabUprn = estabUprn;
//	}
//
//	/**
//	 * Input fields for CSV or PubSub message that are concatenated and sent to the parser 
//	 * microservice for tokenisation.
//	 * 
//	 * @return concatenated address
//	 */
//	public String getAddressAll() {
//		return Stream.of(addressLine1, addressLine2, addressLine3, townName, postcode)
//				.filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining(" "));
//	}
//
//	/**
//	 * Returns an InputAddress object as a List of Strings. Used when creating a CSV of invalid addresses.
//	 * 
//	 * @return List of InputAddress values
//	 */
//	public List<String> getRow() {
//		return List.of( this.uprn, this.estabUprn, this.addressType, this.estabType, this.addressLevel,
//				this.abpCode, this.organisationName, this.addressLine1, this.addressLine2, this.addressLine3,
//				this.townName, this.postcode, this.latitude, this.longitude, this.region );
//
//	}
//
//	/**
//	 * Returns an a List of Strings representing the header values for creating a CSV of invalid addresses.
//	 * 
//	 * @return List of header values
//	 */
//	public List<String> getHeader() {
//		return List.of("UPRN", "ESTAB_UPRN", "ADDRESS_TYPE", "ESTAB_TYPE", "ADDRESS_LEVEL", "ABP_CODE",
//				"ORGANISATION_NAME", "ADDRESS_LINE1", "ADDRESS_LINE2", "ADDRESS_LINE3", "TOWN_NAME", "POSTCODE",
//				"LATITUDE", "LONGITUDE", "REGION");
//	}
//
//}