package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class BulkRequest {

	private String id; // unique within a batch but otherwise free-format, usually a sequential number
	private String address; // input full address string

}
