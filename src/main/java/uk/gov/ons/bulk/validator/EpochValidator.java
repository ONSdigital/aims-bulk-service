package uk.gov.ons.bulk.validator;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.exception.BulkAddressRuntimeException;

@Slf4j
public class EpochValidator implements ConstraintValidator<Epoch, String> {

	public String epochs;
	
	@Override
	public void initialize(Epoch epoch) {
		try {
			Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("defaults.properties"));
			
			if (epochs == null) {
				epochs = properties.getProperty("aims.epochs");
			}
		} catch (IOException e) {
			throw new BulkAddressRuntimeException(e);
		}
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		log.debug("Epochs: " + epochs);

		Pattern pattern = Pattern.compile(String.format("^(%s)$", epochs)); //^(106|105|104|103|102|101|99|97|95)$
		Matcher matcher = pattern.matcher(value);
		
		return matcher.matches();
	}
}
