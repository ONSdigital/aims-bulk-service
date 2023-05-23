package uk.gov.ons.bulk.validator;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import uk.gov.ons.bulk.exception.BulkAddressRuntimeException;

@Slf4j
public class EpochValidator implements ConstraintValidator<Epoch, String> {

	public EpochValidator(){
	}

	@Value("${aims.epochs}")
	public String epochs;

	public void setEpochs(String epochs) {
		this.epochs = epochs;
	}

	public void init() {
		try {
			Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("defaults.properties"));
			this.epochs = properties.getProperty("aims.epochs").replace(", ","|");
			log.info("epochs = " + epochs);
		} catch (IOException e) {
			throw new BulkAddressRuntimeException(e);
		}
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		if (epochs == null) this.init();

		Pattern pattern = Pattern.compile(String.format("^(%s)$", epochs)); //^(99|97|95)$
		Matcher matcher = pattern.matcher(value);
		
		return matcher.matches();
	}
}
