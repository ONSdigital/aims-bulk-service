package uk.gov.ons.bulk.entities;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
@NoArgsConstructor
public @Data class IdsErrors {
	@NonNull
	@JsonProperty("ids_job_id")
	private String idsJobId;
	@NonNull
	private String timestamp;
	@NonNull
	private List<String> messages;
}

