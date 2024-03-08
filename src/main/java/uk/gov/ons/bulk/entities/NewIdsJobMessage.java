package uk.gov.ons.bulk.entities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class NewIdsJobMessage {
	@NotNull(message = "payload cannot be empty")
	@Valid
	private NewIdsJobPayload payload;
	@JsonSetter(nulls = Nulls.SKIP)
	private boolean test = false; 
}
