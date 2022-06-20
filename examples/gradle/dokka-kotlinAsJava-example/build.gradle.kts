plugins {
    kotlin("jvm") version "1.7.0"
    id("org.jetbrains.dokka") version ("1.7.0")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test-junit"))

    // Will apply the plugin to all dokka tasks
    dokkaPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.0")

    // Will apply the plugin only to the `:dokkaHtml` task
    //dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.0")

    // Will apply the plugin only to the `:dokkaGfm` task
    //dokkaGfmPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.7.0")
}
