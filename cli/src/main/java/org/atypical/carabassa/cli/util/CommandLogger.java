package org.atypical.carabassa.cli.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Wrapper around SLF4J for CLI commands. */
public class CommandLogger {

  private static final String SEPARATOR = " : ";

  private static final Logger logger = LoggerFactory.getLogger(CommandLogger.class);

    public static void debug(String message) {
        logger.debug(message);
    }

    public static void info(String message) {
        logger.info(message);
    }

    public static void warn(String message) {
        logger.warn(message);
    }

    public static void warn(String message, Exception e) {
        logger.warn(message + SEPARATOR + e.getMessage());
    }

    public static void error(String message) {
        logger.error(message);
    }

    public static void error(String message, Exception e) {
        logger.error(message + SEPARATOR + e.getMessage());
    }
}
