/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it.cli

fun jsonBuilder(
    outputPath: String,
    pluginsClasspath: String,
    projectPath: String,
    globalSourceLinks: String = "",
    globalExternalDocumentationLinks: String = "",
    globalPerPackageOptions: String = "",
    reportUndocumented: Boolean = false

): String {
   return """{
  "moduleName": "Dokka Example",
  "moduleVersion": null,
  "outputDir": "$outputPath",
  "pluginsClasspath": ["$pluginsClasspath"],
  "cacheRoot": null,
  "offlineMode": false,
  "sourceLinks": [$globalSourceLinks],
  "externalDocumentationLinks": [$globalExternalDocumentationLinks],
  "perPackageOptions": [$globalPerPackageOptions],
  "sourceSets": [
    {
      "displayName": "jvm",
      "sourceSetID": {
        "scopeId": ":dokkaHtml",
        "sourceSetName": "main"
      },
      "sourceRoots": [
        "$projectPath"
      ],
      "dependentSourceSets": [],
      "samples": [],
      "includes": [],
      "includeNonPublic": false,
      "reportUndocumented": $reportUndocumented,
      "skipEmptyPackages": true,
      "skipDeprecated": false,
      "jdkVersion": 8,
      "sourceLinks": [],
      "perPackageOptions": [],
      "externalDocumentationLinks": [],
      "noStdlibLink": false,
      "noJdkLink": false,
      "suppressedFiles": [],
      "analysisPlatform": "jvm"
    }
  ]
}
"""
}
