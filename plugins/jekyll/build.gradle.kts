import org.jetbrains.configureBintrayPublication

publishing {
    publications {
        register<MavenPublication>("jekyllPlugin") {
            artifactId = "jekyll-plugin"
            from(components["java"])
        }
    }
}

configureBintrayPublication("jekyllPlugin")

dependencies {
    compileOnly(project(":plugins:base"))
    implementation(project(":plugins:gfm"))
}
