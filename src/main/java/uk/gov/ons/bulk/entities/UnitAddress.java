package uk.gov.ons.bulk.entities;

import com.opencsv.bean.CsvBindByName;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper=true)
public @Data class UnitAddress extends CSVAddress {

	@CsvBindByName(column = "mmstreet_toid")
	private String mmstreetToid;
	@CsvBindByName(column = "mmtopo_toid")
	private String mmtopoToid;
	@CsvBindByName(column = "bng_northing")
	private String bngNorthing;
	@CsvBindByName(column = "bng_easting")
	private String bngEasting;
	
}
