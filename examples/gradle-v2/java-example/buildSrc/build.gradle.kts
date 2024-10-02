plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25")
    val dokkaVersion = providers.gradleProperty("dokkaVersion").getOrElse("2.0.0-Beta")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
}
