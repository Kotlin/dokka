plugins {
    id("com.moowork.node") version "1.3.1"
}

task("generateSearchFiles") {
    dependsOn("npm_install", "npm_run_build")
}

tasks {
    "npm_run_build" {
        inputs.dir("$projectDir/src/main/js/search/").withPathSensitivity(PathSensitivity.RELATIVE)
        inputs.files("$projectDir/package.json", "$projectDir/webpack.config.js").withPathSensitivity(PathSensitivity.RELATIVE)
        outputs.dir("$projectDir/dist/")
        outputs.cacheIf { true }
    }
    clean {
        delete = setOf("$projectDir/node_modules", "$projectDir/dist/", "$projectDir/package-lock.json")
    }
}