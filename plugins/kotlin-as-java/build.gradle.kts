publishing {
    publications {
        register<MavenPublication>("kotlin-as-java-plugin") {
            artifactId = "kotlin-as-java-plugin"
            from(components["java"])
        }
    }
}