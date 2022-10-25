package uk.gov.ons.bulk.exception;

import java.util.ArrayList;
import java.util.List;

import javax.validation.ConstraintViolationException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import lombok.extern.slf4j.Slf4j;

/**
 * Custom Exception Handler for REST argument and constraint violations.
 * Any violations result in a BAD_REQUEST being returned to the user with the 
 * following format:
 * 
 * {
 *   "status": "BAD_REQUEST",
 *   "message": "runBulkRequest.excludenorthernireland: excludenorthernireland must be true or false",
 *   "errors": [
 *     "uk.gov.ons.bulk.controllers.BulkAddressController runBulkRequest.excludenorthernireland: excludenorthernireland must be true or false"
 *   ]
 * }
 * 
 * Violations are also logged
 *
 */
@Slf4j
@ControllerAdvice
public class BulkApiRestExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
		
		List<String> errors = new ArrayList<String>();
		
		ex.getBindingResult().getFieldErrors().forEach(error -> {
			errors.add(String.format("%s: %s", error.getField(), error.getDefaultMessage()));
		});

		ex.getBindingResult().getGlobalErrors().forEach(error -> {
			errors.add(String.format("%s: %s", error.getObjectName(), error.getDefaultMessage()));
		});

		BulkApiError apiError = new BulkApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errors);
		
		// Log the argument validation errors - may be useful
		log.error(apiError.toString());
		
		return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
	}
	
	@Override
	protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
			HttpHeaders headers, HttpStatus status, WebRequest request) {
			
		BulkApiError apiError = new BulkApiError(HttpStatus.BAD_REQUEST, 
				ex.getLocalizedMessage(), String.format("%s parameter is missing", ex.getParameterName()));
		
		// Log the error - may be useful
		log.error(apiError.toString());
		
		return handleExceptionInternal(ex, apiError, headers, apiError.getStatus(), request);
	}

	@ExceptionHandler({ ConstraintViolationException.class })
	public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
		
		List<String> errors = new ArrayList<String>();

		ex.getConstraintViolations().forEach(violation -> {	
			errors.add(String.format("%s %s: %s", violation.getRootBeanClass().getName(), 
					violation.getPropertyPath(), violation.getMessage()));
		});

		BulkApiError apiError = new BulkApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), errors);
		
		// Log the parameter validation errors - may be useful
		log.error(apiError.toString());
		
		return new ResponseEntity<Object>(apiError, new HttpHeaders(), apiError.getStatus());
	}
}
