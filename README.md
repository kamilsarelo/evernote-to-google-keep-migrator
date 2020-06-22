# Evernote to Google Keep Migrator

If you ever wanted to migrate your notes from [Evernote](https://evernote.com) to [Google Keep](https://keep.google.com) then this project might help you or give you an idea of how it can be done in an automatic way.

## Motivation

🚧👷 work in progress 👷🚧

## Idea

There is no direct and efficient way to migrate Evernote data to Google Keep since Google Keep lacks any import feature. So the idea is very simple: let's automate what I would have done manually - copy notes from Evernote and paste them manually one after another in Google Keep. The notes might require some additional adaptations since Evernote uses HTML and Google Keep just plain text - but this can be automated too.

Since both services provide web interfaces I can use the Selenium framework for automated testing of web applications to accomplish the task. Basically, I need a web browser window with Evernote, another one with Google Keep, and an application that controls both. With the power of DOM everything within the browser windows can be selected and manipulated. That is all I need 🙂 

## Dependencies

* macOS (parts of the code are specific for macOS, adaptations are required for other operating systems)
* [WebDriver for Chrome](https://chromedriver.chromium.org/downloads) (alternatively [WebDriver for Firefox](https://github.com/mozilla/geckodriver))
* [Selenium Client & WebDriver language bindings for Java](https://www.selenium.dev/downloads/
)
* [jsoup Java HTML Parser](https://jsoup.org/)

## Application

The Java code orchestrating everything is a simple MVC application. The model provides the Evernote and Google Keep WebDrivers. The view is the application's minimalistic UI with three buttons and takes care of arranging the WebDriver windows. The controller is responsible of logging in to Evernote and Google Keep as well as migrating notes.

### Starting the application

The application is started from Eclipse. The input source (keyboard layout) has to be set to **"US International PC"** before starting the application, otherwise the **"@"** character might not work properly in the WebDriver.

The application starts two WebDriver instances: one for Evernote, and one for Google Keep respectively. Both WebDriver windows are positioned next to each other just below the application's UI. Then the application logs in to both services. The usernames and passwords for both services are provided via Java system properties using command line arguments, or an Eclipse launch configuration, or any other IDE's run configuration.

Logging in to Evernote is a no-brainer - just a matter of entering the username and password and pressing a button using Selenium. With Google Keep or Google generally it is a challenging problem.

Some time ago [Google decided to prevent people from using UI automation to access their services](https://stackoverflow.com/questions/59534028/sign-in-to-gmail-account-fails-selenium-automation/59569816#59569816). Google is able to detect that a "robot" might be trying to log in and they don't want that to happen so you get this error message when using Selenium:
*This browser or app may not be secure. Try using a different browser. If you’re already using a supported browser, you can refresh your screen and try again to sign in.*

Fortunately, they missed something: you can use your Google account to sign in to a different site in the WebDriver and then go to Google Keep. I chose IMDb since it is one of the most popular websites on the internet and they offer to [sign in with Google](https://www.imdb.com/registration/signin?ref=nv_generic_lgin).

Once the application logged in to both services the fun part can begin! *(Note that I additionally require a notebook to be selected and a specific sort order of the notes to be set in Evernote. However, this can easily be removed from the code depending on your requirements.)*

[![Starting the migrator](/assets/startup_600.gif)](/assets/startup_full.gif)

### Migrating a note

🚧👷 work in progress 👷🚧

A click on the button **"migrate the selected note in Evernote"** will first look at the selected note in Evernote, scrape it, then convert HTML-lists to plain-text-lists, and finally remove all HTML tags. In a second step...

[![Migrate selected](/assets/migrate_600.gif)](/assets/migrate_full.gif)

### Selecting and migrating the next note

A click on the button **"select the next note in Evernote and migrate it"** will simply select the next note in Evernote and then migrate it to Google Keep like before. A convenience feature that comes in very handy when you want to migrate a huge number of notes semi-automatically and keep control after each migration. I chose semi-automation because it allows me to have control over what is happening and what is being migrated — at least to a certain degree - and I have seen full-automation  fail too often.

[![Migrate next](/assets/migratenext_600.gif)](/assets/migratenext_full.gif)

### Deleting all notes

A click on the button **"delete all notes in Google Keep"** will delete all taken notes in Google Keep. This is useful in case you want to start from scratch again.

[![Delete](/assets/delete_600.gif)](/assets/delete_full.gif)

## Conclusion

🚧👷 work in progress 👷🚧

* bottom line: this was really fun to code but **Google Keep didn't live up to my expectations at all**
* no caching
* sorting not remembered
* no equally sized grid view
* performance and usability issues with lots of notes (especially on iOS)

...as a result, I am still looking for a simpler and faster alternative to Evernote 😉
