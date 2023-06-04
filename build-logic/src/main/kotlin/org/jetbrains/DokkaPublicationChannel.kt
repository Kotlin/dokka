@file:Suppress("LocalVariableName")

package org.jetbrains

import org.jetbrains.DokkaVersionType.*

enum class DokkaPublicationChannel {
    SpaceDokkaDev,
    MavenCentral {
        override val repositoryName = "sonatype"
    },
    MavenCentralSnapshot {
        //
        override val repositoryName = "sonatype"
    },
    GradlePluginPortal,
    MavenProjectLocal,
    ;

    open val repositoryName: String = name

    val acceptedDokkaVersionTypes: Set<DokkaVersionType>
        get() = when (this) {
            MavenCentral -> setOf(RELEASE, RC)
            MavenCentralSnapshot -> setOf(SNAPSHOT)
            SpaceDokkaDev -> setOf(RELEASE, RC, DEV, SNAPSHOT)
            GradlePluginPortal -> setOf(RELEASE, RC)
            MavenProjectLocal -> setOf(RELEASE, RC, DEV, SNAPSHOT)
        }

    fun isSpaceRepository() = this == SpaceDokkaDev

    fun isMavenRepository() = this == MavenCentral || this == MavenCentralSnapshot

    fun isGradlePluginPortal() = this == GradlePluginPortal

    companion object {
        fun fromPropertyString(value: String): DokkaPublicationChannel = when (value) {
            "space-dokka-dev" -> SpaceDokkaDev
            "maven-central-release" -> MavenCentral
            "maven-central-snapshot" -> MavenCentralSnapshot
            "gradle-plugin-portal" -> GradlePluginPortal
            "maven-project-local" -> MavenProjectLocal
            else -> throw IllegalArgumentException("Unknown dokka_publication_channel '$value'")
        }
    }
}
