package uk.gov.ons.bulk.entities;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class BulkRequestContainer {
	
	@NotNull(message = "addresses is mandatory")
	@Size(min = 1, message = "addresses cannot be empty")
	private @Valid BulkRequest[] addresses;
}
