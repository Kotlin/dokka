package org.jetbrains.dokka.gradle.internal

import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.gradle.AbstractDokkaTask
import org.jetbrains.dokka.toPrettyJsonString
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.toCompactJsonString

/**
 * Serializes [DokkaConfiguration] of this [AbstractDokkaTask] as json
 *
 * Should be used for short-term debugging only, no guarantees are given for the support of this API.
 *
 * Better alternative should be introduced as part of [#2873](https://github.com/Kotlin/dokka/issues/2873).
 */
@InternalDokkaApi
fun AbstractDokkaTask.buildJsonConfiguration(prettyPrint: Boolean = true): String {
    val configuration = this.buildDokkaConfiguration()
    return if (prettyPrint) {
        configuration.toPrettyJsonString()
    } else {
        configuration.toCompactJsonString()
    }
}
