package uk.gov.ons.bulk.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.opencsv.bean.CsvToBean;
import com.opencsv.bean.CsvToBeanBuilder;

import uk.gov.ons.bulk.entities.AuxAddress;

class ValidatedAddressTest {

	@Test
	void testAuxCSVValidation() {

		File csvFile = new File("src/test/resources/aux-addresses-test.csv");

		try (Reader reader = new FileReader(csvFile)) {

			CsvToBean<AuxAddress> csvToBean = new CsvToBeanBuilder<AuxAddress>(reader).withType(AuxAddress.class)
					.withIgnoreLeadingWhiteSpace(true).build();

			List<ValidatedAddress<AuxAddress>> validatedAddresses = csvToBean.parse().stream()
					.map(address -> new ValidatedAddress<AuxAddress>(address)).collect(Collectors.toList());

			List<ValidatedAddress<AuxAddress>> validAddresses = validatedAddresses.stream()
					.filter(address -> address.isValid()).collect(Collectors.toList());

			List<ValidatedAddress<AuxAddress>> invalidAddresses = validatedAddresses.stream()
					.filter(address -> !address.isValid()).collect(Collectors.toList());

			assertEquals(2, validAddresses.size());
			assertEquals(2, invalidAddresses.size());
			// The correct rows are invalid
			assertEquals(List.of("99", "88"), invalidAddresses.stream().map(address -> address.getAddress().getUprn())
					.collect(Collectors.toList()));
			// The correct rows are valid
			assertEquals(List.of("1234567891011", "1234567891012"), validAddresses.stream()
					.map(address -> address.getAddress().getUprn()).collect(Collectors.toList()));

			// The correct validation messages are linked to the correct rows - single
			// validation error
			assertEquals(Set.of("LONGITUDE cannot be greater than 180"), 
					invalidAddresses.stream()
						.filter(address -> address.getAddress().getUprn().equals("99"))
						.map(violations -> violations.getViolations())
						.flatMap(violations -> violations.stream()
								.map(violation -> violation.getMessage())).collect(Collectors.toSet()));

			// The correct validation messages are linked to the correct rows - multiple
			// validation errors
			assertEquals(Set.of("LATITUDE cannot be less than -90", "FIELDOFFICER_ID is mandatory", 
					"POSTCODE is mandatory", "MSOA is mandatory"),
					invalidAddresses.stream()
						.filter(address -> address.getAddress().getUprn().equals("88"))
						.map(violations -> violations.getViolations())
						.flatMap(violations -> violations.stream()
								.map(violation -> violation.getMessage())).collect(Collectors.toSet()));

		} catch (Exception e) {
			fail(e);
		}

	}

}
