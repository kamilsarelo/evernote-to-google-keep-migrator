# Evernote to Google Keep Migrator

If you ever wanted to migrate your notes from [Evernote](https://evernote.com) to [Google Keep](https://keep.google.com) then this project might help you or give you an idea of how it can be done automatically.

## Motivation

Over years I have collected hundreds of cooking recipes - yes, really tried out, not just collected for the sake of collecting - in Evernote. This worked well in the beginning, but over time I became frustrated with Evernote because of problems like: every update on iOS seemed to slow the app down continuously, or every update also revamped the entire UI completely and worsened the usability, or the search - essential for this amount of data - was slow and unintuitive.

When looking for an alternative on the market I stumbled upon Google Keep. It seemed simple, clean, intuitive, stable, and fast, provided minimum formatting options, sharing, and great search capabilities. It is also available on multiple platforms. Therefore I decided to move my recipe collection from Evernote to Google Keep.

## Idea

Unfortunately, there is no direct and efficient way to migrate Evernote data to Google Keep since Google Keep lacks any import feature. So my idea was very simple: I will automate everything that I would have done manually - copy notes from Evernote and paste them manually one after another in Google Keep. Notes might require some additional adaptations since Evernote uses HTML and Google Keep close-to-plain-text, but this can be automated too.

Since both services provide web interfaces I can use the Selenium framework for automated testing of web applications to accomplish the task. Basically, I need a web browser window with Evernote, another one with Google Keep, and an application that controls both. With the power of DOM in the browser, everything can be selected and manipulated. That is all I need 🙂 

## Dependencies

* macOS (parts of the code are specific for macOS, adaptations are required for other operating systems)
* [WebDriver for Chrome](https://chromedriver.chromium.org/downloads) (alternatively [WebDriver for Firefox](https://github.com/mozilla/geckodriver))
* [Selenium Client & WebDriver language bindings for Java](https://www.selenium.dev/downloads/
)
* [jsoup Java HTML Parser](https://jsoup.org/)

## Application

🚧👷 work in progress 👷🚧

The Java code orchestrating everything is a simple MVC application:
* the [Model](../../raw/master/filter.js) class provides the Evernote and Google Keep WebDrivers
* the [View]() class is the application's minimalistic UI with three buttons and takes care of arranging the WebDriver windows
* the [Controller]() class is responsible of logging in to Evernote and Google Keep as well as migrating notes

Additionally, the [EvernoteToGoogleKeepMigrator]() class gets everything up and running and the [Util]() class provides some shared common methods.

### Starting the application

The application is started from Eclipse. The input source (keyboard layout) has to be set to **"US International PC"** before starting the application, otherwise the **"@"** character might not work properly in the WebDriver.

The application starts two WebDriver instances: one for Evernote, and one for Google Keep respectively. Both WebDriver windows are positioned next to each other just below the application's UI. Then the application logs in to both services. The usernames and passwords for both services are provided via Java system properties using command line arguments, or an Eclipse launch configuration, or any other IDE's run configuration.

Logging in to Evernote is a no-brainer - just a matter of entering the username and password and pressing a button using Selenium. With Google Keep or Google generally, it is a challenging problem.

Some time ago [Google decided to prevent people from using UI automation to access their services](https://stackoverflow.com/questions/59534028/sign-in-to-gmail-account-fails-selenium-automation/59569816#59569816). Google can detect that a "robot" might be trying to log in and they don't want that to happen so you get this error message when using Selenium:
*This browser or app may not be secure. Try using a different browser. If you’re already using a supported browser, you can refresh your screen and try again to sign in.*

However, you can use your Google account to sign in to a different site in the WebDriver and then go to Google Keep. I chose IMDb since it is one of the most popular websites on the internet and they offer to [sign in with Google](https://www.imdb.com/registration/signin?ref=nv_generic_lgin).

Once the application logged in to both services the fun part can begin! *(Note that I additionally require a notebook to be selected and a specific sort order of the notes to be set in Evernote. However, this can easily be removed from the code depending on the requirements.)*

[![Starting the migrator](/assets/startup_800.gif)](/assets/startup_full.gif)

### Migrating a note

A click on the button **"migrate the selected note in Evernote"** will first scrape the selected note in Evernote - see the method **scrapeNoteFromEvernote()** in the **Controller** class. Then the application finds all the images in a note and downloads them via Java-Selenium-JavaScript magic to the local file system. The download must be done in the browser session because of the login to Evernote. If required, the application scales the images down to fit the Google Keep limitations.

In the second step, the application converts HTML-lists to plain-text-lists, removes all HTML tags, removes unnecessary empty lines, etc - see the method **prepareNoteForKeep(Note)** in the **Controller** class. There are no limits to customizations here. For example, I used a special code that was fixing and unifying the units in ingredients or code that was unifying certain words across all recipes.

In the final step, the application focuses the Google Keep window and starts adding a note - see the method **addNoteToKeep(Note)** in the **Controller** class. Besides Selenium, I use here the clipboard and key presses via the good old **java.awt.Robot** heavily for uploading the previously downloaded images to Google Keep.

Noteworthy details:
* Evernote's notebook name is used as a label in Google Keep
* all notes containing images reiceive the label "image"

[![Migrate selected](/assets/migrate_800.gif)](/assets/migrate_full.gif)

### Selecting and migrating the next note

A click on the button **"select the next note in Evernote and migrate it"** will simply select the next note in Evernote and then migrate it to Google Keep like before. A convenience feature that comes in very handy when you want to migrate a huge number of notes semi-automatically and keep control after each migration. I chose semi-automation because it allows me to have control over what is happening and what is being migrated — at least to a certain degree - and I have seen full-automation  fail too often.

[![Migrate next](/assets/migratenext_800.gif)](/assets/migratenext_full.gif)

### Deleting all notes

A click on the button **"delete all notes in Google Keep"** will delete all taken notes in Google Keep. This is useful in case you want to start from scratch again.

[![Delete](/assets/delete_800.gif)](/assets/delete_full.gif)

## Conclusion

**Bottom line: this was fun to code but Google Keep did not live up to my expectations at all** ☹

After migrating a considerable amount of notes I noticed the following shortcomings of Google Keep:
* notes have different heights (my wish: a true grid view with squares and just one image per square)
* no possibility to sort notes by "date created" or "title"
* pinning and unpinning a note changes its order
* no counter of notes
* no possibility to print a note
* performance issues on iOS
* generally serious performance issues with hundreds of notes (e.g. problems with scrolling and responsiveness)
* nothing is cached, everything is reloaded on each access
* full-size images are used for preview-thumbnails, there is no down-scaling for better performance

Essentially, Google Keep cannot cover my use case.

...as a result, I am still looking for a simpler and faster alternative to Evernote 😉
