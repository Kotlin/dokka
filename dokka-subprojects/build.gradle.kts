/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    id("dokkabuild.base")
    id("dev.adamko.dev-publish") version "0.2.0"
    alias(libs.plugins.kotlinx.binaryCompatibilityValidator)
}

//tasks.register("publishAllPublicationsToProjectLocalRepository") {
//    dependsOn(subprojects.map { "${it.path}:publishAllPublicationsToProjectLocalRepository" })
//}

tasks.integrationTestPreparation {
    dependsOn(tasks.updateDevRepo)
}

dependencies {
//    devPublication(projects.analysisJavaPsi)
//    devPublication(projects.analysisKotlinApi)
//    devPublication(projects.analysisKotlinDescriptors)
//    devPublication(projects.analysisKotlinDescriptorsCompiler)
//    devPublication(projects.analysisKotlinDescriptorsIde)
//    devPublication(projects.analysisKotlinSymbols)
    devPublication(projects.analysisMarkdownJb)
    devPublication(projects.core)
    //devPublication(projects.coreContentMatcherTestUtils)
//    devPublication(projects.coreTestApi)
    devPublication(projects.dokkaCore)
    devPublication(projects.pluginAllModulesPage)
    devPublication(projects.pluginAndroidDocumentation)
    devPublication(projects.pluginBase)
//    devPublication(projects.pluginBaseFrontend)
    devPublication(projects.pluginBaseTestUtils)
    devPublication(projects.pluginGfm)
    devPublication(projects.pluginGfmTemplateProcessing)
    devPublication(projects.pluginJavadoc)
    devPublication(projects.pluginJekyll)
    devPublication(projects.pluginJekyllTemplateProcessing)
    devPublication(projects.pluginKotlinAsJava)
    devPublication(projects.pluginMathjax)
    devPublication(projects.pluginTemplating)
    devPublication(projects.pluginVersioning)
}
