package org.jetbrains.dokka

interface DokkaBootstrap {

    fun configure(logger: DokkaLogger,
                  moduleName: String,
                  classpath: List<String>,
                  sources: List<String>,
                  samples: List<String>,
                  includes: List<String>,
                  outputDir: String,
                  format: String,
                  includeNonPublic: Boolean,
                  reportUndocumented: Boolean,
                  skipEmptyPackages: Boolean,
                  skipDeprecated: Boolean,
                  jdkVersion: Int,
                  generateIndexPages: Boolean,
                  sourceLinks: List<String>)

    fun generate()
}