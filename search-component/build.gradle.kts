plugins {
    id("com.moowork.node") version "1.3.1"
}

task("generateSearchFiles") {
    dependsOn("npm_install", "npm_run_build")
}

tasks {
    "npm_run_build" {
        inputs.dir("$projectDir/src/main/js/search/")
        inputs.files("$projectDir/package.json", "$projectDir/*.config.js")
        outputs.dir("$projectDir/dist/")
    }
    clean {
        delete = setOf("$projectDir/node_modules", "$projectDir/dist/", "$projectDir/package-lock.json")
    }
}