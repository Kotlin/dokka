allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        google()
    }
}

afterEvaluate {
    logger.quiet("Gradle version: ${gradle.gradleVersion}")
    logger.quiet("Kotlin version: ${properties["dokka_it_kotlin_version"]}")
    properties["dokka_it_android_gradle_plugin_version"]?.let { androidVersion ->
        logger.quiet("Android version: $androidVersion")
    }
}
