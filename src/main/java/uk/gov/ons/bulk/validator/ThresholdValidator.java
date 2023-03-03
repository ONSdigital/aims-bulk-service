package uk.gov.ons.bulk.validator;

import org.springframework.beans.factory.annotation.Value;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class ThresholdValidator implements ConstraintValidator<Threshold, String> {

	public ThresholdValidator(){
	}

		@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {

		try {
			Double numValue = Double.parseDouble(value);
			if (numValue >= 0 && numValue <= 100) return true;
			} catch (NumberFormatException e) {
				return false;
			}

		return false;
	}
}
