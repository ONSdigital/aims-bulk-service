//package uk.gov.ons.bulk.entities;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import javax.validation.constraints.NotBlank;
//import javax.validation.constraints.NotEmpty;
//
//import com.opencsv.bean.CsvBindByName;
//
//import lombok.Data;
//import lombok.EqualsAndHashCode;
//
//@EqualsAndHashCode(callSuper = true)
//public @Data class AuxAddress extends CSVAddress {
//
//	@CsvBindByName(column = "htc_willingness")
//	@NotBlank(message = "HTC_WILLINGNESS is mandatory")
//	private String htcWillingness;
//	@CsvBindByName(column = "htc_digital")
//	@NotBlank(message = "HTC_DIGITAL is mandatory")
//	private String htcDigital;
//	@CsvBindByName(column = "treatment_code")
//	@NotBlank(message = "TREATMENT_CODE is mandatory")
//	private String treatmentCode;
//	@CsvBindByName(column = "fieldcoordinator_id")
//	@NotBlank(message = "FIELDCOORDINATOR_ID is mandatory")
//	private String fieldCoordinatorId;
//	@CsvBindByName(column = "fieldofficer_id")
//	@NotBlank(message = "FIELDOFFICER_ID is mandatory")
//	private String fieldOfficerId;
//	@CsvBindByName(column = "ce_expected_capacity")
//	private String ceExpectedCapacity;
//	@CsvBindByName(column = "ce_secure")
//	@NotBlank
//	@NotEmpty(message = "CE_SECURE is mandatory")
//	private String ceSecure;
//	@CsvBindByName(column = "print_batch")
//	private String printBatch;
//
//	@Override
//	public List<String> getRow() {
//		List<String> contents = new ArrayList<String>(super.getRow());
//		contents.addAll(List.of( this.htcWillingness, this.htcDigital, this.treatmentCode, this.fieldCoordinatorId,
//			this.fieldOfficerId, this.ceExpectedCapacity, this.ceSecure, this.printBatch ));
//		return contents;
//	}
//
//	@Override
//	public List<String> getHeader() {
//		List<String> header = new ArrayList<String>(super.getHeader());
//		header.addAll(List.of("HTC_WILLINGNESS", "HTC_DIGITAL", "TREATMENT_CODE",
//				"FIELDCOORDINATOR_ID", "FIELDOFFICER_ID", "CE_EXPECTED_CAPACITY", "CE_SECURE", "PRINT_BATCH"));
//		return header;
//	}
//}
