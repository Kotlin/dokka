/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.internal

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.gradle.DOKKA_V1_DEPRECATION_MESSAGE
import org.jetbrains.dokka.toCompactJsonString
import org.jetbrains.dokka.toPrettyJsonString

/**
 * Serializes [DokkaConfiguration] of this [org.jetbrains.dokka.gradle.AbstractDokkaTask] as json
 *
 * Should be used for short-term debugging only, no guarantees are given for the support of this API.
 *
 * Better alternative should be introduced as part of [#2873](https://github.com/Kotlin/dokka/issues/2873).
 */
@InternalDokkaApi
@Deprecated(DOKKA_V1_DEPRECATION_MESSAGE)
fun @Suppress("DEPRECATION") org.jetbrains.dokka.gradle.AbstractDokkaTask.buildJsonConfiguration(prettyPrint: Boolean = true): String {
    val configuration = this.buildDokkaConfiguration()
    return if (prettyPrint) {
        configuration.toPrettyJsonString()
    } else {
        configuration.toCompactJsonString()
    }
}
