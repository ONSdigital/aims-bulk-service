package uk.gov.ons.bulk.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Value;

class EpochValidator implements ConstraintValidator<Epoch, String> {

	@Value("${aims.epochs}")
	private String epochs;
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		
		Pattern pattern = Pattern.compile(String.format("^(%s)$", epochs)); //^(89|87|80|39)$
		Matcher matcher = pattern.matcher(value);
		
		return matcher.matches();
	}
}
