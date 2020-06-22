package com.kamilsarelo.evernotetogooglekeepmigrator;

import java.util.Date;
import java.util.logging.ConsoleHandler;
import java.util.logging.ErrorManager;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public final class Utils {

	// constructors ////////////////////////////////////////////////////////////////////////////////

	private Utils() {}

	// methods /////////////////////////////////////////////////////////////////////////////////////

	public static boolean isNullOrEmpty(final String string) {
		return string == null || string.isEmpty();
	}

	public static void decorateLogger(final Logger logger) {
		if (logger != null) {
			logger.setUseParentHandlers(false);

			// https://stackoverflow.com/questions/194165/how-do-i-change-java-logging-console-output-from-std-err-to-std-out/42458416#42458416
			final ConsoleHandler handler = new ConsoleHandler() {

				// https://stackoverflow.com/questions/194165/how-do-i-change-java-logging-console-output-from-std-err-to-std-out/2906222#2906222
				@Override
				public void publish(final LogRecord record) {
					try {
						final String message = getFormatter().format(record);
						if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
							System.err.write(message.getBytes());
						} else {
							System.out.write(message.getBytes());
						}
					} catch (final Exception exception) {
						reportError(null, exception, ErrorManager.FORMAT_FAILURE);
					}
				}

			};
			// https://www.logicbig.com/tutorials/core-java-tutorial/logging/customizing-default-format.html
			handler.setFormatter(new SimpleFormatter() {

				private static final String FORMAT = "[%1$tF %1$tT] [%2$s] [%3$s] %4$s %n";

				@Override
				public synchronized String format(final LogRecord lr) {
					return String.format(
							FORMAT,
							new Date(lr.getMillis()),
							lr.getLevel().getLocalizedName(),
							lr.getLoggerName(),
							lr.getMessage());
				}

			});
			handler.setLevel(Level.ALL);

			logger.addHandler(handler);
			logger.setLevel(Level.ALL);
		}
	}

}
