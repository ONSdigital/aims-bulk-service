package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class NewIdsJobMessage {
	private NewIdsJobPayload payload;
	@JsonSetter(nulls = Nulls.SKIP)
	private boolean test = false; 
}
