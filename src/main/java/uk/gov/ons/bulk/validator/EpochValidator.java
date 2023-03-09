package uk.gov.ons.bulk.validator;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.ons.bulk.util.PropertiesLoader;

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
			Properties properties = PropertiesLoader.loadProperties("messages.properties");
			this.epochs = properties.getProperty("aims.epochs").replace(", ","|");
			log.info("epochs = " + epochs);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		if (epochs == null) this.init();

		Pattern pattern = Pattern.compile(String.format("^(%s)$", epochs)); //^(98|97|96|95|94|93)$
		Matcher matcher = pattern.matcher(value);
		
		return matcher.matches();
	}
}
