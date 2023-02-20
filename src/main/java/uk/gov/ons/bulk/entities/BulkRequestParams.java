package uk.gov.ons.bulk.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public @Data class BulkRequestParams {

	private	String offset; // Specifies the offset from zero, used for pagination.	Default: 0 Maximum: 250
	private	String limitperaddress; // Specifies the number of addresses to return Default: 10 Maximum: 100
	private	String classificationfilter; //	Classification code filter. Can be pattern match (ZW*), exact match (RD06), multiple exact match (RD02,RD04) or a preset keyword such as residential, commercial, workplace or educational
	private	String rangekm; // Limit results to those within this number of kilometers of point (decimal e.g. 0.1)	Optional
	private	String lat; //	Latitude of point in decimal format (e.g. 50.705948).
	private	String lon; //	Longitude of point in decimal format (e.g. -3.5091076).
	private	String historical; // Include historical addresses Default: True
	private	String matchthreshold; // Minimum confidence score (percentage) for match to be included in results. Default: 5
	private	String verbose; // Include the full address details in the response (including relatives, crossRefs, paf and nag). Default: False
	private	String epoch; // Select a specific AddressBase Epoch to search.
	private	String includeauxiliarysearch; // Search in the auxiliary index, if available Default: false
	private	String excludeengland;
	private	String excludescotland;
	private	String excludewales;
	private	String excludenorthernireland;
	private	String pafdefault; // Choose PAF address over NAG Default: False
	@Setter(AccessLevel.NONE)
	private	String eboost; // Set to 0 to exclude addresses in England Default: 1.0
	@Setter(AccessLevel.NONE)
	private	String nboost; // Set to 0 to exclude addresses in Northern Ireland	Optional Default: 1.0
	@Setter(AccessLevel.NONE)
	private	String sboost; // Set to 0 to exclude addresses in Scotland	Optional Default: 1.0
	@Setter(AccessLevel.NONE)
	private	String wboost; // Set to 0 to exclude addresses in Wales Default: 1.0
	
	public BulkRequestParams(String limitperaddress, String classificationfilter, String historical, String matchthreshold, String verbose,
			String epoch, String excludeengland, String excludescotland, String excludewales,
			String excludenorthernireland, String pafdefault) {
		
		this.limitperaddress = limitperaddress;
		if (classificationfilter != null) this.classificationfilter = classificationfilter;
		this.historical = historical;
		this.matchthreshold = matchthreshold;
		this.verbose = verbose;
		this.epoch = epoch;
		this.excludeengland = excludeengland;
		this.excludescotland = excludescotland;
		this.excludewales = excludewales;
		this.excludenorthernireland = excludenorthernireland;
		if(excludeengland.equals("true")) this.eboost = "0";
		if(excludescotland.equals("true")) this.sboost = "0";
		if(excludewales.equals("true")) this.wboost = "0";
		if(excludenorthernireland.equals("true")) this.nboost = "0";
		this.pafdefault = pafdefault;
	}
}
