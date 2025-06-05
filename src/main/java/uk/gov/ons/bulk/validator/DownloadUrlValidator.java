package uk.gov.ons.bulk.validator;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DownloadUrlValidator implements ConstraintValidator<DownloadUrl, String> {

  private static final String UrlRegex =
    "^(?!.*\\.\\.)"                       // no “..” anywhere
  + "https://storage\\.googleapis\\.com/" // begin with https://storage.googleapis.com/
  + "results_[0-9]+_[0-9]+/"              // first folder: results_<digits>_<digits>/
  + "results_[0-9]+\\.csv\\.gz"           // file:      results_<digits>.csv.gz
  + "(?:\\?.*)?$";                        // '?anything…' for url parameters

  private final Pattern pattern = Pattern.compile(UrlRegex);

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {

    Matcher matcher = pattern.matcher(value);
    boolean matches = matcher.matches();
    if (!matches) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate(
        "must be a valid GCS download URL like so:\n" +
        "https://storage.googleapis.com/results_<digits>_<digits>/results_<digits>.csv.gz"
      ).addConstraintViolation();
    }
    return matches;
  }
}