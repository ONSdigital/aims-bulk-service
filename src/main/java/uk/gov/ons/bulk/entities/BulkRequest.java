package uk.gov.ons.bulk.entities;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class BulkRequest {
	@Schema(type="string", example="12345ABCDE")
	@NotBlank(message = "id is mandatory")
	private String id; // unique within a batch but otherwise free-format, usually a sequential number
	@Schema(type="string", example="142 Dingle Drive Dimchruch DI1 1NG")
	@NotBlank(message = "address is mandatory")
	private String address; // input full address string

}
