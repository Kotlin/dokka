/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka

import org.jetbrains.dokka.DokkaConfiguration.ExternalDocumentationLink
import java.net.URL


public fun ExternalDocumentationLink.Companion.jdk(jdkVersion: Int): ExternalDocumentationLinkImpl =
    ExternalDocumentationLink(
        url =
        if (jdkVersion < 11) "https://docs.oracle.com/javase/${jdkVersion}/docs/api/"
        else "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/",
        packageListUrl =
        if (jdkVersion < 11) "https://docs.oracle.com/javase/${jdkVersion}/docs/api/package-list"
        else "https://docs.oracle.com/en/java/javase/${jdkVersion}/docs/api/element-list"
    )


public fun ExternalDocumentationLink.Companion.kotlinStdlib(): ExternalDocumentationLinkImpl =
    ExternalDocumentationLink("https://kotlinlang.org/api/latest/jvm/stdlib/")


public fun ExternalDocumentationLink.Companion.androidSdk(): ExternalDocumentationLinkImpl =
    ExternalDocumentationLink("https://developer.android.com/reference/kotlin/")


public fun ExternalDocumentationLink.Companion.androidX(): ExternalDocumentationLinkImpl = ExternalDocumentationLink(
    url = URL("https://developer.android.com/reference/kotlin/"),
    packageListUrl = URL("https://developer.android.com/reference/kotlin/androidx/package-list")
)
