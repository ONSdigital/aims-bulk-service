package uk.gov.ons.bulk.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import org.springframework.beans.factory.annotation.Value;

public class EpochValidator implements ConstraintValidator<Epoch, String> {

	public EpochValidator(String epochs){
		this.altEpochs = epochs;
	}

	@Value("${aims.epochs}")
	private String epochs;

	private String altEpochs;
	
	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		System.out.println("epochs = " + epochs);
		System.out.println("altEpochs = " + epochs);
		String testEpochs = epochs;
		if (epochs.isEmpty()) testEpochs = altEpochs;

		Pattern pattern = Pattern.compile(String.format("^(%s)$", testEpochs)); //^(98|97|96|95|94|93)$
		Matcher matcher = pattern.matcher(value);
		
		return matcher.matches();
	}
}
