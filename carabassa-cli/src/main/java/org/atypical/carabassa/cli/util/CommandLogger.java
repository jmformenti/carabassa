package org.atypical.carabassa.cli.util;

public class CommandLogger {

	public void info(String message) {
		System.out.println(message);
	}

	public void warn(String message) {
		System.err.println(message);
	}

	public void warn(String message, Exception e) {
		System.err.println(message);
	}

	public void error(String message) {
		System.err.println(message);
	}

	public void error(String message, Exception e) {
		System.err.println(e.getMessage());
	}

}
