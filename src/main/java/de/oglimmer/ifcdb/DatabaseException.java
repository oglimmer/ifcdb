package de.oglimmer.ifcdb;

/**
 * Exception wrapper for the public interface
 * 
 * @author Oli Zimpasser
 * 
 */
@SuppressWarnings("serial")
public class DatabaseException extends Exception {

	public DatabaseException() {
		super();
	}

	public DatabaseException(String message, Throwable cause) {
		super(message, cause);
	}

	public DatabaseException(String message) {
		super(message);
	}

	public DatabaseException(Throwable cause) {
		super(cause);
	}

}
