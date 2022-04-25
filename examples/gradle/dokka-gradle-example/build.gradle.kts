import org.jetbrains.dokka.gradle.DokkaTask
import java.net.URL

plugins {
    kotlin("jvm") version "1.6.21"
    id("org.jetbrains.dokka") version ("1.6.21")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit"))
}

tasks.withType<DokkaTask>().configureEach {
    dokkaSourceSets {
        named("main") {
            moduleName.set("Dokka Gradle Example")
            includes.from("Module.md")
            sourceLink {
                localDirectory.set(file("src/main/kotlin"))
                remoteUrl.set(URL("https://github.com/Kotlin/dokka/tree/master/" +
                        "examples/gradle/dokka-gradle-example/src/main/kotlin"
                ))
                remoteLineSuffix.set("#L")
            }
        }
    }
}
