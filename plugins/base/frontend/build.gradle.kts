plugins {
    id("com.moowork.node") version "1.3.1"
}

tasks {
    val npmRunBuild by registering {
        inputs.dir("$projectDir/src/main/")
        inputs.files("$projectDir/package.json", "$projectDir/webpack.config.js")
        outputs.dir("$projectDir/dist/")
        outputs.cacheIf { true }
    }

    register("generateFrontendFiles") {
        dependsOn(npmInstall, npmRunBuild)
    }

    clean {
        delete = setOf("$projectDir/node_modules", "$projectDir/dist/")
    }
}