package uk.gov.ons.bulk.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Value;

public class EpochValidator implements ConstraintValidator<Epoch, String> {

	@Value("${aims.epochs}")
	public String epochs;// = "99|97|95";

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		Pattern pattern = Pattern.compile(String.format("^(%s)$", epochs)); //^(99|97|95)$
		Matcher matcher = pattern.matcher(value);
		
		return matcher.matches();
	}
}
