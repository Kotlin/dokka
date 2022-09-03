plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

gradlePlugin {
    plugins {
        create("build-logic") {
            id = "build-logic"
            implementationClass = "org.jetbrains.BuildLogic"
        }
    }
}

dependencies {
    implementation("com.github.jengelman.gradle.plugins:shadow:2.0.4")
    implementation("org.jetbrains.kotlinx:binary-compatibility-validator:0.11.0")
    implementation("io.github.gradle-nexus:publish-plugin:1.1.0")
}
