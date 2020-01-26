package org.atypical.core.exception;

public class EntityExistsException extends Exception {

	private static final long serialVersionUID = -4461830445358974905L;

	public EntityExistsException() {
		super();
	}

	public EntityExistsException(String errorMessage) {
		super(errorMessage);
	}

}
