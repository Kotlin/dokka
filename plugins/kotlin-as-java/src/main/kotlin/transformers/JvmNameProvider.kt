package org.jetbrains.dokka.kotlinAsJava.transformers

import org.jetbrains.dokka.kotlinAsJava.directlyAnnotatedJvmName
import org.jetbrains.dokka.kotlinAsJava.fileLevelJvmName
import org.jetbrains.dokka.kotlinAsJava.jvmNameAsString
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.WithExtraProperties

data class Name(val fqName: String) {
    val name = fqName.substringAfterLast(".")
}

class JvmNameProvider {
    fun <T> nameFor(entry: T): String where T : Documentable, T : WithExtraProperties<out Documentable> =
        entry.directlyAnnotatedJvmName()?.jvmNameAsString()
            ?: entry.name
            ?: throw IllegalStateException("Failed to provide a name for ${entry.javaClass.canonicalName}")

    fun <T> nameForSyntheticClass(entry: T): Name where T : WithSources, T : WithExtraProperties<out Documentable>, T : Documentable {
        val name: String = (entry.fileLevelJvmName()?.params?.get("name") as? StringValue)?.value
            ?: entry.sources.entries.first().value.path.split("/").last().split(".").first().capitalize() + "Kt"
        return Name("${entry.dri.packageName}.$name")
    }

    fun nameForGetter(entry: DProperty): String? =
        entry.getter?.directlyAnnotatedJvmName()?.jvmNameAsString()

    fun nameForSetter(entry: DProperty): String? =
        entry.setter?.directlyAnnotatedJvmName()?.jvmNameAsString()

    private fun List<Annotations.Annotation>.jvmNameAnnotation(): Annotations.Annotation? =
        firstOrNull { it.isJvmName() }
}