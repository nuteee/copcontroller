package com.nute.copcontroller.entities;

public class CopControllerException extends Throwable {

	private static final long serialVersionUID = 1L;

	public CopControllerException(String message, Throwable cause) {
		super(message, cause);
	}

	public CopControllerException(Throwable cause) {
		super(cause);
	}

}
