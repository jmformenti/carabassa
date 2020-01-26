package org.atypical.core.exception;

public class EntityNotFoundException extends Exception {

	private static final long serialVersionUID = -2414857058938303290L;

	public EntityNotFoundException() {
		super();
	}

	public EntityNotFoundException(String errorMessage) {
		super(errorMessage);
	}

}
