package uk.gov.ons.bulk.entities;

import java.io.IOException;
import java.util.Properties;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import uk.gov.ons.bulk.exception.BulkAddressRuntimeException;
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
	@JsonSetter(nulls = Nulls.SKIP)
	@Min(value = 1, message = "Number of matches per input address should be an integer between 1 and 100 (1 is default)")
	@Max(value = 100, message = "Number of matches per input address should be an integer between 1 and 100 (1 is default)")
	private String addressLimit = getProperty("aims.default-limit");
	@JsonProperty("quality_match_threshold")
	@JsonSetter(nulls = Nulls.SKIP)
	@Min(value = 0, message = "Match quality threshold should be decimal number between 0 and 100 (10 is default)")
	@Max(value = 100, message = "Match quality threshold should be decimal number between 0 and 100 (10 is default)")
	private String qualityMatchThreshold = getProperty("aims.default-threshold");
	@JsonProperty("epoch_number")
	@JsonSetter(nulls = Nulls.SKIP)
	@Epoch(message = "epoch must be one of 99, 97, 95")
	private String epoch = getProperty("aims.current-epoch");
	@JsonProperty("historical_flag")
	@JsonSetter(nulls = Nulls.SKIP)
	@Pattern(regexp = "^true$|^false$", message = "historical must be true or false")
	private String historical = getProperty("aims.default-historical");

	private String getProperty(String property) {
		try {
			Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("defaults.properties"));
		    return properties.getProperty(property);
		}
	    catch (IOException e) {
		    throw new BulkAddressRuntimeException(e);
	    }
	}
}
