/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.kotlinAsJava.transformers

import org.jetbrains.dokka.kotlinAsJava.directlyAnnotatedJvmName
import org.jetbrains.dokka.kotlinAsJava.fileLevelJvmName
import org.jetbrains.dokka.kotlinAsJava.jvmNameAsString
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties

public data class Name(val fqName: String) {
    val name: String = fqName.substringAfterLast(".")
}

public class JvmNameProvider {
    public fun <T> nameFor(entry: T): String where T : Documentable, T : WithExtraProperties<T> =
        entry.directlyAnnotatedJvmName()?.jvmNameAsString()
            ?: entry.name
            ?: throw IllegalStateException("Failed to provide a name for ${entry.javaClass.canonicalName}")

    public fun <T> nameForSyntheticClass(entry: T): Name where T : WithSources, T : WithExtraProperties<T>, T : Documentable {
        val name: String = (entry.fileLevelJvmName()?.params?.get("name") as? StringValue)?.value
            ?: (entry.sources.entries.first().value.path.split("/").last().split(".").first().capitalize() + "Kt")
        return Name("${entry.dri.packageName}.$name")
    }

    public fun nameForGetter(entry: DProperty): String? =
        entry.getter?.directlyAnnotatedJvmName()?.jvmNameAsString()

    public fun nameForSetter(entry: DProperty): String? =
        entry.setter?.directlyAnnotatedJvmName()?.jvmNameAsString()

    private fun List<Annotations.Annotation>.jvmNameAnnotation(): Annotations.Annotation? =
        firstOrNull { it.isJvmName() }
}
