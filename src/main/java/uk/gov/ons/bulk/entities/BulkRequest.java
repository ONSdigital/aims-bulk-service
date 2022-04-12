package uk.gov.ons.bulk.entities;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class BulkRequest {

	@NotBlank(message = "id is mandatory")
	private String id; // unique within a batch but otherwise free-format, usually a sequential number
	@NotBlank(message = "address is mandatory")
	private String address; // input full address string

}
