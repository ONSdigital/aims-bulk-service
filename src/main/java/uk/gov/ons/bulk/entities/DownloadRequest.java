package uk.gov.ons.bulk.entities;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class DownloadRequest {
	
	@NotBlank(message = "jobId is mandatory")
	private String jobId;
	@NotBlank(message = "downloadPath is mandatory")
	private String downloadPath;
}
