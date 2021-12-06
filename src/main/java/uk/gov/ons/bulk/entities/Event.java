package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class Event {
	
	private String type;
	private String source;
	private String channel;
	private String dateTime;
	private String transactionId;
}
