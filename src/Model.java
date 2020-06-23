package com.kamilsarelo.evernotetogooglekeepmigrator;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public final class Model {

	// logger //////////////////////////////////////////////////////////////////////////////////////

	private final static Logger LOGGER = Logger.getLogger(Model.class.getName());
	static {
		Utils.decorateLogger(LOGGER);
	}

	// lazy loading singleton //////////////////////////////////////////////////////////////////////

	private static volatile Model instance = null;

	public static Model getInstance() {
		if (instance == null) {
			synchronized (Model.class) {
				if (instance == null) {
					instance = new Model();
				}
			}
		}
		return instance;
	}

	// fields //////////////////////////////////////////////////////////////////////////////////////

	private final WebDriver driverEvernote;
	private final WebDriver driverKeep;

	// constructors ////////////////////////////////////////////////////////////////////////////////

	private Model() {
		// Chrome driver
		driverEvernote = new ChromeDriver();
		// Firefox driver
		// driverEvernote = new FirefoxDriver();

		if (!(driverEvernote instanceof JavascriptExecutor)) {
			LOGGER.severe("Evernote driver is not an instance of JavascriptExecutor");
			System.exit(0);
		}
		driverEvernote.manage().deleteAllCookies();

		driverKeep = new ChromeDriver();
		if (!(driverKeep instanceof JavascriptExecutor)) {
			LOGGER.severe("Google Keep driver is not an instance of JavascriptExecutor");
			System.exit(0);
		}
		driverKeep.manage().deleteAllCookies();
	}

	// methods /////////////////////////////////////////////////////////////////////////////////////

	public WebDriver getDriverEvernote() {
		return driverEvernote;
	}

	public WebDriver getDriverKeep() {
		return driverKeep;
	}

	public void onWindowClosed() {
		for (final WebDriver driver : new WebDriver[] { driverEvernote, driverKeep }) {
			if (driver != null) {
				try {
					driver.quit();
				} catch (final Exception e) {
					LOGGER.log(Level.SEVERE, e.getMessage(), e);
				}
			}
		}
	}

}
