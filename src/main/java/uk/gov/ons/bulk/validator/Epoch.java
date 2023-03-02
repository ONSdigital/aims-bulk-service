package uk.gov.ons.bulk.validator;

import uk.gov.ons.bulk.entities.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.validation.Constraint;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EpochValidator.class)
@Documented
public @interface Epoch {

  String message() default "{Epoch.invalid}";
  
  Class<?>[] groups() default { };

  Class<? extends Payload>[] payload() default { };

}
