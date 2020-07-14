import org.jetbrains.configurePublication

dependencies {
    implementation(project(":plugins:base"))
    testImplementation(project(":plugins:base"))
    testImplementation(project(":plugins:base:test-utils"))
    testImplementation(project(":test-tools"))
}

configurePublication("kotlin-as-java-plugin")
