package org.jetbrains.dokka.dokkatoo.tasks

import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import org.jetbrains.dokka.dokkatoo.internal.appendPath
import org.jetbrains.dokka.dokkatoo.tasks.LogHtmlPublicationLinkTask.Companion.ENABLE_TASK_PROPERTY_NAME
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

/**
 * Prints an HTTP link in the console when the HTML publication is generated.
 *
 * The HTML publication requires a web server, since it loads resources via javascript.
 *
 * By default, it uses
 * [IntelliJ's built-in server](https://www.jetbrains.com/help/idea/php-built-in-web-server.html)
 * to host the file.
 *
 * This task can be disabled using the [ENABLE_TASK_PROPERTY_NAME] project property.
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
   * * then IntelliJ requires the [indexHtmlPath] is
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
    // don't assign a group. This task is a 'finalizer' util task, so it doesn't make sense
    // to display this task prominently.
    group = "other"

    val serverActive = providers.of(ServerActiveCheck::class) {
      parameters.uri.convention(serverUri)
    }
    super.onlyIf("server URL is reachable") { serverActive.get() }

    val logHtmlPublicationLinkTaskEnabled = providers
      .gradleProperty(ENABLE_TASK_PROPERTY_NAME)
      .orElse("true")
      .map(String::toBoolean)
    super.onlyIf("task is enabled via property") {
      logHtmlPublicationLinkTaskEnabled.get()
    }
  }

  @TaskAction
  fun exec() {
    val serverUri = serverUri.orNull
    val filePath = indexHtmlPath.orNull

    if (serverUri != null && !filePath.isNullOrBlank()) {
      val link = URI(serverUri).appendPath(filePath).toString()

      logger.lifecycle("Generated Dokka HTML publication: $link")
    }
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

    interface Parameters : ValueSourceParameters {
      /** E.g. `http://localhost:63342` */
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
        return response.statusCode() > 0
      } catch (ex: Exception) {
        return false
      }
    }
  }

  companion object {
    /**
     * Control whether the [LogHtmlPublicationLinkTask] task is enabled. Useful for disabling the
     * task locally, or in CI/CD, or for tests.
     *
     * ```properties
     * #$GRADLE_USER_HOME/gradle.properties
     * org.jetbrains.dokka.dokkatoo.tasks.logHtmlPublicationLinkEnabled=false
     * ```
     *
     * or via an environment variable
     *
     * ```env
     * ORG_GRADLE_PROJECT_org.jetbrains.dokka.dokkatoo.tasks.logHtmlPublicationLinkEnabled=false
     * ```
     */
    const val ENABLE_TASK_PROPERTY_NAME = "org.jetbrains.dokka.dokkatoo.tasks.logHtmlPublicationLinkEnabled"
  }
}
