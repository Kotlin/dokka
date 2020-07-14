import org.jetbrains.configurePublication

dependencies {
    implementation(project(":plugins:base"))
    implementation(project(":plugins:gfm"))
}

configurePublication("jekyll-plugin")
