package uk.gov.ons.bulk.entities;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.data.annotation.Transient;
import org.springframework.data.geo.Point;

import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
public @Data class Tokens {
	private String organisationName;
	private String departmentName;
	private String subBuildingName;
	private String buildingName;
	private Short buildingNumber;
	private Short paoStartNumber;
	private String paoStartSuffix;
	private Short paoEndNumber;
	private String paoEndSuffix;
	private Short saoStartNumber;
	private String saoStartSuffix;
	private Short saoEndNumber;
	private String saoEndSuffix;
	private String streetName;
	private String locality;
	private String townName;
	private Point location;
	private String addressLevel;
	private Long uprn;
	private String addressAll;
	@Transient
	private String latitude;
	@Transient
	private String longitude;
	private String addressLine1;
	private String addressLine2;
	private String addressLine3;
	@Transient
	private String postcode;

	public Tokens(TokensBuilder builder) {
		this.organisationName = builder.organisationName;
		this.departmentName = builder.departmentName;
		this.subBuildingName = builder.subBuildingName;
		this.buildingName = builder.buildingName;
		this.buildingNumber = builder.buildingNumber;
		this.paoStartNumber = builder.paoStartNumber;
		this.paoStartSuffix = builder.paoStartSuffix;
		this.paoEndNumber = builder.paoEndNumber;
		this.paoEndSuffix = builder.paoEndSuffix;
		this.saoStartNumber = builder.saoStartNumber;
		this.saoStartSuffix = builder.saoStartSuffix;
		this.saoEndNumber = builder.saoEndNumber;
		this.saoEndSuffix = builder.saoEndSuffix;
		this.streetName = builder.streetName;
		this.locality = builder.locality;
		this.townName = builder.townName;
		this.addressLevel = builder.addressLevel;
		this.uprn = builder.uprn;
		this.latitude = builder.latitude;
		this.longitude = builder.longitude;
		this.addressLine1 = builder.addressLine1;
		this.addressLine2 = builder.addressLine2;
		this.addressLine3 = builder.addressLine3;
		this.postcode = builder.postcode;
		this.location = new Point(parseStringToOptionalDouble(builder.latitude).orElse(0.0), parseStringToOptionalDouble(builder.longitude).orElse(0.0));	
		this.addressAll = Stream.of(
				builder.organisationName,
				builder.departmentName, 
				builder.subBuildingName, 
				builder.buildingName,
				buildingNumber != null ? builder.buildingNumber.toString() : "",
				builder.streetName,
				builder.locality, 
				builder.townName, 
				builder.postcode).filter(s -> s != null && !s.isEmpty()).collect(Collectors.joining(" "));
	}

	private static Optional<Double> parseStringToOptionalDouble(String value) {

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

	public static class TokensBuilder {
		
		private String organisationName;
		private String departmentName;
		private String subBuildingName;
		private String buildingName;
		private Short buildingNumber;
		private Short paoStartNumber;
		private String paoStartSuffix;
		private Short paoEndNumber;
		private String paoEndSuffix;
		private Short saoStartNumber;
		private String saoStartSuffix;
		private Short saoEndNumber;
		private String saoEndSuffix;
		private String streetName;
		private String locality;
		private String townName;
		private String addressLevel;
		private Long uprn;
		private String latitude;
		private String longitude;
		private String addressLine1;
		private String addressLine2;
		private String addressLine3;
		private String postcode;
		
		public TokensBuilder organisationName(String organisationName) {
			this.organisationName = organisationName;
			return this;
		}

		public TokensBuilder departmentName(String departmentName) {
			this.departmentName = departmentName;
			return this;
		}

		public TokensBuilder subBuildingName(String subBuildingName) {
			this.subBuildingName = subBuildingName;
			return this;
		}

		public TokensBuilder buildingName(String buildingName) {
			this.buildingName = buildingName;
			return this;
		}

		public TokensBuilder buildingNumber(Short buildingNumber) {
			this.buildingNumber = buildingNumber;
			return this;
		}

		public TokensBuilder paoStartNumber(Short paoStartNumber) {
			this.paoStartNumber = paoStartNumber;
			return this;
		}

		public TokensBuilder paoStartSuffix(String paoStartSuffix) {
			this.paoStartSuffix = paoStartSuffix;
			return this;
		}

		public TokensBuilder paoEndNumber(Short paoEndNumber) {
			this.paoEndNumber = paoEndNumber;
			return this;
		}

		public TokensBuilder paoEndSuffix(String paoEndSuffix) {
			this.paoEndSuffix = paoEndSuffix;
			return this;
		}

		public TokensBuilder saoStartNumber(Short saoStartNumber) {
			this.saoStartNumber = saoStartNumber;
			return this;
		}

		public TokensBuilder saoStartSuffix(String saoStartSuffix) {
			this.saoStartSuffix = saoStartSuffix;
			return this;
		}

		public TokensBuilder saoEndNumber(Short saoEndNumber) {
			this.saoEndNumber = saoEndNumber;
			return this;
		}

		public TokensBuilder saoEndSuffix(String saoEndSuffix) {
			this.saoEndSuffix = saoEndSuffix;
			return this;
		}

		public TokensBuilder streetName(String streetName) {
			this.streetName = streetName;
			return this;
		}

		public TokensBuilder locality(String locality) {
			this.locality = locality;
			return this;
		}

		public TokensBuilder townName(String townName) {
			this.townName = townName;
			return this;
		}

		public TokensBuilder addressLevel(String addressLevel) {
			this.addressLevel = addressLevel;
			return this;
		}

		public TokensBuilder uprn(Long uprn) {
			this.uprn = uprn;
			return this;
		}

		public TokensBuilder latitude(String latitude) {
			this.latitude = latitude;
			return this;
		}

		public TokensBuilder longitude(String longitude) {
			this.longitude = longitude;
			return this;
		}

		public TokensBuilder addressLine1(String addressLine1) {
			this.addressLine1 = addressLine1;
			return this;
		}

		public TokensBuilder addressLine2(String addressLine2) {
			this.addressLine2 = addressLine2;
			return this;
		}

		public TokensBuilder addressLine3(String addressLine3) {
			this.addressLine3 = addressLine3;
			return this;
		}

		public TokensBuilder postcode(String postcode) {
			this.postcode = postcode;
			return this;
		}
		
		public Tokens build() {
            Tokens tokens =  new Tokens(this);
            return tokens;
        }
	}
}
