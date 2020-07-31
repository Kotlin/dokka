package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import java.net.URL


fun ExternalDocumentationLink.Companion.jdk(jdkVersion: Int): ExternalDocumentationLinkImpl {
    return ExternalDocumentationLink(
        url =
        if (jdkVersion < 11) "https://docs.oracle.com/javase/${jdkVersion}/docs/api/"
        else "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/java.base/",
        packageListUrl =
        if (jdkVersion < 11) "https://docs.oracle.com/javase/${jdkVersion}/docs/api/package-list"
        else "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/element-list"
    )
}

fun ExternalDocumentationLink.Companion.kotlinStdlib(): ExternalDocumentationLinkImpl {
    return ExternalDocumentationLink("https://kotlinlang.org/api/latest/jvm/stdlib/")
}

fun ExternalDocumentationLink.Companion.androidSdk(): ExternalDocumentationLinkImpl {
    return ExternalDocumentationLink("https://developer.android.com/reference/")
}

fun ExternalDocumentationLink.Companion.androidX(): ExternalDocumentationLinkImpl {
    return ExternalDocumentationLink(
        url = URL("https://developer.android.com/reference/kotlin/"),
        packageListUrl = URL("https://developer.android.com/reference/androidx/package-list")
    )
}
