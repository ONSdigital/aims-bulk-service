package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
public @Data class BulkRequestParams {

	private	String offset; // Specifies the offset from zero, used for pagination.	Default: 0 Maximum: 250
	private	String limit; // Specifies the number of addresses to return Default: 10 Maximum: 100
	private	String classificationfilter; //	Classification code filter. Can be pattern match (ZW*), exact match (RD06), multiple exact match (RD02,RD04) or a preset keyword such as residential, commercial, workplace or educational
	private	String rangekm; // Limit results to those within this number of kilometers of point (decimal e.g. 0.1)	Optional
	private	String lat; //	Latitude of point in decimal format (e.g. 50.705948).
	private	String lon; //	Longitude of point in decimal format (e.g. -3.5091076).
	private	String historical; // Include historical addresses Default: True
	private	String matchthreshold; // Minimum confidence score (percentage) for match to be included in results. Default: 5
	private	String verbose; // Include the full address details in the response (including relatives, crossRefs, paf and nag). Default: False
	private	String epoch; // Select a specific AddressBase Epoch to search.
	private	String includeauxiliarysearch; // Search in the auxiliary index, if available Default: false
	private	String eboost; // Set to 0 to exclude addresses in England Default: 1.0
	private	String nboost; // Set to 0 to exclude addresses in Northern Ireland	Optional Default: 1.0
	private	String sboost; // Set to 0 to exclude addresses in Scotland	Optional Default: 1.0
	private	String wboost; // Set to 0 to exclude addresses in Wales Default: 1.0
}
