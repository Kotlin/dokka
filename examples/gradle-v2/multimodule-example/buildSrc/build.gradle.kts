plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    val dokkaVersion = providers.gradleProperty("dokkaVersion").getOrElse("2.1.0")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
}
