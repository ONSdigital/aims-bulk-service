package uk.gov.ons.bulk.entities;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.geo.Point;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@TypeAlias("lpi")
public @Data class LpiSkinny {

	@Transient
	private String organisationName;
	@Transient
	private String organisation;
	@Transient
	private String departmentName;
	@Transient
	private String subBuildingName;
	@Transient
	private String buildingName;
	@Transient
	private Short buildingNumber;
	@Transient
	private String streetName;
	@Transient
	private String locality;
	@Transient
	private String townName;
	@Field(name = "townName")
	private String townNameUnitAddress;
	@Transient
	private String postcode;
	private String addressBasePostal;
	private String country;
	private Float easting;
	private Float northing;
	private String language;
	private Point location;
	@Transient
	private String latitude;
	@Transient
	private String longitude;
	private Byte lpiLogicalStatus;
	private String mixedNag;
	private String mixedNagStart; // keyword
	private String mixedWelshNag;
	private String mixedWelshNagStart; // keyword
	private String nagAll;
	private Short paoStartNumber;
	private String paoStartSuffix;
	private Long parentUprn;
	private String postcodeLocator;
	private Short saoStartNumber;
	private String secondarySort;
	private String streetDescriptor;
	private Long uprn;
	@Transient
	private String addressLine1;
	@Transient
	private String addressLine2;
	@Transient
	private String addressLine3;

	public LpiSkinny(LpiSkinnyBuilder builder) {
		this.organisation = builder.organisation;
		this.organisationName = builder.organisationName;
		this.departmentName = builder.departmentName;
		this.subBuildingName = builder.subBuildingName;
		this.buildingName = builder.buildingName;
		this.buildingNumber = builder.buildingNumber;
		this.streetName = builder.streetName;
		this.locality = builder.locality;
		this.townName = builder.townName;
		this.townNameUnitAddress = builder.townNameUnitAddress; // The uncapitalised version
		this.postcode = builder.postcode;
		this.addressBasePostal = builder.addressBasePostal;
		this.country = builder.country;
		this.easting = builder.easting;
		this.northing = builder.northing;
		this.language = builder.language;
		this.latitude = builder.latitude;
		this.longitude = builder.longitude;
		this.lpiLogicalStatus = builder.lpiLogicalStatus;
		this.mixedWelshNag = builder.mixedWelshNag;
		this.mixedWelshNagStart = builder.mixedWelshNagStart;
		this.paoStartNumber = builder.paoStartNumber;
		this.paoStartSuffix = builder.paoStartSuffix;
		this.parentUprn = builder.parentUprn;
		this.postcodeLocator = builder.postcodeLocator;
		this.saoStartNumber = builder.saoStartNumber;
		this.secondarySort = builder.secondarySort;
		this.streetDescriptor = builder.streetDescriptor;
		this.uprn = builder.uprn;
		this.addressLine1 = builder.addressLine1;
		this.addressLine2 = builder.addressLine2;
		this.addressLine3 = builder.addressLine3;
		this.nagAll = getNagAll();
		this.mixedNag = getMixedNag();
		this.mixedNagStart = getMixedNagStart();
		this.location = new Point(parseStringToOptionalDouble(builder.latitude).orElse(0.0),
				parseStringToOptionalDouble(builder.longitude).orElse(0.0));
	}

	protected static Optional<Double> parseStringToOptionalDouble(String value) {

		if (value == null || value.isEmpty()) {
			return Optional.empty();
		} else {
			try {
				return Optional.of(Double.valueOf(value));
			} catch (NumberFormatException e) {
				return Optional.empty();
			}
		}
	}
	
	public String getNagAll() {
		return Stream
				.of(this.organisationName, this.departmentName, this.subBuildingName, this.buildingName,
						buildingNumber != null ? this.buildingNumber.toString() : "", this.streetName, this.locality,
						this.townName, this.postcode)
				.filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining(" "));
	}

	public String getMixedNag() {
		return Stream.of(
				Stream.of(this.organisation, this.addressLine1, this.addressLine2, this.addressLine3, this.townNameUnitAddress,
						this.postcode).filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining(", ")),
				this.postcode.replaceAll(" ", "")).collect(Collectors.joining(" "));
	}

	public String getMixedNagStart() {
		if (!this.getMixedNag().isBlank() && this.getMixedNag().length() > 11) {
			return this.getMixedNag().substring(0, 11);
		} else {
			return this.getMixedNag();
		}
	}

	public static class LpiSkinnyBuilder {
		
		private String organisation;
		private String organisationName;
		private String departmentName;
		private String subBuildingName;
		private String buildingName;
		private Short buildingNumber;
		private String streetName;
		private String locality;
		private String townName;
		private String townNameUnitAddress; // The uncapitalised version
		private String postcode;
		private String addressBasePostal;
		private String country;
		private Float easting;
		private Float northing;
		private String language;
		private String latitude;
		private String longitude;
		private Byte lpiLogicalStatus;
		private String mixedWelshNag;
		private String mixedWelshNagStart; // keyword
		private Short paoStartNumber;
		private String paoStartSuffix;
		private Long parentUprn;
		private String postcodeLocator;
		private Short saoStartNumber;
		private String secondarySort;
		private String streetDescriptor;
		private Long uprn;
		private String addressLine1;
		private String addressLine2;
		private String addressLine3;
		
		public LpiSkinnyBuilder organisation(String organisation) {
			this.organisation = organisation;
			return this;
		}
		
		public LpiSkinnyBuilder organisationName(String organisationName) {
			this.organisationName = organisationName;
			return this;
		}
		
		public LpiSkinnyBuilder departmentName(String departmentName) {
			this.departmentName = departmentName;
			return this;
		}
		
		public LpiSkinnyBuilder subBuildingName(String subBuildingName) {
			this.subBuildingName = subBuildingName;
			return this;
		}
		
		public LpiSkinnyBuilder buildingName(String buildingName) {
			this.buildingName = buildingName;
			return this;
		}
		
		public LpiSkinnyBuilder buildingNumber(Short buildingNumber) {
			this.buildingNumber = buildingNumber;
			return this;
		}
		
		public LpiSkinnyBuilder streetName(String streetName) {
			this.streetName = streetName;
			return this;
		}
		
		public LpiSkinnyBuilder locality(String locality) {
			this.locality = locality;
			return this;
		}
		
		public LpiSkinnyBuilder townName(String townName) {
			this.townName = townName;
			return this;
		}
		
		public LpiSkinnyBuilder townNameUnitAddress(String townNameUnitAddress) {
			this.townNameUnitAddress = townNameUnitAddress;
			return this;
		}
		
		public LpiSkinnyBuilder postcode(String postcode) {
			this.postcode = postcode;
			return this;
		}
				
		public LpiSkinnyBuilder addressBasePostal(String addressBasePostal) {
			this.addressBasePostal = addressBasePostal;
			return this;
		}
		
		public LpiSkinnyBuilder country(String country) {
			this.country = country;
			return this;
		}
		
		public LpiSkinnyBuilder easting(Float easting) {
			this.easting = easting;
			return this;
		}
		
		public LpiSkinnyBuilder northing(Float northing) {
			this.northing = northing;
			return this;
		}
		
		public LpiSkinnyBuilder language(String language) {
			this.language = language;
			return this;
		}
		
		public LpiSkinnyBuilder latitude(String latitude) {
			this.latitude = latitude;
			return this;
		}
		
		public LpiSkinnyBuilder longitude(String longitude) {
			this.longitude = longitude;
			return this;
		}
		
		public LpiSkinnyBuilder lpiLogicalStatus(Byte lpiLogicalStatus) {
			this.lpiLogicalStatus = lpiLogicalStatus;
			return this;
		}
		
		public LpiSkinnyBuilder mixedWelshNag(String mixedWelshNag) {
			this.mixedWelshNag = mixedWelshNag;
			return this;
		}
		
		public LpiSkinnyBuilder mixedWelshNagStart(String mixedWelshNagStart) {
			this.mixedWelshNagStart = mixedWelshNagStart;
			return this;
		}
		
		public LpiSkinnyBuilder paoStartNumber(Short paoStartNumber) {
			this.paoStartNumber = paoStartNumber;
			return this;
		}
		
		public LpiSkinnyBuilder paoStartSuffix(String paoStartSuffix) {
			this.paoStartSuffix = paoStartSuffix;
			return this;
		}
		
		public LpiSkinnyBuilder parentUprn(Long parentUprn) {
			this.parentUprn = parentUprn;
			return this;
		}
		
		public LpiSkinnyBuilder postcodeLocator(String postcodeLocator) {
			this.postcodeLocator = postcodeLocator;
			return this;
		}
		
		public LpiSkinnyBuilder saoStartNumber(Short saoStartNumber) {
			this.saoStartNumber = saoStartNumber;
			return this;
		}
		
		public LpiSkinnyBuilder secondarySort(String secondarySort) {
			this.secondarySort = secondarySort;
			return this;
		}
		
		public LpiSkinnyBuilder streetDescriptor(String streetDescriptor) {
			this.streetDescriptor = streetDescriptor;
			return this;
		}
		
		public LpiSkinnyBuilder uprn(Long uprn) {
			this.uprn = uprn;
			return this;
		}
		
		public LpiSkinnyBuilder addressLine1(String addressLine1) {
			this.addressLine1 = addressLine1;
			return this;
		}
		
		public LpiSkinnyBuilder addressLine2(String addressLine2) {
			this.addressLine2 = addressLine2;
			return this;
		}
		
		public LpiSkinnyBuilder addressLine3(String addressLine3) {
			this.addressLine3 = addressLine3;
			return this;
		}
				
		public LpiSkinny build() {
			return new LpiSkinny(this);
		}
	}
}
