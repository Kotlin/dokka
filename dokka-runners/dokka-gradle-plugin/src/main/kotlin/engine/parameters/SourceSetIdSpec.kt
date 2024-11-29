/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
package org.jetbrains.dokka.gradle.engine.parameters

import org.gradle.api.Named
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.kotlin.dsl.newInstance
import org.jetbrains.dokka.gradle.internal.InternalDokkaGradlePluginApi
import java.io.Serializable
import javax.inject.Inject

/**
 * An identifier for a [DokkaSourceSetSpec].
 *
 * The identifiers must be distinct for all source sets across an entire Gradle build,
 * to ensure when Dokka Modules are aggregated into a Dokka Publication, the source sets can be
 * uniquely identified.
 */
abstract class SourceSetIdSpec
@InternalDokkaGradlePluginApi
@Inject
constructor(
    /**
     * Unique identifier of the scope that this source set is placed in.
     */
    @get:Input
    val scopeId: String,

    @get:Input
    val sourceSetName: String,
) : Named, Serializable {

    @Internal
    override fun getName(): String = "$scopeId/$sourceSetName"

    override fun toString(): String = "DokkaSourceSetIdSpec($scopeId/$sourceSetName)"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SourceSetIdSpec) return false

        if (scopeId != other.scopeId) return false
        return sourceSetName == other.sourceSetName
    }

    override fun hashCode(): Int {
        var result = scopeId.hashCode()
        result = 31 * result + sourceSetName.hashCode()
        return result
    }

    companion object {

        /** Utility for creating a new [SourceSetIdSpec] instance using [ObjectFactory.newInstance] */
        @InternalDokkaGradlePluginApi
        fun ObjectFactory.dokkaSourceSetIdSpec(
            scopeId: String,
            sourceSetName: String,
        ): SourceSetIdSpec = newInstance<SourceSetIdSpec>(scopeId, sourceSetName)
    }
}
