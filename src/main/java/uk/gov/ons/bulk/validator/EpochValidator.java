package uk.gov.ons.bulk.validator;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import lombok.extern.slf4j.Slf4j;
import uk.gov.ons.bulk.exception.BulkAddressRuntimeException;

@Slf4j
public class EpochValidator implements ConstraintValidator<Epoch, String> {

	@Value("${aims.epochs}")
	public String epochs;// = "99|97|95";
	
	@Override
	public void initialize(Epoch epoch) {
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		
//		if (value == null) {
//			value = getProperty("aims.current-epoch");
//		}
		log.debug("Epochs: " + epochs);

		Pattern pattern = Pattern.compile(String.format("^(%s)$", epochs)); //^(99|97|95)$
		Matcher matcher = pattern.matcher(value);
		
		return matcher.matches();
	}
	
//	private String getProperty(String property) {
//		try {
//			Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("defaults.properties"));
//		    return properties.getProperty(property);
//		}
//	    catch (IOException e) {
//		    throw new BulkAddressRuntimeException(e);
//	    }
//	}
}
