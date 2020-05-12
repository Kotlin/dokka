import org.jetbrains.configureBintrayPublication

plugins {
    id("com.github.johnrengelman.shadow")
}

repositories {
    maven(url = "https://dl.bintray.com/kotlin/kotlinx")
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-cli-jvm:0.2.1")
    implementation("com.google.code.gson:gson:2.8.5")
    implementation(project(":core"))
}


tasks {
    shadowJar {
        val dokka_version: String by project
        archiveFileName.set("dokka-cli-$dokka_version.jar")
        archiveClassifier.set("")
        manifest {
            attributes("Main-Class" to "org.jetbrains.dokka.MainKt")
        }
    }
}

publishing {
    publications {
        register<MavenPublication>("dokkaCli") {
            artifactId = "dokka-cli"
            project.shadow.component(this)
        }
    }
}

configureBintrayPublication("dokkaCli")