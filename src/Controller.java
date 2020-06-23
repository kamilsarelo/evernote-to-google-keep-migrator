package com.kamilsarelo.evernotetogooglekeepmigrator;

import java.awt.AWTException;
import java.awt.Image;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

public final class Controller {

	// logger //////////////////////////////////////////////////////////////////////////////////////

	private final static Logger LOGGER = Logger.getLogger(Controller.class.getName());
	static {
		Utils.decorateLogger(LOGGER);
	}

	// lazy loading singleton //////////////////////////////////////////////////////////////////////

	private static volatile Controller instance = null;

	public static Controller getInstance() {
		if (instance == null) {
			synchronized (Controller.class) {
				if (instance == null) {
					instance = new Controller();
				}
			}
		}
		return instance;
	}

	// constants ///////////////////////////////////////////////////////////////////////////////////

	private static final long DRIVER_WAIT_SECONDS = 10;

	private static final Path IMAGE_DIRECTORY_PATH = Paths.get(System.getProperty("user.home"), "com.kamilsarelo.everkeepr");
	static {
		try {
			IMAGE_DIRECTORY_PATH.toFile().mkdirs();
		} catch (final Exception e) {
			LOGGER.severe("Temporary folder for download/upload of photos cannot be created");
			LOGGER.log(Level.SEVERE, e.getMessage(), e);
			System.exit(0);
		}
	}

	private static final String EVERNOTE_STRING_SORTING = "Title (ascending)";

	private static final String KEEP_STRING_TAKE_A_NOTE = "Take a note…";
	private static final String KEEP_STRING_MORE = "More";
	private static final String KEEP_STRING_ADD_LABEL = "Add label";
	private static final String KEEP_STRING_LABEL_NOTE = "Label note";
	private static final String KEEP_STRING_ENTER_LABEL_NAME = "Enter label name";
	private static final String KEEP_STRING_ADD_IMAGE = "Add image";
	private static final String KEEP_STRING_CLOSE = "Close";
	private static final String KEEP_STRING_DELETE_NOTE = "Delete note";

	private static final String KEEP_LABEL_PHOTOS = "image";

	// fields //////////////////////////////////////////////////////////////////////////////////////

	private final CountDownLatch latchLogin = new CountDownLatch(2);

	// constructors ////////////////////////////////////////////////////////////////////////////////

	private Controller() {
		View.getInstance().positionDrivers(
				Model.getInstance().getDriverEvernote(),
				Model.getInstance().getDriverKeep());

		new Thread(() -> awaitLogin()).start();
		new Thread(() -> loginEvernote()).start();
		new Thread(() -> loginKeep()).start();
	}

	// private methods /////////////////////////////////////////////////////////////////////////////

