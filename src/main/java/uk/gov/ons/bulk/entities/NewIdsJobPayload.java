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
	@JsonProperty("ids_user_id")
	private String idsUserId;
	@JsonProperty("address_limit")
	private String addressLimit;
	@JsonProperty("quality_match_threshold")
	private String qualityMatchThreshold;
	@JsonProperty("epoch_number")
	private String epoch;
	@JsonProperty("historical_flag")
	private String historical;
}
