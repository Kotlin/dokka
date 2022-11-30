initscript {
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://cache-redirector.jetbrains.com/jcenter")
    }
    dependencies {
        classpath("org.jetbrains.dokka:dokka-gradle-plugin:for-integration-tests-SNAPSHOT")
    }
}

allprojects {
    plugins.withId("org.jetbrains.dokka") {
        tasks.withType<org.jetbrains.dokka.gradle.AbstractDokkaTask>().configureEach {
            finalizeCoroutines.set(false)
        }
    }
}
