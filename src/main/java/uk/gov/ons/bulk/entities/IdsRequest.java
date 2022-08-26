package uk.gov.ons.bulk.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public @Data class IdsRequest {

	private String id; // unique within a batch but otherwise free-format, usually a sequential number
	private String address; // input full address string
}
