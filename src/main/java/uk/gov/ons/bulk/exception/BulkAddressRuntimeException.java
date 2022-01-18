package uk.gov.ons.bulk.exception;

public class BulkAddressRuntimeException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public BulkAddressRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	public BulkAddressRuntimeException(Throwable cause) {
		super(cause);
	}

	public BulkAddressRuntimeException(String message) {
		super(message);
	}	
}
