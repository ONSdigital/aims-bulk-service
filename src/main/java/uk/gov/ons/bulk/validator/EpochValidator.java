package uk.gov.ons.bulk.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Value;

public class EpochValidator implements ConstraintValidator<Epoch, String> {

	public EpochValidator(){
	}

	@Value("${aims.epochs}")
	public String epochs;

	public void setEpochs(String epochs) {
		this.epochs = epochs;
	}

		@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		Pattern pattern = Pattern.compile(String.format("^(%s)$", epochs)); //^(98|97|96|95|94|93)$
		Matcher matcher = pattern.matcher(value);
		
		return matcher.matches();
	}
}
