package org.jetbrains.dokka.gradle.it

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.jetbrains.dokka.gradle.utils.gradleKtsProjectIntegrationTest
import org.jetbrains.dokka.gradle.utils.gradleProperties
import org.junit.jupiter.api.Test

class BasicProjectIntegrationTest {


    private val basicProject = gradleKtsProjectIntegrationTest("it-basic") {
        gradleProperties = gradleProperties.lines().joinToString("\n") { line ->
            if (line.startsWith("testMavenRepoDir")) {
                "testMavenRepoDir=${testMavenRepoRelativePath}"
            } else {
                line
            }
        }
    }

    @Test
    fun `test basic project`() {
        val build = basicProject.runner
            .withArguments(
                "clean",
                "dokkaGenerate",
                "--stacktrace",
                "--info",
            )
            .forwardOutput()
            .build()

        build.output shouldContain "BUILD SUCCESSFUL"
        build.output shouldContain "Generation completed successfully"


        val actualDokkaConfJson = basicProject
            .projectDir
            .resolve("build/dokka-config/dokka_configuration.json")
            .toFile()
            .readText()

        val actualDokkaConf = Json.parseToJsonElement(actualDokkaConfJson).jsonObject

        actualDokkaConf["moduleName"] shouldBe expectedDokkaConf["moduleName"]
        actualDokkaConf["moduleVersion"] shouldBe expectedDokkaConf["moduleVersion"]
        actualDokkaConf["cacheRoot"] shouldBe expectedDokkaConf["cacheRoot"]
        actualDokkaConf["offlineMode"] shouldBe expectedDokkaConf["offlineMode"]

        actualDokkaConf["includeNonPublic"] shouldBe expectedDokkaConf["includeNonPublic"]
        actualDokkaConf["reportUndocumented"] shouldBe expectedDokkaConf["reportUndocumented"]
        actualDokkaConf["skipEmptyPackages"] shouldBe expectedDokkaConf["skipEmptyPackages"]
        actualDokkaConf["skipDeprecated"] shouldBe expectedDokkaConf["skipDeprecated"]
        actualDokkaConf["jdkVersion"] shouldBe expectedDokkaConf["jdkVersion"]
    }
}

private val expectedDokkaConf = Json.parseToJsonElement(
// language=json
    """
{
  "moduleName": "Basic Project",
  "moduleVersion": "1.7.20-SNAPSHOT",
  "outputDir": ".../build/dokka/jekyll",
  "cacheRoot": null,
  "offlineMode": false,
  "sourceSets": [
    {
      "displayName": "jvm",
      "sourceSetID": {
        "scopeId": ":dokkaJekyll",
        "sourceSetName": "main"
      },
      "classpath": [
        ".../kotlin-stdlib-1.7.20.jar",
        ".../kotlin-stdlib-common-1.7.20.jar",
        ".../annotations-13.0.jar"
      ],
      "sourceRoots": [
        ".../src/main/kotlin",
        ".../src/main/java"
      ],
      "dependentSourceSets": [],
      "samples": [],
      "includes": [],
      "includeNonPublic": false,
      "reportUndocumented": false,
      "skipEmptyPackages": true,
      "skipDeprecated": false,
      "jdkVersion": 8,
      "sourceLinks": [
        {
          "localDirectory": ".../src/main",
          "remoteUrl": "https://github.com/Kotlin/dokka/tree/master/integration-tests/gradle/projects/it-basic/src/main",
          "remoteLineSuffix": "#L"
        }
      ],
      "perPackageOptions": [
        {
          "matchingRegex": "it.suppressedByPackage.*",
          "includeNonPublic": false,
          "reportUndocumented": false,
          "skipDeprecated": false,
          "suppress": true,
          "documentedVisibilities": [
            "PUBLIC"
          ]
        },
        {
          "matchingRegex": "it.overriddenVisibility.*",
          "includeNonPublic": false,
          "reportUndocumented": false,
          "skipDeprecated": false,
          "suppress": false,
          "documentedVisibilities": [
            "PRIVATE"
          ]
        }
      ],
      "externalDocumentationLinks": [
        {
          "url": "https://docs.oracle.com/javase/8/docs/api/",
          "packageListUrl": "https://docs.oracle.com/javase/8/docs/api/package-list"
        },
        {
          "url": "https://kotlinlang.org/api/latest/jvm/stdlib/",
          "packageListUrl": "https://kotlinlang.org/api/latest/jvm/stdlib/package-list"
        }
      ],
      "languageVersion": null,
      "apiVersion": null,
      "noStdlibLink": false,
      "noJdkLink": false,
      "suppressedFiles": [
        ".../src/main/kotlin/it/suppressedByPath"
      ],
      "analysisPlatform": "jvm",
      "documentedVisibilities": [
        "PUBLIC",
        "PROTECTED"
      ]
    }
  ],
  "pluginsClasspath": [
    ".../dokka-analysis-1.8.0-SNAPSHOT.jar",
    ".../jekyll-plugin-1.8.0-SNAPSHOT.jar",
    ".../gfm-plugin-1.8.0-SNAPSHOT.jar",
    ".../dokka-base-1.8.0-SNAPSHOT.jar",
    ".../kotlin-analysis-intellij-1.8.0-SNAPSHOT.jar",
    ".../kotlin-analysis-compiler-1.8.0-SNAPSHOT.jar",
    ".../kotlinx-html-jvm-0.7.5.jar",
    ".../kotlinx-coroutines-core-jvm-1.6.3.jar",
    ".../kotlin-stdlib-jdk8-1.7.20.jar",
    ".../jackson-databind-2.12.7.1.jar",
    ".../jackson-annotations-2.12.7.jar",
    ".../jackson-core-2.12.7.jar",
    ".../jackson-module-kotlin-2.12.7.jar",
    ".../kotlin-reflect-1.7.20.jar",
    ".../kotlin-stdlib-jdk7-1.7.20.jar",
    ".../kotlin-stdlib-1.7.20.jar",
    ".../jsoup-1.15.3.jar",
    ".../freemarker-2.3.31.jar",
    ".../kotlin-stdlib-common-1.7.20.jar",
    ".../annotations-13.0.jar"
  ],
  "pluginsConfiguration": [
    {
      "fqPluginName": "org.jetbrains.dokka.base.DokkaBase",
      "serializationFormat": "JSON",
      "values": "{ \"customStyleSheets\": [\".../customResources/logo-styles.css\", \".../customResources/custom-style-to-add.css\"], \"customAssets\" : [\".../customResources/custom-resource.svg\"] }"
    }
  ],
  "modules": [],
  "failOnWarning": false,
  "delayTemplateSubstitution": false,
  "suppressObviousFunctions": false,
  "includes": [],
  "suppressInheritedMembers": false,
  "finalizeCoroutines": true
}
"""
).jsonObject
