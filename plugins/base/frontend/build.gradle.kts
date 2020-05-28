plugins {
    id("com.moowork.node") version "1.3.1"
}

task("generateFrontendFiles") {
    dependsOn("npm_install", "npm_run_build")
}

tasks {
    "npm_run_build" {
        inputs.dir("$projectDir/src/main/components/")
        inputs.files("$projectDir/package.json", "$projectDir/webpack.config.js")
        outputs.dir("$projectDir/dist/")
    }
    clean {
        delete = setOf("$projectDir/node_modules", "$projectDir/dist/", "$projectDir/package-lock.json")
    }
}
