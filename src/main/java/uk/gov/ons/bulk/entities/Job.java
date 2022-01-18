package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class Job {
	
	private String runid;
	private String userid;
	private String status;
	private String totalrecs;
	private String recssofar;
	private String startdate;
	private String enddate;
}
