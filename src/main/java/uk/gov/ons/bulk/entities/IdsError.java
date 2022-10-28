package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
@NoArgsConstructor
public @Data class IdsError {
	@NonNull
	@JsonProperty("ids_job_id")
	private String idsJobId;
	@NonNull
	private String timestamp;
	@NonNull
	private String message;
}

