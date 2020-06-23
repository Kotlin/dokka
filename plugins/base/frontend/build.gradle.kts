plugins {
    id("com.moowork.node") version "1.3.1"
}

task("generateFrontendFiles") {
    dependsOn("npmInstall", "npm_run_build")
}

tasks {
    val npmRunBuild by registering {
        inputs.dir("$projectDir/src/main/")
        inputs.files("$projectDir/package.json", "$projectDir/webpack.config.js")
        outputs.dir("$projectDir/dist/")
        outputs.cacheIf { true }
    }

    clean {
        delete = setOf("$projectDir/node_modules", "$projectDir/dist/")
    }
}