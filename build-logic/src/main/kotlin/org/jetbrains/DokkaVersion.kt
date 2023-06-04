package org.jetbrains

import org.gradle.api.Project
import org.gradle.kotlin.dsl.dokkaBuild

// TODO migrate usages of these properties to use DokkaBuildProperties & Provider API

val Project.dokkaVersion: String
    get() = dokkaBuild.dokkaVersion.get()

val Project.dokkaVersionType: DokkaVersionType?
    get() = dokkaBuild.dokkaVersionType.orNull
