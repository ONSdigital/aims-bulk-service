//package uk.gov.ons.bulk.entities;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.validation.constraints.NotBlank;
//
//import com.opencsv.bean.CsvBindByName;
//
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//@EqualsAndHashCode(callSuper = true)
//public @Data class CSVAddress extends InputAddress {
//
//	@CsvBindByName
//	@NotBlank(message = "OA is mandatory")
//	private String oa;
//	@CsvBindByName
//	@NotBlank(message = "LSOA is mandatory")
//	private String lsoa;
//	@CsvBindByName
//	@NotBlank(message = "MSOA is mandatory")
//	private String msoa;
//	@CsvBindByName
//	@NotBlank(message = "LAD is mandatory")
//	private String lad;
//	
//	@Override
//	public List<String> getRow() {
//		
//		List<String> contents = new ArrayList<String>(super.getRow()); 
//		contents.addAll(List.of( this.oa, this.lsoa, this.msoa, this.lad ));
//
//		return contents;
//	}
//	
//	@Override
//	public List<String> getHeader() {
//		List<String> header = new ArrayList<String>(super.getHeader()); 
//		header.addAll(List.of("OA", "LSOA", "MSOA", "LAD"));
//		return header;
//	}
//}
