## Branches

As of late January 2020:

* master is the latest released version (0.10.0).
* dev-0.10.1 is the maintenance branch for 0.10.0. It will contain mostly bugfixes.
* dev-0.11.0 is a big rewrite of dokka, it's changing fast and things may break but this is where new features should be developed.

## Building dokka

Dokka is built with Gradle. To build it, use `./gradlew build`.
Alternatively, open the project directory in IntelliJ IDEA and use the IDE to build and run dokka.

Here's how to import and configure Dokka in IntelliJ IDEA:
 * Select "Open" from the IDEA welcome screen, or File > Open if a project is
  already open
* Select the directory with your clone of Dokka
  * Note: IDEA may have an error after the project is initally opened; it is OK
    to ignore this as the next step will address this error
* After IDEA opens the project, select File > New > Module from existing sources
  and select the `build.gradle` file from the root directory of your Dokka clone
* Use the default options and select "OK"
* After Dokka is loaded into IDEA, open the Gradle tool window (View > Tool
  Windows > Gradle) and click on the top left "Refresh all Gradle projects"
  button
* Verify the following project settings.  In File > Settings > Build, Execution,
  Deployment > Build Tools > Gradle > Runner:
  * Ensure "Delegate IDE build/run actions to gradle" is checked
  * "Gradle Test Runner" should be selected in the "Run tests using" drop-down
    menu
* Note: After closing and re-opening the project, IDEA may give an error
  message: "Error Loading Project: Cannot load 3 modules".  Open up the details
  of the error, and click "Remove Selected", as these module `.iml` files are
  safe to remove.

