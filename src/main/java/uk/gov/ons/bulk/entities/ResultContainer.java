package uk.gov.ons.bulk.entities;

import java.util.ArrayList;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class ResultContainer {
	
	private String jobid;
	private String status;
	private ArrayList<Result> results;
}
