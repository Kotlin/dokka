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
}

internal val Project.publicationChannel: DokkaPublicationChannel
    get() {
        val dokka_publication_channel: String by this
        return when (dokka_publication_channel) {
            "space-dokka-dev" -> SpaceDokkaDev
            "bintray-kotlin-dev" -> BintrayKotlinDev
            "bintray-kotlin-eap" -> BintrayKotlinEap
            "bintray-kotlin-dokka" -> BintrayKotlinDokka
            else -> throw GradleException("Unknown dokka_publication_channel=$dokka_publication_channel")
        }
    }
