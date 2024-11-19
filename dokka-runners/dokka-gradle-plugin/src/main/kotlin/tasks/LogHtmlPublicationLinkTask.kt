/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.tasks

import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask
import org.gradle.kotlin.dsl.of
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import org.jetbrains.dokka.gradle.internal.PluginFeaturesService.Companion.pluginFeaturesService
import org.jetbrains.dokka.gradle.internal.appendPath
import org.slf4j.LoggerFactory
import java.net.HttpURLConnection
import java.net.URI
import javax.inject.Inject

/**
 * Prints an HTTP link in the console when the HTML publication is generated.
 *
 * The HTML publication requires a web server since it loads resources via JavaScript.
 *
 * By default, it uses
 * [IntelliJ's built-in server](https://www.jetbrains.com/help/phpstorm/php-built-in-web-server.html#ws_html_preview_output_built_in_browser)†
 * to host the file.
 *
 * This task can be disabled via a Gradle property.
 * See [org.jetbrains.dokka.gradle.internal.PluginFeaturesService.enableLogHtmlPublicationLink].
 *
 * ---
 *
 * † For some reason, the only doc page for the built-in server I could find is for PhpStorm,
 * but the built-in server is also available in IntelliJ IDEA.
 */
@UntrackedTask(because = "logging-only task")
abstract class LogHtmlPublicationLinkTask
@Inject
@InternalDokkaGradlePluginApi
constructor(
    providers: ProviderFactory
) : DokkaBaseTask() {

    /**
     * A localhost server, for serving Dokka HTML.
     *
     * A server is required because Dokka HTML uses JavaScript.
     */
    @get:Console
    abstract val serverUri: Property<String>

    /**
     * Path to the `index.html` of the publication, relative to the root directory,
     * prepended with the IntelliJ project name.
     *
     * The path will be appended to [serverUri].
     *
     * The IntelliJ built-in server requires a relative path originating from the root directory
     * of the IntelliJ project.
     *
     * For example,
     *
     * - given an IntelliJ project named `MyProject` located in a directory:
     *    ```
     *    /Users/rachel/projects/my-project/
     *    ```
     * - and the publication is generated with an index file
     *    ```
     *    /Users/rachel/projects/my-project/docs/build/dokka/html/index.html
     *    ````
     * - then [indexHtmlPath] must be
     *    ```
     *    docs/build/dokka/html/index.html
     *    ```
     * - so that (assuming [serverUri] is `http://localhost:63342`) the logged URL is
     *    ```
     *    http://localhost:63342/MyProject/docs/build/dokka/html/index.html
     *    ```
     */
    @get:Console
    abstract val indexHtmlPath: Property<String>

    init {
        // Don't assign a group - this task is a 'finalizer' util task, so it doesn't make sense
        // to display this task prominently.
        group = "other"

        val enableLogHtmlPublicationLink = project.pluginFeaturesService.enableLogHtmlPublicationLink
        super.onlyIf("task is enabled via 'enableLogHtmlPublicationLink' property") {
            enableLogHtmlPublicationLink.get()
        }

        super.onlyIf("${::serverUri.name} is present") {
            !serverUri.orNull.isNullOrBlank()
        }

        super.onlyIf("${::indexHtmlPath.name} is present") {
            !indexHtmlPath.orNull.isNullOrBlank()
        }

        val serverActive = providers.of(ServerActiveCheck::class) {
            parameters.uri.convention(serverUri)
        }
        super.onlyIf("server URL is reachable") { serverActive.get() }
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
     * Use HTTP connection utils included with Java to avoid bringing in a new dependency for such
     * a small util.
     *
     * The check uses a [ValueSource] source ensure the server status is queried every time,
     * and the result is not Configuration Cached.
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
            val uri = URI.create(parameters.uri.get())
            val (status, err) = httpGetStatus(uri)

            if (err != null) {
                logger.info("could not reach URI ${uri}: $err")
            }

            logger.info("got $status from $uri")
            return status > 0
        }

        private fun httpGetStatus(uri: URI): Pair<Int, Exception?> {
            var connection: HttpURLConnection? = null
            try {
                val url = uri.toURL()
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 1_000 // 1 second
                connection.readTimeout = 1_000 // 1 second
                return (connection.responseCode to null)
            } catch (ex: Exception) {
                return (-1 to ex)
            } finally {
                connection?.disconnect()
            }
        }

        // When the minimum supported Java version is 11+
        //private fun httpGetStatusJdk11(uri: URI): Result<Int> {
        //    try {
        //        val client = java.net.http.HttpClient.newHttpClient()
        //        val request = java.net.http.HttpRequest
        //            .newBuilder()
        //            .uri(uri)
        //            .timeout(Duration.ofSeconds(1))
        //            .GET()
        //            .build()
        //        // don't care about the status - only if the server is available
        //        val response = client.send(request, java.net.http.HttpResponse.BodyHandlers.ofString())
        //        return Result.success(response.statusCode())
        //    } catch (ex: Exception) {
        //        return Result.failure(ex)
        //    }
        //}
    }

    companion object
}
