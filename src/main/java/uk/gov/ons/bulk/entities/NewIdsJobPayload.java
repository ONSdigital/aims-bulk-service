package uk.gov.ons.bulk.entities;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.ons.bulk.validator.Epoch;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public @Data class NewIdsJobPayload extends Payload {
	
	@JsonProperty("big_query_dataset")
	@NotEmpty(message = "big query dataset name must be supplied")
	private String bigQueryDataset;
	@JsonProperty("big_query_table")
	@NotEmpty(message = "big query table name must be supplied")
	private String bigQueryTable;
	@JsonProperty("ids_user_id")
	@NotEmpty(message = "IDS user id must be supplied")
	private String idsUserId;
	@JsonProperty("address_limit")
	@Min(value = 1, message = "Number of matches per input address should be an integer between 1 and 100 (5 is default)")
	@Max(value = 100, message = "Number of matches per input address should be an integer between 1 and 100 (5 is default)")
	private String addressLimit;
	@JsonProperty("quality_match_threshold")
	@Min(value = 0, message = "Match quality threshold should be decimal number between 0 and 100 (10 is default)")
	@Max(value = 100, message = "Match quality threshold should be decimal number between 0 and 100 (10 is default)")
	private String qualityMatchThreshold;
	@JsonProperty("epoch_number")
	@Epoch(message = "{epoch.val.message}")
	private String epoch;
	@JsonProperty("historical_flag")
	@Pattern(regexp = "^true$|^false$", message = "historical must be true or false")
	private String historical;
}
