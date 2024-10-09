package uk.gov.ons.bulk.validator;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.exception.BulkAddressRuntimeException;
import uk.gov.ons.bulk.util.BulkProperties;

@Slf4j
public class EpochValidator implements ConstraintValidator<Epoch, String> {

	private String epochs;
	private String reversedEpochsList;

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		epochs = BulkProperties.getProperty("aims.epochs");

		log.debug("Epochs: " + epochs);

		Pattern pattern = Pattern.compile(String.format("^(%s)$", epochs)); // e.g., ^(10x|10y|10z)$
		Matcher matcher = pattern.matcher(value);

		boolean matches = matcher.matches();
		if (!matches) {
			// Manipulate into a reversed array with comma split values
			String[] epochArray = epochs.split("\\|");
			List<String> epochList = Arrays.asList(epochArray);
			Collections.reverse(epochList);
			reversedEpochsList = String.join(", ", epochList);

			context.disableDefaultConstraintViolation();
			context.buildConstraintViolationWithTemplate("epoch_number must be one of " + reversedEpochsList)
					.addConstraintViolation();
		}
		return matches;
	}
}