rootProject.name = "dokka"

include(
    ":core",
    ":core:test-api",
    ":core:content-matcher-test-utils",

    ":kotlin-analysis",
    ":kotlin-analysis:intellij-dependency",
    ":kotlin-analysis:compiler-dependency",

    ":runners:gradle-plugin",
    ":runners:cli",
    ":runners:maven-plugin",

    ":plugins:base",
    ":plugins:base:frontend",
    ":plugins:base:search-component",
    ":plugins:base:base-test-utils",
    ":plugins:all-modules-page",
    ":plugins:templating",
    ":plugins:versioning",
    ":plugins:android-documentation",

    ":plugins:mathjax",
    ":plugins:gfm",
    ":plugins:gfm:gfm-template-processing",
    ":plugins:jekyll",
    ":plugins:jekyll:jekyll-template-processing",
    ":plugins:kotlin-as-java",
    ":plugins:javadoc",

    ":integration-tests",
    ":integration-tests:gradle",
    ":integration-tests:cli",
    ":integration-tests:maven",

    ":test-utils",

    ":docs",
)

val isCiBuild = System.getenv("GITHUB_ACTIONS") != null || System.getenv("TEAMCITY_VERSION") != null

plugins {
    `gradle-enterprise`
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlwaysIf(isCiBuild)
    }
}

@Suppress("UnstableApiUsage") // Central declaration of repositories is an incubating feature
dependencyResolutionManagement {

    repositories {
        mavenCentral()
        google()
    }

    pluginManagement {
        repositories {
            gradlePluginPortal()
            mavenCentral()
        }
    }
}
