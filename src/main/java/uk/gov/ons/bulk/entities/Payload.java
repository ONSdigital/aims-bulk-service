package uk.gov.ons.bulk.entities;

import jakarta.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class Payload {
	@NotEmpty(message = "ids_job_id must be supplied")
	@JsonProperty("ids_job_id")
	private String idsJobId;
}
