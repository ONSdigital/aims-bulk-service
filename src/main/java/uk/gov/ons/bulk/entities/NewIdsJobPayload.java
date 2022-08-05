package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opencsv.bean.CsvBindByName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public @Data class NewIdsJobPayload extends Payload {
	
	@CsvBindByName(column = "big_query_dataset")
	private String bigQueryDataset;
	@CsvBindByName(column = "big_query_table")
	private String bigQueryTable;
	@CsvBindByName(column = "address_limit")
	private String addressLimit;
	@CsvBindByName(column = "quality_match_threshold")
	private String qualityMatchThreshold;
}
