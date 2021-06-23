@file:Suppress("LocalVariableName")

package org.jetbrains

import org.gradle.api.Project

enum class DokkaPublicationChannel {
    SpaceDokkaDev,
    BintrayKotlinDev,
    BintrayKotlinEap,
    BintrayKotlinDokka,
    MavenCentral,
    MavenCentralSnapshot;

    val isSpaceRepository get() = this == SpaceDokkaDev

    val isBintrayRepository
        get() = when (this) {
            BintrayKotlinDev, BintrayKotlinEap, BintrayKotlinDokka -> true
            else -> false
        }

    val isMavenRepository
        get() = when (this) {
            MavenCentral, MavenCentralSnapshot -> true
            else -> false
        }

    val acceptedDokkaVersionTypes: List<DokkaVersionType>
        get() = when(this) {
            MavenCentral -> listOf(DokkaVersionType.Release)
            MavenCentralSnapshot -> listOf(DokkaVersionType.Snapshot)
            SpaceDokkaDev -> listOf(DokkaVersionType.Release, DokkaVersionType.Dev, DokkaVersionType.MC, DokkaVersionType.Snapshot)
            BintrayKotlinDev -> listOf(DokkaVersionType.Dev, DokkaVersionType.MC, DokkaVersionType.Snapshot)
            BintrayKotlinEap -> listOf(DokkaVersionType.MC)
            BintrayKotlinDokka -> listOf(DokkaVersionType.Release)
        }

    companion object {
        fun fromPropertyString(value: String): DokkaPublicationChannel = when (value) {
            "space-dokka-dev" -> SpaceDokkaDev
            "bintray-kotlin-dev" -> BintrayKotlinDev
            "bintray-kotlin-eap" -> BintrayKotlinEap
            "bintray-kotlin-dokka" -> BintrayKotlinDokka
            "maven-central-release" -> MavenCentral
            "maven-central-snapshot" -> MavenCentralSnapshot
            else -> throw IllegalArgumentException("Unknown dokka_publication_channel=$value")
        }
    }
}

val Project.publicationChannels: Set<DokkaPublicationChannel>
    get() {
        val publicationChannel = this.properties["dokka_publication_channel"]?.toString()
        val publicationChannels = this.properties["dokka_publication_channels"]?.toString()
        if (publicationChannel != null && publicationChannels != null) {
            throw IllegalArgumentException(
                "Only one of dokka_publication_channel and dokka_publication_channel*s* can be set. Found: \n" +
                        "dokka_publication_channel=$publicationChannel\n" +
                        "dokka_publication_channels=$publicationChannels"
            )
        }

        if (publicationChannel != null) {
            return setOf(DokkaPublicationChannel.fromPropertyString(publicationChannel))
        }

        if (publicationChannels != null) {
            return publicationChannels.split("&").map { channel ->
                DokkaPublicationChannel.fromPropertyString(channel)
            }.toSet()
        }

        return emptySet()
    }

