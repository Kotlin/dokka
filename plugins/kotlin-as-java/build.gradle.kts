publishing {
    publications {
        register<MavenPublication>("kotlin-as-java-plugin") {
            artifactId = "kotlin-as-java-plugin"
            from(components["java"])
        }
    }
}

dependencies {
    implementation(project(":plugins:base"))
}