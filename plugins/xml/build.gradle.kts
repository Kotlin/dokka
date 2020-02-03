publishing {
    publications {
        register<MavenPublication>("xmlPlugin") {
            artifactId = "xml-plugin"
            from(components["java"])
        }
    }
}