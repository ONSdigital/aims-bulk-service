package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class CollectionCase {
	
	private String id;
	private String caseType;
	private String survey;
	private InputAddress address;
	private boolean handDelivery;
	private boolean skeleton;
	private boolean surveyLaunched;
}
