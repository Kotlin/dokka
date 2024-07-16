package dev.adamko.dokkatoo.tasks

import dev.adamko.dokkatoo.internal.DokkatooInternalApi
import dev.adamko.dokkatoo.internal.appendPath
import dev.adamko.dokkatoo.tasks.LogHtmlPublicationLinkTask.Companion.ENABLE_TASK_PROPERTY_NAME
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.inject.Inject
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.*
import org.gradle.work.DisableCachingByDefault
import org.slf4j.LoggerFactory

/**
 * Prints an HTTP link in the console when the HTML publication is generated.
 *
 * The HTML publication requires a web server since it loads resources via JavaScript.
 *
 * By default, it uses
 * [IntelliJ's built-in server](https://www.jetbrains.com/help/phpstorm/php-built-in-web-server.html#ws_html_preview_output_built_in_browser)†
 * to host the file.
 *
 *
 * This task can be disabled using the [ENABLE_TASK_PROPERTY_NAME] project property.
 *
 * ---
 *
 * † For some reason, the only doc page for the built-in server I could find is for PhpStorm,
 * but the built-in server is also available in IntelliJ IDEA.
 */
@DisableCachingByDefault(because = "logging-only task")
abstract class LogHtmlPublicationLinkTask
@Inject
@DokkatooInternalApi
constructor(
  providers: ProviderFactory
) : DokkatooTask() {

  @get:Console
  abstract val serverUri: Property<String>

  /**
   * Path to the `index.html` of the publication. Will be appended to [serverUri].
   *
   * The IntelliJ built-in server requires a relative path originating from the _parent_ directory
   * of the IntelliJ project.
   *
   * For example,
   *
   * * given an IntelliJ project path of
   *    ```
   *    /Users/rachel/projects/my-project/
   *    ```
   * * and the publication is generated with an index file
   *    ```
   *    /Users/rachel/projects/my-project/docs/build/dokka/html/index.html
   *    ````
   * * then IntelliJ requires [indexHtmlPath] is
   *    ```
   *    my-project/docs/build/dokka/html/index.html
   *    ```
   * * so that (assuming [serverUri] is `http://localhost:63342`) the logged URL is
   *    ```
   *    http://localhost:63342/my-project/docs/build/dokka/html/index.html
   *    ```
   */
  @get:Console
  abstract val indexHtmlPath: Property<String>

  init {
    // Don't assign a group - this task is a 'finalizer' util task, so it doesn't make sense
    // to display this task prominently.
    group = "other"

    val serverActive = providers.of(ServerActiveCheck::class) {
      parameters.uri.convention(serverUri)
    }
    super.onlyIf("server URL is reachable") { serverActive.get() }

    val logHtmlPublicationLinkTaskEnabled = providers
      .gradleProperty(ENABLE_TASK_PROPERTY_NAME)
      .map(String::toBoolean)
      .orElse(true)

    super.onlyIf("task is enabled via property") {
      logHtmlPublicationLinkTaskEnabled.get()
    }

    super.onlyIf("${::serverUri.name} is present") {
      !serverUri.orNull.isNullOrBlank()
    }

    super.onlyIf("${::indexHtmlPath.name} is present") {
      !indexHtmlPath.orNull.isNullOrBlank()
    }
  }

  @TaskAction
  fun exec() {
    val serverUri = serverUri.get()
    val indexHtmlPath = indexHtmlPath.get()

    logger.info(
      "LogHtmlPublicationLinkTask received variables " +
          "serverUri:$serverUri, " +
          "indexHtmlPath:$indexHtmlPath"
    )

    val link = URI(serverUri).appendPath(indexHtmlPath)

    logger.lifecycle("Generated Dokka HTML publication: $link")
  }

  /**
   * Check if the server URI that can host the generated Dokka HTML publication is accessible.
   *
   * Use the [HttpClient] included with Java 11 to avoid bringing in a new dependency for such
   * a small util.
   *
   * The check uses a [ValueSource] source to attempt to be compatible with Configuration Cache, but
   * I'm not certain that this is necessary, or if a [ValueSource] is the best way to achieve it.
   */
  internal abstract class ServerActiveCheck : ValueSource<Boolean, ServerActiveCheck.Parameters> {

    private val logger = LoggerFactory.getLogger(ServerActiveCheck::class.java)

    interface Parameters : ValueSourceParameters {
      /**
       * IntelliJ built-in server's default address is `http://localhost:63342`
       * See https://www.jetbrains.com/help/idea/settings-debugger.html
       */
      val uri: Property<String>
    }

    override fun obtain(): Boolean {
      try {
        val uri = URI.create(parameters.uri.get())
        val client = HttpClient.newHttpClient()
        val request = HttpRequest
          .newBuilder()
          .uri(uri)
          .timeout(Duration.ofSeconds(1))
          .GET()
          .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofString())
        // don't care about the status - only if the server is available
        logger.info("got ${response.statusCode()} from $uri")
        return response.statusCode() > 0
      } catch (ex: Exception) {
        logger.info("could not reach URI ${parameters.uri.get()}: $ex")
        return false
      }
    }
  }

  companion object {
    /**
     * Control whether the [LogHtmlPublicationLinkTask] task is enabled. Useful for disabling the
     * task locally, or in CI/CD, or for tests.
     *
     * It can be set in any `gradle.properties` file. For example, on a specific machine:
     *
     * ```properties
     * # $GRADLE_USER_HOME/gradle.properties
     * dev.adamko.dokkatoo.tasks.logHtmlPublicationLinkEnabled=false
     * ```
     *
     * or via an environment variable
     *
     * ```env
     * ORG_GRADLE_PROJECT_dev.adamko.dokkatoo.tasks.logHtmlPublicationLinkEnabled=false
     * ```
     */
    const val ENABLE_TASK_PROPERTY_NAME = "dev.adamko.dokkatoo.tasks.logHtmlPublicationLinkEnabled"
  }
}
