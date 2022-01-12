package uk.gov.ons.bulk.controllers;

import static uk.gov.ons.bulk.util.BulkAddressConstants.BAD_AUX_ADDRESS_FILE_NAME;
import static uk.gov.ons.bulk.util.BulkAddressConstants.BAD_UNIT_ADDRESS_FILE_NAME;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValue;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.entities.AuxAddress;
import uk.gov.ons.bulk.entities.UnitAddress;
//import uk.gov.ons.bulk.service.AddressService;
import uk.gov.ons.bulk.util.ValidatedAddress;

@Slf4j
@Controller
public class BulkAddressController {

//	@Autowired
//	private AddressService addressService;

	// BigQuery client object provided by our autoconfiguration.
	@Autowired
	BigQuery bigquery;

	@Value("${aims.gcp.bucket}")
	private String gcsBucket;
	
	@Value("${aims.elasticsearch.cluster.fat-enabled}")
	private boolean fatClusterEnabled;
	
	@Value("${aims.display.limit}")
	private int displayLimit;

	@GetMapping(value = "/")
	@ResponseStatus(HttpStatus.OK)
	public String index(Model model) {
		
		model.addAttribute("fatClusterEnabled", fatClusterEnabled);
		
		return "index";
	}
	
	@GetMapping(value = "/error")
	@ResponseStatus(HttpStatus.OK)
	public String error(Model model) {
		return "error";
	}

	@PostMapping(value = "/bulk")
	public String runBulkRequest(String addressesJson, Model model) {
	//	model.addAttribute("status", true);
		// Create dataset UUID
		// Create results table for UUID
		// Create one cloud task for each address
		// Execute task with backoff / throttling
		// Capture results in BigQuery table
		return "request-id";
	}

	@GetMapping(value = "/test")
	public String runTestRequest(Model model) {
	 //   model.addAttribute("status", false);
		// Create dataset UUID
		// Create results table for UUID
		// Create one cloud task for each address
		// Execute task with backoff / throttling
		// Capture results in BigQuery table
		return "progress";
	}

	@GetMapping(value = "/bulk-progress")
	public String getBulkRequestProgress(String requestId,Model model) {
	//	model.addAttribute("message", "unexpected error");
	//	model.addAttribute("status", false);

		try {
			String query = "SELECT * FROM ons-aims-initial-test.bulk_status.bulkinfo;";
			QueryJobConfiguration queryConfig =
					QueryJobConfiguration.newBuilder(query).build();

			// Run the query using the BigQuery object
			for (FieldValueList row : bigquery.query(queryConfig).iterateAll()) {
				for (FieldValue val : row) {
					System.out.println(val);
				}
			}
			} catch (Exception ex) {
				model.addAttribute("message",
						String.format("An error occurred while processing the CSV file: %s", ex.getMessage()));
				model.addAttribute("status", true);
				return "error";
			}
	//	model.addAttribute("status", true);
		// get count of completed queries for given requestId
		// compare with number of queries requested in order to give progress
		// give ETA ?
		return "progress";
	}

