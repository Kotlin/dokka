import org.jetbrains.configureBintrayPublication

publishing {
    publications {
        register<MavenPublication>("gfmPlugin") {
            artifactId = "gfm-plugin"
            from(components["java"])
        }
    }
}

configureBintrayPublication("gfmPlugin")

dependencies {
    compileOnly(project(":plugins:base"))
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:test-utils"))
}
