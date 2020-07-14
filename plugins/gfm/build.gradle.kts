import org.jetbrains.configurePublication

dependencies {
    implementation(project(":plugins:base"))
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:test-utils"))
}

configurePublication("gfm-plugin")
