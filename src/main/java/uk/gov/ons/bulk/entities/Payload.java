package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opencsv.bean.CsvBindByName;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class Payload {
	
	@CsvBindByName(column = "ids_job_id")
	private String idsJobId;
}
