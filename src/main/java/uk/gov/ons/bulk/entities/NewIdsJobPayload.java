package uk.gov.ons.bulk.entities;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.ons.bulk.validator.Epoch;
import uk.gov.ons.bulk.util.BulkProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public @Data class NewIdsJobPayload extends Payload {

	@JsonProperty("big_query_dataset")
	@NotEmpty(message = "big_query_dataset name must be supplied")
	private String bigQueryDataset;

	@JsonProperty("big_query_table")
	@NotEmpty(message = "big_query_table name must be supplied")
	private String bigQueryTable;

	@JsonProperty("ids_user_id")
	@NotEmpty(message = "ids_user_id must be supplied")
	private String idsUserId;

	@JsonProperty("address_limit")
	@JsonSetter(nulls = Nulls.SKIP)
	@Min(value = 1, message = "address_limit should be an integer between 1 and 100 (1 is default)")
	@Max(value = 100, message = "address_limit should be an integer between 1 and 100 (1 is default)")
	private String addressLimit = BulkProperties.getYamlProperty("aims.default.limit");

	@JsonProperty("quality_match_threshold")
	@JsonSetter(nulls = Nulls.SKIP)
	@Min(value = 0, message = "quality_match_threshold should be decimal number between 0 and 100 (10 is default)")
	@Max(value = 100, message = "quality_match_threshold should be decimal number between 0 and 100 (10 is default)")
	private String qualityMatchThreshold = BulkProperties.getYamlProperty("aims.default.threshold");

	@JsonProperty("epoch_number")
	@JsonSetter(nulls = Nulls.SKIP)
	@Epoch // No message attribute here
	private String epoch = BulkProperties.getYamlProperty("aims.current-epoch");

	@JsonProperty("historical_flag")
	@JsonSetter(nulls = Nulls.SKIP)
	@Pattern(regexp = "^true$|^false$", message = "historical_flag must be true or false")
	private String historical = BulkProperties.getYamlProperty("aims.default.historical");
}
