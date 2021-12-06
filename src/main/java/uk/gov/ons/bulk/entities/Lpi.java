package uk.gov.ons.bulk.entities;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.geo.Point;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@TypeAlias("lpi")
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public @Data class Lpi extends LpiSkinny {

	private String organisation;
	private String locality;
	private Short paoEndNumber;
	private String paoEndSuffix;
	private String paoText;
	private Short saoEndNumber;
	private String saoEndSuffix;
	private String saoStartSuffix;
	
	public Lpi(LpiBuilder builder) {
		this.organisation = builder.organisation;
		this.locality = builder.locality;
		this.paoEndNumber = builder.paoEndNumber;
		this.paoEndSuffix = builder.paoEndSuffix;
		this.paoText = builder.paoText;
		this.saoEndNumber = builder.saoEndNumber;
		this.saoEndSuffix = builder.saoEndSuffix;
		this.saoStartSuffix = builder.saoStartSuffix;
		setTownName(builder.townName);
		setTownNameUnitAddress(builder.townNameUnitAddress);
		setOrganisationName(builder.organisationName);
		setDepartmentName(builder.departmentName);
		setSubBuildingName(builder.subBuildingName);
		setBuildingName(builder.buildingName);
		setBuildingNumber(builder.buildingNumber);
		setStreetName(builder.streetName);
		setPostcode(builder.postcode);
		setAddressBasePostal(builder.addressBasePostal);
		setCountry(builder.country);
		setEasting(builder.easting);
		setNorthing(builder.northing);
		setLanguage(builder.language);
		setLatitude(builder.latitude);
		setLongitude(builder.longitude);
		setLpiLogicalStatus(builder.lpiLogicalStatus);
		setMixedWelshNag(builder.mixedWelshNag);
		setMixedWelshNagStart(builder.mixedWelshNagStart); // keyword
		setPaoStartNumber(builder.paoStartNumber);
		setPaoStartSuffix(builder.paoStartSuffix);
		setParentUprn(builder.parentUprn);
		setPostcodeLocator(builder.postcodeLocator);
		setSaoStartNumber(builder.saoStartNumber);
		setSecondarySort(builder.secondarySort);
		setStreetDescriptor(builder.streetDescriptor);
		setUprn(builder.uprn);
		setAddressLine1(builder.addressLine1);
		setAddressLine2(builder.addressLine2);
		setAddressLine3(builder.addressLine3);
		setMixedNag(this.getMixedNag());
		setMixedNagStart(this.getMixedNagStart()); // keyword
		setNagAll(this.getNagAll());
		setLocation(new Point(super.parseStringToOptionalDouble(builder.latitude).orElse(0.0),
				super.parseStringToOptionalDouble(builder.longitude).orElse(0.0)));
	}
	
	public String getMixedNag() {
		return Stream.of(this.organisation, super.getMixedNag()).filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining(", "));
	}
	
	public String getMixedNagStart() {
		if (!this.getMixedNag().isBlank() && this.getMixedNag().length() > 11) {
			return this.getMixedNag().substring(0, 11);
		} else {
			return this.getMixedNag();
		}
	}
	
	public String getNagAll() {
		return Stream.of(this.organisation.toUpperCase(), super.getDepartmentName(), super.getSubBuildingName(),
				super.getBuildingName(), super.getBuildingNumber() != null ? super.getBuildingNumber().toString() : "",
				super.getStreetName(), super.getLocality(), super.getTownName(), super.getPostcode())
				.filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining(" "));
	}

	public static class LpiBuilder {
		
		private String organisationName;
		private String organisation;
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
		
		private Short paoEndNumber;
		private String paoEndSuffix;
		private String paoText;
		private Short saoEndNumber;
		private String saoEndSuffix;
		private String saoStartSuffix;
		
		public LpiBuilder organisationName(String organisationName) {
			this.organisationName = organisationName;
			return this;
		}
		
		public LpiBuilder organisation(String organisation) {
			this.organisation = organisation;
			return this;
		}
		
		public LpiBuilder departmentName(String departmentName) {
			this.departmentName = departmentName;
			return this;
		}
		
		public LpiBuilder subBuildingName(String subBuildingName) {
			this.subBuildingName = subBuildingName;
			return this;
		}
		
		public LpiBuilder buildingName(String buildingName) {
			this.buildingName = buildingName;
			return this;
		}
		
		public LpiBuilder buildingNumber(Short buildingNumber) {
			this.buildingNumber = buildingNumber;
			return this;
		}
		
		public LpiBuilder streetName(String streetName) {
			this.streetName = streetName;
			return this;
		}
		
		public LpiBuilder locality(String locality) {
			this.locality = locality;
			return this;
		}
		
		public LpiBuilder townName(String townName) {
			this.townName = townName;
			return this;
		}
		
		public LpiBuilder townNameUnitAddress(String townNameUnitAddress) {
			this.townNameUnitAddress = townNameUnitAddress;
			return this;
		}
		
		public LpiBuilder postcode(String postcode) {
			this.postcode = postcode;
			return this;
		}
				
		public LpiBuilder addressBasePostal(String addressBasePostal) {
			this.addressBasePostal = addressBasePostal;
			return this;
		}
		
		public LpiBuilder country(String country) {
			this.country = country;
			return this;
		}
		
		public LpiBuilder easting(Float easting) {
			this.easting = easting;
			return this;
		}
		
		public LpiBuilder northing(Float northing) {
			this.northing = northing;
			return this;
		}
		
		public LpiBuilder language(String language) {
			this.language = language;
			return this;
		}
		
		public LpiBuilder latitude(String latitude) {
			this.latitude = latitude;
			return this;
		}
		
		public LpiBuilder longitude(String longitude) {
			this.longitude = longitude;
			return this;
		}
		
		public LpiBuilder lpiLogicalStatus(Byte lpiLogicalStatus) {
			this.lpiLogicalStatus = lpiLogicalStatus;
			return this;
		}
		
		public LpiBuilder mixedWelshNag(String mixedWelshNag) {
			this.mixedWelshNag = mixedWelshNag;
			return this;
		}
		
		public LpiBuilder mixedWelshNagStart(String mixedWelshNagStart) {
			this.mixedWelshNagStart = mixedWelshNagStart;
			return this;
		}
		
		public LpiBuilder paoStartNumber(Short paoStartNumber) {
			this.paoStartNumber = paoStartNumber;
			return this;
		}
		
		public LpiBuilder paoStartSuffix(String paoStartSuffix) {
			this.paoStartSuffix = paoStartSuffix;
			return this;
		}
		
		public LpiBuilder parentUprn(Long parentUprn) {
			this.parentUprn = parentUprn;
			return this;
		}
		
		public LpiBuilder postcodeLocator(String postcodeLocator) {
			this.postcodeLocator = postcodeLocator;
			return this;
		}
		
		public LpiBuilder saoStartNumber(Short saoStartNumber) {
			this.saoStartNumber = saoStartNumber;
			return this;
		}
		
		public LpiBuilder secondarySort(String secondarySort) {
			this.secondarySort = secondarySort;
			return this;
		}
		
		public LpiBuilder streetDescriptor(String streetDescriptor) {
			this.streetDescriptor = streetDescriptor;
			return this;
		}
		
		public LpiBuilder uprn(Long uprn) {
			this.uprn = uprn;
			return this;
		}
		
		public LpiBuilder addressLine1(String addressLine1) {
			this.addressLine1 = addressLine1;
			return this;
		}
		
		public LpiBuilder addressLine2(String addressLine2) {
			this.addressLine2 = addressLine2;
			return this;
		}
		
		public LpiBuilder addressLine3(String addressLine3) {
			this.addressLine3 = addressLine3;
			return this;
		}
		
		public LpiBuilder paoEndNumber(Short paoEndNumber) {
			this.paoEndNumber = paoEndNumber;
			return this;
		}
		
		public LpiBuilder paoEndSuffix(String paoEndSuffix) {
			this.paoEndSuffix = paoEndSuffix;
			return this;
		}
		
		public LpiBuilder paoText(String paoText) {
			this.paoText = paoText;
			return this;
		}
		
		public LpiBuilder saoEndNumber(Short saoEndNumber) {
			this.saoEndNumber = saoEndNumber;
			return this;
		}
		
		public LpiBuilder saoEndSuffix(String saoEndSuffix) {
			this.saoEndSuffix = saoEndSuffix;
			return this;
		}
		
		public LpiBuilder saoStartSuffix(String saoStartSuffix) {
			this.saoStartSuffix = saoStartSuffix;
			return this;
		}
				
		public Lpi build() {
			return new Lpi(this);
		}
	}
	
}
