package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class BulkRequest {
	
	private String id;
	private String address;
}
