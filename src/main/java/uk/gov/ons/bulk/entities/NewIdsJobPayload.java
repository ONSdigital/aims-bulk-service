package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public @Data class NewIdsJobPayload extends Payload {
	
	@JsonProperty("big_query_dataset")
	private String bigQueryDataset;
	@JsonProperty("big_query_table")
	private String bigQueryTable;
	@JsonProperty("address_limit")
	private String addressLimit;
	@JsonProperty("quality_match_threshold")
	private String qualityMatchThreshold;
}
