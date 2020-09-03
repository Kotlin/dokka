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
  
In order to publish dokka locally use: `./gradlew publishToMavenLocal` and add `mavenLocal()` to repositories in the project you want to generate documentation for.
This will allow you to run the latest version in your project from local maven repository. 
Keep in mind that those builds are postfixed with `-SNAPSHOT`, eg. `1.4.0-SNAPSHOT`, so remember to update plugin version.
Dokka version generated with this build is taken from `gradle.properties` file under `dokka_version_base` and is visible in logs while running `publishToMavenLocal` task.

