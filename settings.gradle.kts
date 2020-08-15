rootProject.name = "dokka"

include("core")
include("plugins:base:search-component")
include("testApi")
include("test-tools")
include("runners:gradle-plugin")
include("runners:cli")
include("runners:maven-plugin")
include("kotlin-analysis")
include("kotlin-analysis:intellij-dependency")
include("kotlin-analysis:compiler-dependency")
include("plugins:base")
include("plugins:base:frontend")
include("plugins:base:test-utils")
include("plugins:mathjax")
include("plugins:gfm")
include("plugins:jekyll")
include("plugins:kotlin-as-java")
include("plugins:javadoc")
include("integration-tests")
include("integration-tests:gradle")
include("integration-tests:cli")
include("integration-tests:maven")

pluginManagement {
    val kotlin_version: String by settings
    plugins {
        kotlin("jvm").version(kotlin_version)
    }
}
