package org.atypical.carabassa.cli.util;

import org.slf4j.Logger;

public class CommandLogger {

	private Logger logger;

	public CommandLogger(Logger logger) {
		this.logger = logger;
	}

	public void info(String message) {
		System.out.println(message);
		logger.info(message);
	}

	public void warn(String message) {
		System.err.println(message);
		logger.warn(message);
	}

	public void warn(String message, Exception e) {
		System.err.println(message);
		logger.warn(message, e);
	}

	public void error(String message, Exception e) {
		System.err.println(e.getMessage());
		logger.error(message, e);
	}

}
