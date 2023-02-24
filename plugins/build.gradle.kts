import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

subprojects {
    apply {
        plugin("maven-publish")
    }

    dependencies {
        compileOnly(project(":core"))
        implementation(kotlin("stdlib-jdk8"))
        implementation(kotlin("stdlib"))
        implementation(kotlin("reflect"))

        testImplementation(project(":test-utils"))
        testImplementation(project(":core:test-api"))
        testImplementation("org.junit.jupiter:junit-jupiter:5.6.0")
    }

    tasks.test {
        useJUnitPlatform()
        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
            events(TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            showExceptions = true
            showCauses = true
            showStackTraces = true
        }
    }

    val compileTestKotlin: KotlinCompile by tasks
    compileTestKotlin.kotlinOptions {
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=org.jetbrains.dokka.plugability.PreviewDokkaPluginApi"
        )
    }
}
