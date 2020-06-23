package com.kamilsarelo.evernotetogooglekeepmigrator;

import java.awt.EventQueue;
import java.awt.im.InputContext;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class EvernoteToGoogleKeepMigrator {

	// logger //////////////////////////////////////////////////////////////////////////////////////

	private final static Logger LOGGER = Logger.getLogger(EvernoteToGoogleKeepMigrator.class.getName());
	static {
		Utils.decorateLogger(LOGGER);
	}

	// static initialization ///////////////////////////////////////////////////////////////////////

	static {
		// https://stackoverflow.com/questions/2061194/swing-on-osx-how-to-trap-command-q
		// https://stackoverflow.com/questions/47525497/jframe-do-nothing-on-close-is-not-working-on-command-q
		System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
	}

	static {
		final InputContext context = InputContext.getInstance();
		if (!Utils.isNullOrEmpty(context.getLocale().getDisplayLanguage()) || !Utils.isNullOrEmpty(context.getLocale().getLanguage())) {
			// change the keyboard layout to "US International PC" to make the @ character work properly in the driver
			// https://stackoverflow.com/questions/7186474/convert-at-symbol-to-charsequence
			LOGGER.severe("keyboard layout is not set to \"US International PC\"");
			System.exit(0);
		}
	}

	static {
		// Chrome driver
		final Path path = Paths.get(System.getProperty("user.dir"), "driver", "chromedriver-mac64-83.0.4103.39");
		// Firefox driver
		// final Path path = Paths.get(System.getProperty("user.dir"), "driver", "geckodriver-v0.26.0-macos");

		if (!Files.exists(path)) {
			LOGGER.severe("WebDriver not found");
			System.exit(0);
		}

		final Set<PosixFilePermission> perms = new HashSet<>();
		perms.add(PosixFilePermission.OWNER_EXECUTE);
		try {
			Files.setPosixFilePermissions(path, perms);
		} catch (final IOException e) {
			LOGGER.severe("WebDriver cannot be made executable");
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			System.exit(0);
		}

		// Chrome driver
		System.setProperty("webdriver.chrome.driver", path.toString());
		// Firefox driver
		// System.setProperty("webdriver.gecko.driver", path.toString());
	}

	// constants ///////////////////////////////////////////////////////////////////////////////////

	public static final String EVERNOTE_USERNAME;
	static {
		// set the Evernote username as system property in command line: $ java -Devernote.username="..."
		// or in Eclipse launch configuration's VM arguments: -Devernote.username="..."
		final String property = System.getProperty("evernote.username");
		if (property == null) {
			LOGGER.severe("Evernote username not found in environment properties");
			System.exit(0);
		}
		EVERNOTE_USERNAME = property;
	}

	public static final String EVERNOTE_PASSWORD;
	static {
		// set the Evernote password as system property in command line: $ java -Devernote.password="..."
		// or in Eclipse launch configuration's VM arguments: -Devernote.password="..."
		final String property = System.getProperty("evernote.password");
		if (property == null) {
			LOGGER.severe("Evernote password not found in environment properties");
			System.exit(0);
		}
		EVERNOTE_PASSWORD = property;
	}

	public static final String GOOGLE_USERNAME;
	static {
		// set the Google username as system property in command line: $ java -Dgoogle.username="..."
		// or in Eclipse launch configuration's VM arguments: -Dgoogle.username="..."
		final String property = System.getProperty("google.username");
		if (property == null) {
			LOGGER.severe("Google username not found in environment properties");
			System.exit(0);
		}
		GOOGLE_USERNAME = property;
	}

	public static final String GOOGLE_PASSWORD;
	static {
		// set the Google password as system property in command line: $ java -Dgoogle.password="..."
		// or in Eclipse launch configuration's VM arguments: -Dgoogle.password="..."
		final String property = System.getProperty("google.password");
		if (property == null) {
			LOGGER.severe("Google password not found in environment properties");
			System.exit(0);
		}
		GOOGLE_PASSWORD = property;
	}

	// main method /////////////////////////////////////////////////////////////////////////////////

	public static void main(final String[] args) {
		// initialize View
		EventQueue.invokeLater(View::getInstance);
		// initialize Model
		Model.getInstance();
		// initialize Controller
		Controller.getInstance();
	}

}
