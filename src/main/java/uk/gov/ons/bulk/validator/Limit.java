package uk.gov.ons.bulk.validator;

import uk.gov.ons.bulk.entities.Payload;

import javax.validation.Constraint;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = LimitValidator.class)
@Documented
public @interface Limit {

  String message() default "{Limit.invalid}";
  
  Class<?>[] groups() default { };

  Class<? extends Payload>[] payload() default { };

}
