plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.20-RC-266")
    val dokkaVersion = providers.gradleProperty("dokkaVersion").getOrElse("2.0.20-SNAPSHOT")
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:$dokkaVersion")
}
