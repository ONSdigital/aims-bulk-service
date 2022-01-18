package uk.gov.ons.bulk.exception;

public class BulkAddressException extends Exception {
	private static final long serialVersionUID = 1L;

	public BulkAddressException() {
		super();
	}

	public BulkAddressException(String message, Throwable cause, boolean enableSuppression,
								boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public BulkAddressException(String message, Throwable cause) {
		super(message, cause);
	}

	public BulkAddressException(String message) {
		super(message);
	}

	public BulkAddressException(Throwable cause) {
		super(cause);
	}
}
