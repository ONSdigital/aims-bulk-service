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

@Slf4j
public class EpochValidator implements ConstraintValidator<Epoch, String> {

	private String epochs;
	private String reversedEpochsList;

	@Override
	public void initialize(Epoch epoch) {
		YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
		yamlFactory.setResources(new ClassPathResource("application.yml"));
		Properties properties = yamlFactory.getObject();

		if (properties == null) {
			throw new BulkAddressRuntimeException("Could not load properties from application.yml");
		}

		epochs = properties.getProperty("aims.epochs");
		if (epochs == null) {
			throw new BulkAddressRuntimeException("Property 'aims.epochs' not found in application.yml");
		}
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
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