package buildsrc.conventions

import java.time.Duration
import org.gradle.api.tasks.testing.logging.TestLogEvent

/**
 * A convention plugin that sets up common config and sensible defaults for all subprojects.
 */

plugins {
  base
}

if (project != rootProject) {
  project.version = rootProject.version
  project.group = rootProject.group
}

tasks.withType<AbstractArchiveTask>().configureEach {
  // https://docs.gradle.org/current/userguide/working_with_files.html#sec:reproducible_archives
  isPreserveFileTimestamps = false
  isReproducibleFileOrder = true
}

tasks.withType<AbstractTestTask>().configureEach {
  timeout.set(Duration.ofMinutes(60))

  testLogging {
    showCauses = true
    showExceptions = true
    showStackTraces = true
    showStandardStreams = true
    events(
      TestLogEvent.PASSED,
      TestLogEvent.FAILED,
      TestLogEvent.SKIPPED,
      TestLogEvent.STARTED,
      TestLogEvent.STANDARD_ERROR,
      TestLogEvent.STANDARD_OUT,
    )
  }
}

tasks.withType<AbstractCopyTask>().configureEach {
  includeEmptyDirs = false
}

val updateTestReportCss by tasks.registering {
  description = "Hack so the Gradle test reports have dark mode"
  // the CSS is based on https://github.com/gradle/gradle/pull/12177

  mustRunAfter(tasks.withType<Test>())
  mustRunAfter(tasks.withType<TestReport>())

  val cssFiles = layout.buildDirectory.asFileTree.matching {
    include("reports/**/css/base-style.css")
    include("reports/**/css/style.css")
  }

  outputs.files(cssFiles.files)

  doLast {
    cssFiles.forEach { cssFile ->
      val fileContent = cssFile.readText()

      if ("/* Dark mode */" in fileContent) {
        return@forEach
      } else {
        when (cssFile.name) {
          "base-style.css" -> cssFile.writeText(
            fileContent + """
              
            /* Dark mode */
            @media (prefers-color-scheme: dark) {
                html {
                    background: black;
                }
                body, a, a:visited {
                    color: #E7E7E7FF;
                }
                #footer, #footer a {
                    color: #cacaca;
                }
                ul.tabLinks li {
                    border: solid 1px #cacaca;
                    background-color: #151515;
                }
                ul.tabLinks li:hover {
                    background-color: #383838;
                }
                ul.tabLinks li.selected {
                    background-color: #002d32;
                    border-color: #007987;
                }
                div.tab th, div.tab table {
                    border-bottom: solid #d0d0d0 1px;
                }
                span.code pre {
                    background-color: #0a0a0a;
                    border: solid 1px #5f5f5f;
                }
            }
          """.trimIndent()
          )

          "style.css"      -> cssFile.writeText(
            fileContent + """
            
            /* Dark mode */
            @media (prefers-color-scheme: dark) {
                .breadcrumbs, .breadcrumbs a {
                    color: #9b9b9b;
                }
                #successRate, .summaryGroup {
                    border: solid 2px #d0d0d0;
                }
                .success, .success a {
                    color: #7fff7f;
                }
                div.success, #successRate.success {
                    background-color: #001c00;
                    border-color: #7fff7f;
                }
                .failures, .failures a {
                    color: #a30000;
                }
                .skipped, .skipped a {
                    color: #a26d13;
                }
                div.failures, #successRate.failures {
                    background-color: #170000;
                    border-color: #a30000;
                }
            }
          """.trimIndent()
          )
        }
      }
    }
  }
}

tasks.withType<Test>().configureEach {
  finalizedBy(updateTestReportCss)
}

tasks.withType<TestReport>().configureEach {
  finalizedBy(updateTestReportCss)
}

tasks.matching { it.name == "validatePlugins" }.configureEach {
  // prevent warning
  // Task ':validatePlugins' uses this output of task ':updateTestReportCss' without declaring an explicit or implicit dependency.
  mustRunAfter(updateTestReportCss)
}
