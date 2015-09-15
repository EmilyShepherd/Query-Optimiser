package sjdb;

/**
 * Exception class for representing failures to retrieve named relations
 * and attributes from the catalogue.
 * 
 * @author nmg
 */
public class DatabaseException extends Exception {

	/**
	 * 
	 */
	public DatabaseException() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 */
	public DatabaseException(String message) {
		super(message);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param cause
	 */
	public DatabaseException(Throwable cause) {
		super(cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 */
	public DatabaseException(String message, Throwable cause) {
		super(message, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public DatabaseException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
		// TODO Auto-generated constructor stub
	}

}
