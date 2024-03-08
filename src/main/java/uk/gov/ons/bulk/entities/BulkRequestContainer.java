package uk.gov.ons.bulk.entities;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class BulkRequestContainer {
	
	@NotNull(message = "addresses is mandatory")
	@Size(min = 1, message = "addresses cannot be empty")
	private @Valid BulkRequest[] addresses;
}
