@file:Suppress("LocalVariableName")

package org.jetbrains

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.DokkaPublicationChannel.*

internal enum class DokkaPublicationChannel {
    SpaceDokkaDev,
    BintrayKotlinDev,
    BintrayKotlinEap,
    BintrayKotlinDokka;

    val isSpaceRepository get() = this == SpaceDokkaDev

    val isBintrayRepository
        get() = when (this) {
            SpaceDokkaDev -> false
            BintrayKotlinDev, BintrayKotlinEap, BintrayKotlinDokka -> true
        }

    companion object {
        fun fromPropertyString(value: String): DokkaPublicationChannel = when (value) {
            "space-dokka-dev" -> SpaceDokkaDev
            "bintray-kotlin-dev" -> BintrayKotlinDev
            "bintray-kotlin-eap" -> BintrayKotlinEap
            "bintray-kotlin-dokka" -> BintrayKotlinDokka
            else -> throw IllegalArgumentException("Unknown dokka_publication_channel=$value")
        }
    }
}

internal val Project.publicationChannels: Set<DokkaPublicationChannel>
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

