//package uk.gov.ons.bulk.util;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import javax.validation.ConstraintViolation;
//import javax.validation.Validation;
//import javax.validation.Validator;
//import javax.validation.ValidatorFactory;
//
//import lombok.Data;
//import uk.gov.ons.bulk.entities.InputAddress;
//
//public @Data class ValidatedAddress <T> {
//	
//	private final T address;
//	private final Set<ConstraintViolation<T>> violations;
//	
//	public ValidatedAddress(T address) {
//		super();
//		this.address = address;
//		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
//		Validator validator = factory.getValidator();		
//		this.violations = validator.validate(address);;
//	}
//
//	public List<String> getRow() {		
//		List<String> contents = new ArrayList<String>(((InputAddress) address).getRow());
//		contents.add(String.format("* %s", this.violations.stream().map(x -> x.getMessage()).collect(Collectors.joining(" * "))));
//		return contents;
//	}
//	
//	public List<String> getHeader() {
//		List<String> header = new ArrayList<String>(((InputAddress) address).getHeader()); 
//		header.add("EXCEPTION_REASON");
//		return header;
//	}
//
//	public boolean isValid() {
//		return violations.size() == 0;
//	}
//}
