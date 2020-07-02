repositories {
    maven("https://dl.bintray.com/kotlin/kotlin-dev/")
    jcenter()
    mavenLocal()
    mavenCentral()
}

afterEvaluate {
    logger.quiet("Gradle version: ${gradle.gradleVersion}")
    logger.quiet("Kotlin version: ${properties["dokka_it_kotlin_version"]}")
}
