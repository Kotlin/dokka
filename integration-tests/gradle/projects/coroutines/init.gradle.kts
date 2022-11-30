initscript {
    repositories {
        mavenLocal()
    }
    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:+")
    }
}

allprojects {
    plugins.withId("org.jetbrains.dokka") {
        tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaTask>().configureEach {
            finalizeCoroutines.set(false)
        }
    }
}
