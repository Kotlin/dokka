import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "1.0.0"
    kotlin("plugin.serialization") version embeddedKotlinVersion
    `jvm-test-suite`
}

group = "org.jetbrains.dokka"
version = "2.0.0"

dependencies {
    implementation("org.jetbrains.dokka:dokka-core:1.7.20")

//    compileOnly("com.android.tools.build:gradle:4.0.1")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.4.1")
}

java {
    withSourcesJar()
}

gradlePlugin {
    plugins.create("dokkaGradlePlugin2") {
        id = "org.jetbrains.dokka2"
        displayName = "Dokka plugin 2"
        description = "Dokka, the Kotlin documentation tool"
        implementationClass = "org.jetbrains.dokka.gradle.DokkaPlugin"
        isAutomatedPublishing = true
    }
}

pluginBundle {
    website = "https://www.kotlinlang.org/"
    vcsUrl = "https://github.com/kotlin/dokka.git"
    tags = listOf("dokka", "kotlin", "kdoc", "android", "documentation")
}

tasks.validatePlugins {
    enableStricterValidation.set(true)
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        this.freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

val projectTestMavenRepoDir = layout.buildDirectory.dir("test-maven-repo")

publishing {
    repositories {
        maven(projectTestMavenRepoDir) {
            name = "Test"
        }
    }
}



@Suppress("UnstableApiUsage") // jvm test suites are incubating
testing.suites {
    val test by getting(JvmTestSuite::class) {
        useJUnitJupiter()

        dependencies {
            implementation(project.dependencies.kotlin("test"))
            implementation(project.dependencies.gradleTestKit())
//            implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
//            implementation("com.android.tools.build:gradle:4.0.1")
        }
    }

    val testFunctional by registering(JvmTestSuite::class) {
        useJUnitJupiter()

        dependencies {
            implementation(project)

            implementation(project.dependencies.kotlin("test"))
            implementation(project.dependencies.gradleTestKit())
//            implementation("org.jetbrains.kotlin:kotlin-gradle-plugin")
//            implementation("com.android.tools.build:gradle:4.0.1")
        }

        targets.all {
            testTask.configure {
                shouldRunAfter(test)
                dependsOn(tasks.matching { it.name == "publishAllPublicationsToTestRepository" })

//                dependsOn(installMavenInternal)
                systemProperties(
                    "testMavenRepoDir" to file(projectTestMavenRepoDir).canonicalPath,
                )
            }
        }

        sources {
            java {
                resources {
                    srcDir(tasks.pluginUnderTestMetadata.map { it.outputDirectory })
                }
            }
        }

        gradlePlugin.testSourceSet(sources)
    }

    tasks.check { dependsOn(testFunctional) }
}


tasks.withType<Test>().configureEach {
    testLogging {
        events = TestLogEvent.values().toSet()
        showStandardStreams = true
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}
