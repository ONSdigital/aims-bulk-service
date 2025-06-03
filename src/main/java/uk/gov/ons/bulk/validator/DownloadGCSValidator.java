package uk.gov.ons.bulk.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadGCSValidator implements ConstraintValidator<DownloadGCS, String> {

  private static final String GCS_REGEX =
    "^gs://results_(\\\\d+)_"           // “results_{one or more digits}_”
  + "(\\\\d{12})"                       // “exactly twelve digits”
  + "/results_\\\\1"                    // “/results_{same digits as group #1}”
  + "\\\\.csv\\\\.gz$";                 // “.csv.gz” at end

  private final Pattern pattern = Pattern.compile(GCS_REGEX);

  @Override
  public void initialize(DownloadGCS constraintAnnotation) {
    // No special initialization needed.
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {

    Matcher matcher = pattern.matcher(value);
    boolean matches = matcher.matches();
    if (!matches) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
        "must be a valid GCS download URL like so:\n" +
        "gs://results_<digits>_<12digits>/results_<same digits>.csv.gz"
      ).addConstraintViolation();
    }
    return matches;
  }
}