import org.jetbrains.*

plugins {
    `kotlin-dsl`
    id("org.jetbrains.conventions.maven-publish")
    id("org.jetbrains.conventions.base-java")
    alias(libs.plugins.gradle.pluginPublish)
    signing
}

dependencies {
    api(projects.core)

    compileOnly(libs.gradlePlugin.kotlin)
    compileOnly(libs.gradlePlugin.android)

    testImplementation(projects.testUtils)
    testImplementation(libs.gradlePlugin.kotlin)
    testImplementation(libs.gradlePlugin.android)
}

// Gradle will put its own version of the stdlib in the classpath, so not pull our own we will end up with
// warnings like 'Runtime JAR files in the classpath should have the same version'
configurations.api.configure {
    excludeGradleCommonDependencies()
}

/**
 * These dependencies will be provided by Gradle, and we should prevent version conflict
 * Code taken from the Kotlin Gradle plugin:
 * https://github.com/JetBrains/kotlin/blob/70e15b281cb43379068facb82b8e4bcb897a3c4f/buildSrc/src/main/kotlin/GradleCommon.kt#L72
 */
fun Configuration.excludeGradleCommonDependencies() {
    dependencies
        .withType<ModuleDependency>()
        .configureEach {
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-reflect")
            exclude(group = "org.jetbrains.kotlin", module = "kotlin-script-runtime")
        }
}

group = "org.jetbrains.dokka"
version = dokkaVersion

gradlePlugin {
    website.set("https://www.kotlinlang.org/")
    vcsUrl.set("https://github.com/kotlin/dokka.git")

    plugins {
        all {
            implementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"
            tags.set(listOf("dokka", "kotlin", "kdoc", "android", "documentation"))
        }
        create("dokkaGradlePlugin") {
            id = "org.jetbrains.dokka"
            displayName = "Dokka plugin"
            description = "Dokka, the Kotlin documentation tool"
            isAutomatedPublishing = true
        }
        register("dokkaGradlePluginForIntegrationTests") {
            id = "dokka-gradle-plugin"
            version = "for-integration-tests-SNAPSHOT"
        }
        register("pluginMaven") {
            id = "dokka-gradle-plugin"
        }
    }
}

signing {
    isRequired = false
}

tasks.validatePlugins {
    enableStricterValidation.set(true)
}

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf { publication != publishing.publications["dokkaGradlePluginForIntegrationTests"] }
}

afterEvaluate { // Workaround for an interesting design choice https://github.com/gradle/gradle/blob/c4f935f77377f1783f70ec05381c8182b3ade3ea/subprojects/plugin-development/src/main/java/org/gradle/plugin/devel/plugins/MavenPluginPublishPlugin.java#L49
    configureSpacePublicationIfNecessary("pluginMaven")
    configureSonatypePublicationIfNecessary("pluginMaven")
    createDokkaPublishTaskIfNecessary()
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.WARN
}