	private void awaitLogin() {
		try {
			latchLogin.await();
			View.getInstance().setButtonsEnabled(true);
		} catch (final InterruptedException e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private void loginEvernote() {
		final WebDriver driverEvernote = Model.getInstance().getDriverEvernote();
		// in Selenium 4: WebDriverWait driverWait = new WebDriverWait(driverKeep, Duration.ofSeconds(10));
		final WebDriverWait driverWait = new WebDriverWait(driverEvernote, DRIVER_WAIT_SECONDS);

		try {
			// https://www.oodlestechnologies.com/blogs/How-To-Navigate-A-URL-Using-Selenium-Web-Driver/
			driverEvernote.get("https://www.evernote.com/Login.action"); // alternative: driver.navigate().to("...

			// https://stackoverflow.com/questions/59993665/selenium-send-keys-doesnt-sent-apostrophe/59994377#59994377
			// ...using Selenium v3.5.3 should solve this issue
			driverWait
					.until(ExpectedConditions.visibilityOfElementLocated(By.id("username")))
					.sendKeys(EvernoteToGoogleKeepMigrator.EVERNOTE_USERNAME);
			driverEvernote
					.findElement(By.id("loginButton"))
					.click();

			driverWait
					.until(ExpectedConditions.visibilityOfElementLocated(By.id("password")))
					.sendKeys(EvernoteToGoogleKeepMigrator.EVERNOTE_PASSWORD);
			driverEvernote
					.findElement(By.id("loginButton"))
					.click();

			driverWait
					.until(ExpectedConditions.presenceOfElementLocated(By.id("gwt-debug-Sidebar-notebooksButton")))
					.click();

			latchLogin.countDown();
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private void loginKeep() {
		final WebDriver driverKeep = Model.getInstance().getDriverKeep();
		final WebDriverWait driverWait = new WebDriverWait(driverKeep, DRIVER_WAIT_SECONDS);

		try {
			// https://www.google.com/search?q=selenium+cannot+login+google
			// https://stackoverflow.com/questions/59514049/unable-to-sign-into-google-with-selenium-automation-because-of-this-browser-or
			// https://en.wikipedia.org/wiki/List_of_most_popular_websites
			// https://www.imdb.com/registration/signin?ref=nv_generic_lgin
			driverKeep.get(
					"https://www.imdb.com/ap/signin?openid.pape.max_auth_age=0&openid.return_to=https%3A%2F%2Fwww.imdb.com%2Fap-signin-handler&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.assoc_handle=imdb_google&openid.mode=checkid_setup&siteState=eyJvcGVuaWQuYXNzb2NfaGFuZGxlIjoiaW1kYl9nb29nbGUiLCJyZWRpcmVjdFRvIjoiaHR0cHM6Ly93d3cuaW1kYi5jb20vP3JlZl89bG9naW4ifQ&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&identityProvider=GOOGLE&relyingParty=IMDbPool");

			driverWait
					.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='email']")))
					.sendKeys(EvernoteToGoogleKeepMigrator.GOOGLE_USERNAME);
			driverKeep
					.findElement(By.id("identifierNext"))
					.click();

			driverWait
					.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password']")))
					.sendKeys(EvernoteToGoogleKeepMigrator.GOOGLE_PASSWORD);
			driverKeep
					.findElement(By.id("passwordNext"))
					.click();

			// https://stackoverflow.com/questions/36590274/selenium-how-to-wait-until-page-is-completely-loaded
			driverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#continue")));

			driverKeep.get("https://keep.google.com/");
			driverWait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("#gb")));

			latchLogin.countDown();
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private Note scrapeNoteFromEvernote() throws Exception {
		final WebDriver driverEvernote = Model.getInstance().getDriverEvernote();

		try {
			final String noteNotebook = driverEvernote.findElement(By.id("gwt-debug-NotebookHeader-name")).getAttribute("innerText");
			if (Utils.isNullOrEmpty(noteNotebook)) {
				throw new IllegalAccessException("no notebook selected in Evernote");
			}

			final WebElement elementSortedBy = driverEvernote.findElement(By.cssSelector("div.SelectorOption.SelectorOption-selected"));
			if (!EVERNOTE_STRING_SORTING.equals(elementSortedBy.getAttribute("innerText"))) {
				throw new IllegalAccessException("wrong sort order set in Evernote"
						+ "\n" + "  expected: " + EVERNOTE_STRING_SORTING
						+ "\n" + "    actual: " + elementSortedBy.getAttribute("innerText"));
			}

			driverEvernote.findElement(By.className("focus-NotesView-Note-selected")); // ensure a note is selected in Evernote

			final String noteTitle = driverEvernote.findElement(By.id("gwt-debug-NoteTitleView-textBox")).getAttribute("value");

			final WebElement elementNoteIframe = driverEvernote.findElement(By.name("RichTextArea-entinymce"));
			driverEvernote.switchTo().frame(elementNoteIframe); // http://www.assertselenium.com/webdriver/handling-iframes-using-webdriver/

			final WebElement elementNote = driverEvernote.findElement(By.id("tinymce"));
			final String noteContent = elementNote.getAttribute("innerText"); // returns text, also "textContent" or "getText()", ".innerHTML" returns HTML
			final String noteContentHtml = elementNote.getAttribute("innerHTML");

			final Set<Path> noteImageFilePaths = new LinkedHashSet<>();
			for (final WebElement imageElement : driverEvernote.findElements(By.tagName("img"))) {
				driverEvernote.manage().timeouts().setScriptTimeout(60, TimeUnit.SECONDS);
				// https://stackoverflow.com/questions/934012/get-image-data-in-javascript
				// https://stackoverflow.com/questions/6813704/how-to-download-an-image-using-selenium-any-version
				final String imageBase64 = (String) ((JavascriptExecutor) driverEvernote).executeAsyncScript("" // https://stackoverflow.com/questions/31208818/selenium-and-asynchronos-javascript-calls
						+ "let selenium = arguments[0];"
						+ "fetch('" + imageElement.getAttribute("src") + "')"
						+ "  .then(response => response.blob())"
						+ "  .then(blob => {"
						+ "    let reader = new FileReader();"
						+ "    reader.onload = function() { selenium(this.result); };"
						+ "    reader.readAsDataURL(blob);"
						+ "  });");

				// Google Keep only accepts the following: gif / jpeg / jpg / png / webp (file extension check) + max 10mb + max 25mpix
				// corresponding mime types:
				// - image/gif => .gif
				// - image/pjpeg | image/jpeg => .jpeg | .jpg
				// - image/png => .png
				// - image/webp => .webp
				final String[] imageBase64Parts = imageBase64.split(",");
				if (imageBase64Parts.length == 2) {
					String imageExtension = null;
					if ("data:image/png;base64".equals(imageBase64Parts[0])) {
						imageExtension = "png";
					} else if (Arrays.stream(new String[] { "data:image/jpeg;base64", "data:image/pjpeg;base64" }).anyMatch(imageBase64Parts[0]::equals)) { // https://stackoverflow.com/questions/8992100/test-if-a-string-contains-any-of-the-strings-from-an-array
						imageExtension = "jpg";
					} else if ("data:image/gif;base64".equals(imageBase64Parts[0])) {
						imageExtension = "gif";
					} else if ("data:image/webp;base64".equals(imageBase64Parts[0])) {
						imageExtension = "webp";
					}
					if (imageExtension != null) {
						final Path imageFilePath = Paths.get(IMAGE_DIRECTORY_PATH.toString(), System.nanoTime() + "." + imageExtension); // nano-time is better than "UUID.randomUUID().toString()" because of sorting
						final byte[] imageBytes = Base64.getDecoder().decode(imageBase64Parts[1].getBytes()); // https://www.baeldung.com/java-base64-image-string
						if (imageBytes.length <= 9e6) {
							Files.write(imageFilePath, imageBytes);
						} else {
							// scale image down to fit Google Keep's 10mb limit
							LOGGER.info("scaling down image '" + imageFilePath + "' for Google Keep...");
							// https://stackoverflow.com/questions/6830478/programatically-reducing-jpeg-file-size
							// https://stackoverflow.com/questions/4202244/resize-jpeg-image-in-java
							// https://stackoverflow.com/questions/4216123/how-to-scale-a-bufferedimage
							// https://stackoverflow.com/questions/13605248/java-converting-image-to-bufferedimage
							final BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
							int newWidth, newHeight;
							if (bufferedImage.getHeight() <= bufferedImage.getWidth()) {
								newHeight = 1024;
								newWidth = (int) (newHeight * ((double) bufferedImage.getWidth() / bufferedImage.getHeight()));
							} else {
								newWidth = 1024;
								newHeight = (int) (newWidth * ((double) bufferedImage.getHeight() / bufferedImage.getWidth()));
							}
							final Image scaledInstance = bufferedImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH); // alternative: Image.SCALE_DEFAULT
							final BufferedImage buffered = new BufferedImage(newWidth, newHeight, bufferedImage.getType());
							buffered.getGraphics().drawImage(scaledInstance, 0, 0, null);
							ImageIO.write(buffered, imageExtension, imageFilePath.toFile());
						}
						noteImageFilePaths.add(imageFilePath);
					}
				}
			}

			return new Note(noteNotebook, noteTitle, noteContent, noteContentHtml, noteImageFilePaths);

		} catch (final Exception e) {
			throw e;

		} finally {
			driverEvernote.switchTo().defaultContent();
		}
	}

	private Note prepareNoteForKeep(final Note noteEvernote) {
		final LinkedList<String> contentLines = new LinkedList<>();

		final Document document = Jsoup.parse(noteEvernote.getContentHtml());
		// http://stackoverflow.com/questions/3607965/how-to-convert-html-text-to-plain-text
		// http://stackoverflow.com/questions/1699313/how-to-remove-html-tag-in-java
		// http://www.rgagnon.com/javadetails/java-0424.html
		// http://stackoverflow.com/questions/5640334/how-do-i-preserve-line-breaks-when-using-jsoup-to-convert-html-to-plain-text
		// https://cindyxiaoxiaoli.wordpress.com/2014/02/05/html-to-plain-text-with-java/
		final Elements olElements = document.select("ol");
		for (int index = 0; index < olElements.size(); index++) {
			final Elements liElements = olElements.get(index).select("li");
			for (int jndex = 0; jndex < liElements.size(); jndex++) {
				final Element liElement = liElements.get(jndex);
				liElement.prepend(jndex + 1 + ". ");
			}
		}
		final Elements ulElements = document.select("ul");
		for (int index = 0; index < ulElements.size(); index++) {
			final Elements liElements = ulElements.get(index).select("li");
			for (int jndex = 0; jndex < liElements.size(); jndex++) {
				final Element liElement = liElements.get(jndex);
				liElement.prepend("* ");
			}
		}

		// accept only text and <br>
		final Whitelist whitelist = new Whitelist().addTags("br");
		for (final String line : document.body().html().split("\\r?\\n")) {
			final String cleanLine = Jsoup.clean(line, whitelist);
			if (!cleanLine.isEmpty()) {
				if (cleanLine.equals("<br>")) {
					contentLines.add("");
				} else {
					contentLines.add(Parser.unescapeEntities(Jsoup.clean(cleanLine, Whitelist.none()), false));
				}
			}
		}

		// remove leading, trailing, and multiple whitespaces => do it at the beginning, since a line can become empty after this step
		for (int index = 0; index < contentLines.size(); index++) {
			contentLines.set(
					index,
					contentLines.get(index)
							// non-breaking spaces (char 160) : http://stackoverflow.com/questions/8501072/string-unicode-remove-char-from-the-string  +  http://stackoverflow.com/questions/1702601/unidentified-whitespace-character-in-java
							.replaceAll("\u00A0", " ")
							// leading and trailing spaces
							.trim()
							// two or more spaces : http://stackoverflow.com/questions/2932392/java-how-to-replace-2-or-more-spaces-with-single-space-in-string-and-delete-lead
							.replaceAll("  +", " "));
		}
		// remove empty lines at the beginning
		for (final Iterator<String> iterator = contentLines.iterator(); iterator.hasNext();) {
			if (iterator.next().isEmpty()) {
				iterator.remove();
			} else {
				break;
			}
		}
		// remove empty lines at the end
		for (final Iterator<String> iterator = contentLines.descendingIterator(); iterator.hasNext();) {
			if (iterator.next().isEmpty()) {
				iterator.remove();
			} else {
				break;
			}
		}
		// remove empty lines in-between
		String previousContentLine = null;
		for (final Iterator<String> iterator = contentLines.iterator(); iterator.hasNext();) {
			final String contentLine = iterator.next();
			if (previousContentLine != null && previousContentLine.isEmpty() && contentLine.isEmpty()) {
				iterator.remove();
				continue;
			}
			previousContentLine = contentLine;
		}

		// miscellaneous
		final StringBuilder evernoteNoteContentBuilder = new StringBuilder();

		for (int index = 0; index < contentLines.size(); index++) {
			final String contentLine = contentLines.get(index); // already trimmed above
			if (!contentLine.isEmpty()) {
				evernoteNoteContentBuilder.append(contentLine);
			}
			// new line
			if (index < contentLines.size() - 1) {
				evernoteNoteContentBuilder.append("\r\n"); // "System.lineSeparator());" doesn't work
			}
		}

		// note for keep
		return new Note(
				noteEvernote.getNotebook(),
				noteEvernote.getTitle().toUpperCase(),
				evernoteNoteContentBuilder.toString(),
				null,
				noteEvernote.getImageFilePaths());
	}

	private void addNoteToKeep(final Note note) throws AWTException, InterruptedException, UnsupportedFlavorException, IOException {
		final WebDriver driverKeep = Model.getInstance().getDriverKeep();

		((JavascriptExecutor) driverKeep).executeScript("alert();"); // focus Google Keep window otherwise CMD+V or ENTER in save dialog doesn't work
		driverKeep.switchTo().alert().accept();
		driverKeep.switchTo().defaultContent();

		keyPressRelease(KeyEvent.VK_ESCAPE); // close potential Chrome search field
		Thread.sleep(250);

		((JavascriptExecutor) driverKeep).executeScript("window.scrollTo(0, 0);");
		final WebElement takeANoteElement = driverKeep.findElement(By.cssSelector("div[aria-label='" + KEEP_STRING_TAKE_A_NOTE + "']"));
		((JavascriptExecutor) driverKeep).executeScript("arguments[0].click()", takeANoteElement);

		final WebElement noteElement = takeANoteElement.findElement(By.xpath("../../..")); // http://stackoverflow.com/questions/8577636/select-parent-element-of-known-element-in-selenium

		final List<WebElement> inputElements = noteElement.findElements(By.cssSelector("div[role='textbox']"));

		final WebDriverWait driverWait = new WebDriverWait(driverKeep, DRIVER_WAIT_SECONDS);
		driverWait
				.until(ExpectedConditions.elementToBeClickable(inputElements.get(0)))
				.click();
		setClipboard(note.getTitle());
		keyPressRelease(KeyEvent.VK_META, KeyEvent.VK_V);
		Thread.sleep(250);

		driverWait
				.until(ExpectedConditions.elementToBeClickable(inputElements.get(1)))
				.click();
		setClipboard(note.getContent());
		keyPressRelease(KeyEvent.VK_META, KeyEvent.VK_V);
		Thread.sleep(250);

		noteElement.findElement(By.cssSelector("div[aria-label='" + KEEP_STRING_MORE + "']")).click();

		driverWait
				.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[contains(text(), '" + KEEP_STRING_ADD_LABEL + "')]")))
				.click();

		final WebElement labelsElement = noteElement.findElement(By.xpath("//div[contains(text(), '" + KEEP_STRING_LABEL_NOTE + "')]/..")); // .. | parent::node() | parent::* | parent::div
		driverWait
				.until(ExpectedConditions.visibilityOf(labelsElement));

		final WebElement labelInputElement = labelsElement.findElement(By.cssSelector("input[aria-label='" + KEEP_STRING_ENTER_LABEL_NAME + "']"));
		labelInputElement.sendKeys(note.getNotebook());
		labelInputElement.sendKeys(Keys.RETURN);
		if (!note.getImageFilePaths().isEmpty()) {
			labelInputElement.clear();
			labelInputElement.sendKeys(KEEP_LABEL_PHOTOS);
			labelInputElement.sendKeys(Keys.RETURN);
		}

		noteElement.click(); // close label editor

		if (!note.getImageFilePaths().isEmpty()) { // upload images
			// https://github.com/seleniumhq/selenium-google-code-issue-archive/issues/1815
			// "The current team position is that the print dialog is out of scope for the project."
			// ...thus let's improvise via java.awt.Robot
			for (final Iterator<Path> iterator = new LinkedList<>(note.getImageFilePaths()).descendingIterator(); iterator.hasNext();) { // http://stackoverflow.com/questions/10741902/java-linkedhashset-backwards-iteration
				final String imageFilePathString = iterator.next().toString();
				LOGGER.info("adding the image '" + imageFilePathString + "' to the note in Google Keep...");

				// trigger image upload
				noteElement.findElement(By.cssSelector("div[aria-label='" + KEEP_STRING_ADD_IMAGE + "']")).click();

				// open dialog to enter image's path
				if (!waitTillPathDialogReady()) {
					// fail: "Open a Go to Folder window" didn't open (Shift-Command-G)
					break;
				}

				// paste image's path
				if (!waitTillPathSetInDialog(imageFilePathString)) {
					// fail: path wasn't pasted from clipboard
					break;
				}

				// confirm image's path
				keyPressRelease(KeyEvent.VK_ENTER);
				Thread.sleep(250);

				boolean isClipboardNull = false;
				for (int i = 0; i < 5; i++) {
					setClipboard(null);
					keyPressRelease(KeyEvent.VK_META, KeyEvent.VK_C);
					for (int c = 0; c < 5; c++) {
						Thread.sleep(250);

						if (isClipboardNull = Utils.isNullOrEmpty(getClipboard())) {
							break;
						}
					}
				}
				if (!isClipboardNull) {
					// fail: "Open a Go to Folder window" didn't close (Shift-Command-G)
					break;
				}

				// confirm image upload
				keyPressRelease(KeyEvent.VK_ENTER);
				Thread.sleep(250);
			}
		}

		// confirm new note immediately / no need to wait till the image is uploaded since that is being done in the background
		noteElement.findElement(By.xpath("//div[contains(text(), '" + KEEP_STRING_CLOSE + "')]")).click();
	}

	private boolean waitTillPathDialogReady() throws AWTException, InterruptedException, UnsupportedFlavorException, IOException {
		boolean isClipboardNotEmpty = false;
		outer: for (int i = 0; i < 5; i++) {
			keyPressRelease(KeyEvent.VK_META, KeyEvent.VK_SHIFT, KeyEvent.VK_G); // http://superuser.com/questions/810161/mac-how-to-enter-the-full-directory-path-in-a-save-as-dialog
			Thread.sleep(250);

			for (int j = 0; j < 5; j++) {
				setClipboard(null);
				keyPressRelease(KeyEvent.VK_META, KeyEvent.VK_C);
				Thread.sleep(250);

				if (isClipboardNotEmpty = !Utils.isNullOrEmpty(getClipboard())) {
					break outer;
				}
			}
		}
		return isClipboardNotEmpty;
	}

	private boolean waitTillPathSetInDialog(final String string) throws AWTException, InterruptedException, UnsupportedFlavorException, IOException {
		boolean isClipboardEqualsPath = false;
		outer: for (int i = 0; i < 5; i++) {
			setClipboard(string);
			keyPressRelease(KeyEvent.VK_META, KeyEvent.VK_V);
			Thread.sleep(200 * i);

			setClipboard(null);
			keyPressRelease(KeyEvent.VK_META, KeyEvent.VK_A);
			keyPressRelease(KeyEvent.VK_META, KeyEvent.VK_C);
			for (int j = 0; j < 5; j++) {
				Thread.sleep(250);

				final String clipboardString = getClipboard();
				if (isClipboardEqualsPath = clipboardString != null && string.equals(clipboardString)) {
					break outer;
				}
			}
		}
		return isClipboardEqualsPath;
	}

	private final void keyPressRelease(final int... keycodes) throws AWTException, InterruptedException {
		final Robot robot = new Robot();
		if (keycodes.length == 1) {
			for (final int keycode : keycodes) {
				robot.keyPress(keycode);
				Thread.sleep(10);
				robot.keyRelease(keycode);
				Thread.sleep(10);
			}
		} else if (keycodes.length > 1) {
			for (final int keycode : keycodes) {
				robot.keyPress(keycode);
				Thread.sleep(10);
			}
			for (final int keycode : keycodes) {
				robot.keyRelease(keycode);
				Thread.sleep(10);
			}
		}
	}

	private void setClipboard(final String string) {
		Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
	}

	private String getClipboard() throws UnsupportedFlavorException, IOException {
		final java.awt.datatransfer.Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		final DataFlavor dataFlavor = DataFlavor.stringFlavor;
		if (clipboard.isDataFlavorAvailable(dataFlavor)) {
			return (String) clipboard.getData(dataFlavor);
		}
		return null;
	}

	// public methods //////////////////////////////////////////////////////////////////////////////

	public void migrateSelectedEvernoteNoteToKeepNote() {
		View.getInstance().setButtonsEnabled(false);
		try {
			LOGGER.info("scraping the selected note in Evernote...");
			final Note noteEvernote = scrapeNoteFromEvernote();

			LOGGER.info("preparing the note '" + noteEvernote.getTitle() + "' for Google Keep...");
			final Note noteKeep = prepareNoteForKeep(noteEvernote);

			LOGGER.info("adding the note in Google Keep...");
			addNoteToKeep(noteKeep);

			LOGGER.info("...successfully completed");
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		View.getInstance().setButtonsEnabled(true);
	}

	public void selectNextEvernoteNoteAndMigrateToKeepNote() {
		View.getInstance().setButtonsEnabled(false);

		final WebDriver driverEvernote = Model.getInstance().getDriverEvernote();
		boolean isSuccess = false;
		try {
			LOGGER.info("selecting next note in Evernote...");

			for (final Iterator<WebElement> iterator = driverEvernote.findElements(By.className("focus-NotesView-Note")).iterator(); iterator.hasNext();) {
				if (iterator.next().getAttribute("class").contains("focus-NotesView-Note-selected")) {
					final WebElement nextNoteSidebarElement = iterator.next();
					final String nextNoteSidebarTitle = nextNoteSidebarElement.findElement(By.className("qa-title")).getAttribute("innerText");

					// new Actions(driverEvernote).moveToElement(nextNoteSidebarElement).build().perform();
					((JavascriptExecutor) driverEvernote).executeScript("arguments[0].scrollIntoView(true);", nextNoteSidebarElement);

					final int sleep = 100;
					for (int i = 0; i < DRIVER_WAIT_SECONDS * 1000 / sleep; i++) { // because WebDriverWait does not work: new WebDriverWait(driverEvernote, DRIVER_WAIT_SECONDS).until(ExpectedConditions.elementToBeClickable(element)).click();
						new Actions(driverEvernote).click(nextNoteSidebarElement).build().perform();
						if (nextNoteSidebarElement.getAttribute("class").contains("focus-NotesView-Note-selected")) {
							break;
						}
						Thread.sleep(sleep);
					}

					for (int i = 0; i < DRIVER_WAIT_SECONDS * 1000 / sleep; i++) { // because waiter doesn't work
						final String nextNoteTitle = driverEvernote.findElement(By.id("gwt-debug-NoteTitleView-textBox")).getAttribute("value");
						if (nextNoteSidebarTitle.equals(nextNoteTitle)) {
							break;
						}
						Thread.sleep(sleep);
					}

					isSuccess = true;
					break;
				}
			}
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}

		if (isSuccess) {
			migrateSelectedEvernoteNoteToKeepNote();
		} else {
			View.getInstance().setButtonsEnabled(true);
		}
	}

	public void deleteAllKeepNotes(final boolean isRecursionRoot) {
		View.getInstance().setButtonsEnabled(false);

		try {
			if (isRecursionRoot) {
				LOGGER.info("deleting all notes in Google Keep...");
			}

			final WebDriver driverKeep = Model.getInstance().getDriverKeep();

			final List<WebElement> moreElements = driverKeep.findElements(By.cssSelector("div[aria-label='" + KEEP_STRING_MORE + "']"));
			// ignore the first and second elements
			moreElements.remove(0);
			moreElements.remove(0);

			// break for recursion
			if (moreElements.size() == 0) {
				return;
			}

			for (final WebElement moreElement : moreElements) {
				// hide all alerts that may block clicking
				final List<WebElement> alertElements = driverKeep.findElements(By.cssSelector("div[role='alert']"));
				for (final WebElement alertElement : alertElements) {
					((JavascriptExecutor) driverKeep).executeScript("arguments[0].style.display='none'", alertElement);

				}
				// scroll to menu
				new Actions(driverKeep).moveToElement(moreElement).build().perform();
				// open menu
				new Actions(driverKeep).click(moreElement).build().perform();
				// delete note
				new WebDriverWait(driverKeep, DRIVER_WAIT_SECONDS)
						.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[contains(text(), '" + KEEP_STRING_DELETE_NOTE + "')]")))
						.click();
				new WebDriverWait(driverKeep, DRIVER_WAIT_SECONDS)
						.until(ExpectedConditions.stalenessOf(moreElement));
			}

			deleteAllKeepNotes(false); // call recursively once more, since there might be notes that have been lazy loaded in the mean time

			if (isRecursionRoot) {
				LOGGER.info("...successfully completed");
			}
		} catch (final Exception e) {
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}

		View.getInstance().setButtonsEnabled(true);
	}

	public void onWindowClosed() {
		Model.getInstance().onWindowClosed();
		System.exit(0);
	}

	// inner classes ///////////////////////////////////////////////////////////////////////////////

	private static final class Note {

		private final String notebook;
		private final String title;
		private final String content;
		private final String contentHtml;
		private final Set<Path> imageFilePaths;

		public Note(
				final String notebook,
				final String title,
				final String content,
				final String contentHtml,
				final Set<Path> imageFilePaths) {

			this.notebook = notebook;
			this.title = title;
			this.content = content;
			this.contentHtml = contentHtml;
			this.imageFilePaths = imageFilePaths;
		}

		public String getNotebook() {
			return notebook;
		}

		public String getTitle() {
			return title;
		}

		public String getContent() {
			return content;
		}

		public String getContentHtml() {
			return contentHtml;
		}

		public Set<Path> getImageFilePaths() {
			return imageFilePaths;
		}

	}

}
