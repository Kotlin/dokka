package org.jetbrains.dokka.Utilities

import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.ExternalDocumentationLinkImpl

fun DokkaConfiguration.PassConfiguration.defaultLinks(): List<ExternalDocumentationLinkImpl> {
    val links = mutableListOf<ExternalDocumentationLinkImpl>()
    if (!noJdkLink)
        links += DokkaConfiguration.ExternalDocumentationLink
            .Builder("https://docs.oracle.com/javase/${jdkVersion}/docs/api/")
            .build() as ExternalDocumentationLinkImpl

    if (!noStdlibLink)
        links += DokkaConfiguration.ExternalDocumentationLink
            .Builder("https://kotlinlang.org/api/latest/jvm/stdlib/")
            .build() as ExternalDocumentationLinkImpl
    return links
}
