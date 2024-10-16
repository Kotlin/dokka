plugins {
    `java-application-convention`
    `dokka-convention`
}

dependencies {
    implementation(project(":my-java-library"))
}

dokka {
    dokkaSourceSets.configureEach {
        includes.from("Module.md")
    }
}

application {
    mainClass = "demo.MyJavaApplication"
}
