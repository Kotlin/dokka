@file:Suppress("LocalVariableName")

package org.jetbrains

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.provideDelegate

fun RepositoryHandler.cachedJcenter(project: Project) {
    val dokka_jcenter_repository: String by project
    maven(dokka_jcenter_repository)
}

fun RepositoryHandler.cachedKotlinEap(project: Project){
    val dokka_kotlin_eap_repository: String by project
    maven(dokka_kotlin_eap_repository)
}

fun RepositoryHandler.cachedKotlinDev(project: Project){
    val dokka_kotlin_dev_repository: String by project
    maven(dokka_kotlin_dev_repository)
}

fun RepositoryHandler.cachedKotlinX(project: Project){
    val dokka_kotlinx_repository: String by project
    maven(dokka_kotlinx_repository)
}

fun RepositoryHandler.cachedKotlinPlugin(project: Project){
    val dokka_kotlin_plugin_repository: String by project
    maven(dokka_kotlin_plugin_repository)
}

