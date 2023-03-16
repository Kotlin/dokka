plugins {
    base
    id("com.github.node-gradle.node") version "3.2.1"
}

node {
    version.set(libs.versions.node)

    // https://github.com/node-gradle/gradle-node-plugin/blob/3.5.1/docs/faq.md#is-this-plugin-compatible-with-centralized-repositories-declaration
    download.set(true)
    distBaseUrl.set(null as String?) // Strange cast to avoid overload ambiguity
}

val npmRunBuild = tasks.getByName("npm_run_build") {
    inputs.dir(file("src/main"))
    inputs.files(file("package.json"), file("webpack.config.js"))
    outputs.dir(file("dist/"))
    outputs.cacheIf { true }
}

task("generateFrontendFiles") {
    dependsOn(npmRunBuild)
}

tasks {
    clean {
        delete(file("node_modules"), file("dist"))
    }
}
