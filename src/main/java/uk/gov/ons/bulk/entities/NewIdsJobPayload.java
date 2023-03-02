package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
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
	@Size(min = 1, max = 100, message = "Number of matches per input address should be an integer between 1 and 100 (5 is default)")
	private String addressLimit;
	@JsonProperty("quality_match_threshold")
	@Size(min = 0, max = 100, message = "Match quality threshold should be an integer between 0 and 100 (10 is default)")
	private String qualityMatchThreshold;
	@JsonProperty("epoch_number")
	@Value("${aims.current-epoch}")
	//@Epoch(message = "{epoch.val.message}")
	private String epoch;
	@JsonProperty("historical_flag")
	@Value("true")
	@Pattern(regexp = "^true$|^false$", message = "historical must be true or false")
	private String historical;
}