	@GetMapping(value = "/bulk-result")
	public String getBulkResults(String requestId,Model model) {
	//	model.addAttribute("status", false);
		// fetch contents of results table for requestId
		// present contents as JSON response
		return "7 Gate Reach";
	}

//	@PostMapping(value = "/upload-csv-aux-file")
//	public String uploadCSVAuxFile(@RequestParam(name = "file") MultipartFile file, Model model) {
//
//		/*
//		 * TODO: Reactify the web page to stream the results of the add operation.
//		 */
//
//		// validate file
//		if (file.isEmpty()) {
//			model.addAttribute("message", "Select a CSV file to upload and an Index to load to.");
//			model.addAttribute("status", false);
//		} else {
//
//			try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
//
//				CsvToBean<AuxAddress> csvToBean = new CsvToBeanBuilder<AuxAddress>(reader).withType(AuxAddress.class)
//						.withIgnoreLeadingWhiteSpace(true).build();
//
//				List<ValidatedAddress<AuxAddress>> validatedAddresses = csvToBean.parse().stream()
//						.map(address -> new ValidatedAddress<AuxAddress>(address)).collect(Collectors.toList());
//
//				List<ValidatedAddress<AuxAddress>> invalidAddresses = validatedAddresses.stream()
//						.filter(address -> !address.isValid()).collect(Collectors.toList());
//
//				if (invalidAddresses.size() > 0) {
//					model.addAttribute("badAddressCSVPath", String.format("Bad addresss file name: %s. In bucket: %s",
//							addressService.writeBadAddressesCsv(invalidAddresses, BAD_AUX_ADDRESS_FILE_NAME), gcsBucket));
//					model.addAttribute("badAddresses", invalidAddresses.stream().limit(displayLimit).collect(Collectors.toList()));
//					model.addAttribute("badAddressSize", String.format("Total invalid aux addresses: %d", invalidAddresses.size()));
//				}
//
//				List<ValidatedAddress<AuxAddress>> validAddresses = validatedAddresses.stream()
//						.filter(address -> address.isValid()).collect(Collectors.toList());
//
//				if (validAddresses.size() > 0) {
//					model.addAttribute("addresses", validAddresses.stream().limit(displayLimit).collect(Collectors.toList()));
//					model.addAttribute("addressesSize", String.format("Total valid aux addresses: %d", validAddresses.size()));
//
//
//					// Add the good addresses to Elasticsearch
//					addressService.createAuxAddressesFromCsv(validAddresses).doOnNext(output -> {
//						log.debug(String.format("Added: %s", output.toString()));
//
//						/*
//						 * This is very basic at the moment and just returns to the view the addresses
//						 * that were attempted to load into ES. It won't show any that failed. For
//						 * example an illegal lat or long will cause the load to fail from that point.
//						 * Needs very clean input data.
//						 */
//					}).subscribe();
//				}
//
//				model.addAttribute("status", true);
//			} catch (Exception ex) {
//				model.addAttribute("message",
//						String.format("An error occurred while processing the CSV file: %s", ex.getMessage()));
//				model.addAttribute("status", false);
//				return "error";
//			}
//		}
//
//		return "file-upload-status";
//	}

//	@PostMapping(value = "/upload-csv-unit-file")
//	public String uploadCSVUnitFile(@RequestParam("file") MultipartFile file, Model model) {
//
//		/*
//		 * TODO: Reactify the web page to stream the results of the add operation.
//		 */
//		// validate file
//		if (file.isEmpty()) {
//			model.addAttribute("message", "Select a CSV file to upload and an Index to load to.");
//			model.addAttribute("status", false);
//		} else {
//
//			try (Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
//
//				CsvToBean<UnitAddress> csvToBean = new CsvToBeanBuilder<UnitAddress>(reader).withType(UnitAddress.class)
//						.withIgnoreLeadingWhiteSpace(true)
//						.withIgnoreEmptyLine(true)
//						.withSeparator('|').build();
//
//				List<ValidatedAddress<UnitAddress>> validatedAddresses = csvToBean.parse().stream()
//						.map(address -> new ValidatedAddress<UnitAddress>(address)).collect(Collectors.toList());
//
//				List<ValidatedAddress<UnitAddress>> invalidAddresses = validatedAddresses.stream()
//						.filter(address -> !address.isValid()).collect(Collectors.toList());
//
//				if (invalidAddresses.size() > 0) {
//					model.addAttribute("badAddressCSVPath", String.format("Bad addresss file name: %s. In bucket: %s",
//							addressService.writeBadAddressesCsv(invalidAddresses, BAD_UNIT_ADDRESS_FILE_NAME), gcsBucket));
//					model.addAttribute("badAddresses", invalidAddresses.stream().limit(displayLimit).collect(Collectors.toList()));
//					model.addAttribute("badAddressSize", String.format("Total invalid unit addresses: %d", invalidAddresses.size()));
//				}
//
//				List<ValidatedAddress<UnitAddress>> validAddresses = validatedAddresses.stream()
//						.filter(address -> address.isValid()).collect(Collectors.toList());
//
//				if (validAddresses.size() > 0) {
//					model.addAttribute("addresses", validAddresses.stream().limit(displayLimit).collect(Collectors.toList()));
//					model.addAttribute("addressesSize", String.format("Total valid unit addresses: %d", validAddresses.size()));
//
//					/* Add the good addresses to Elasticsearch
//					 * This is very basic at the moment and just returns to the view the addresses
//					 * that were attempted to load into ES. It won't show any that failed. For
//					 * example an illegal lat or long will cause the load to fail from that point.
//					 * Needs very clean input data.
//					 */
//					if (fatClusterEnabled) {
//						addressService.createFatUnitAddressesFromCsv(validAddresses).doOnNext(output -> {
//							log.debug(String.format("Added: %s", output.toString()));
//						}).subscribe();
//					} else {
//						addressService.createSkinnyUnitAddressesFromCsv(validAddresses).doOnNext(output -> {
//							log.debug(String.format("Added: %s", output.toString()));
//						}).subscribe();
//					}
//				}
//
//				model.addAttribute("status", true);
//			} catch (Exception ex) {
//				model.addAttribute("message",
//						String.format("An error occurred while processing the CSV file: %s", ex.getMessage()));
//				model.addAttribute("status", false);
//				return "error";
//			}
//		}
//
//		return "unit-address-upload-status";
//	}
}
