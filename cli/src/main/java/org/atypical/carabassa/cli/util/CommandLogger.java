package org.atypical.carabassa.cli.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommandLogger {

    private static final String SEPARATOR = " : ";

    private static final Logger logger = LoggerFactory.getLogger(CommandLogger.class);

    public void debug(String message) {
        logger.debug(message);
    }

    public void info(String message) {
        logger.info(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void warn(String message, Exception e) {
        logger.warn(message + SEPARATOR + e.getMessage());
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Exception e) {
        logger.error(message + SEPARATOR + e.getMessage());
    }
}
