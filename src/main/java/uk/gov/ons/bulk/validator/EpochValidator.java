package uk.gov.ons.bulk.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Value;

public class EpochValidator implements ConstraintValidator<Epoch, String> {

	public EpochValidator(){}

	@Value("${aims.epochs}")
	private String epochs;
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		
		Pattern pattern = Pattern.compile(String.format("^(%s)$", epochs)); //^(95|94|93|92)$
		Matcher matcher = pattern.matcher(value);
		
		return matcher.matches();
	}
}
