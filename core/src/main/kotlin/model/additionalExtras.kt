package org.jetbrains.dokka.model

import com.google.common.collect.ArrayListMultimap
import com.google.common.collect.Multimap
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.MergeStrategy
import org.jetbrains.dokka.model.properties.WithExtraProperties

class AdditionalModifiers(val content: SourceSetDependent<Set<ExtraModifiers>>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, AdditionalModifiers> {
        override fun mergeStrategyFor(
            left: AdditionalModifiers,
            right: AdditionalModifiers
        ): MergeStrategy<Documentable> = MergeStrategy.Replace(AdditionalModifiers(left.content + right.content))
    }

    override fun equals(other: Any?): Boolean =
        if (other is AdditionalModifiers) other.content == content else false

    override fun hashCode() = content.hashCode()
    override val key: ExtraProperty.Key<Documentable, *> = AdditionalModifiers
}

fun SourceSetDependent<Set<ExtraModifiers>>.toAdditionalModifiers() = AdditionalModifiers(this)

fun WithExtraProperties<out Documentable>.hasAdditionalModifier(modifier: ExtraModifiers): SourceSetDependent<Boolean>? {
    return extra[AdditionalModifiers]?.content?.mapValues { (_, modifiers) ->
        modifier in modifiers
    }
}

class Annotations(val content: SourceSetDependent<List<Annotation>>) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, Annotations> {
        override fun mergeStrategyFor(left: Annotations, right: Annotations): MergeStrategy<Documentable> =
            MergeStrategy.Replace(Annotations(left.content + right.content))
    }

    override val key: ExtraProperty.Key<Documentable, *> = Annotations

    data class Annotation(val dri: DRI, val params: Map<String, AnnotationParameterValue>, val mustBeDocumented: Boolean = false) {
        override fun equals(other: Any?): Boolean = when (other) {
            is Annotation -> dri == other.dri
            else -> false
        }

        override fun hashCode(): Int = dri.hashCode()
    }
}

fun SourceSetDependent<List<Annotations.Annotation>>.toAnnotations() = Annotations(this)

fun WithExtraProperties<out Documentable>.hasAnnotation(name: String): SourceSetDependent<Boolean>? {
    return extra[Annotations]?.content?.mapValues { (_, annotations) ->
        annotations.any { annotation -> annotation.dri.classNames == name }
    }
}

sealed class AnnotationParameterValue
data class AnnotationValue(val annotation: Annotations.Annotation) : AnnotationParameterValue()
data class ArrayValue(val value: List<AnnotationParameterValue>) : AnnotationParameterValue()
data class EnumValue(val enumName: String, val enumDri: DRI) : AnnotationParameterValue()
data class ClassValue(val className: String, val classDRI: DRI) : AnnotationParameterValue()
data class StringValue(val value: String) : AnnotationParameterValue() {
    val unquotedValue: String
        get() = if (value.startsWith('"') && value.endsWith('"')) {
            if (value.length == 2) "" else value.substring(1, value.lastIndex)
        } else {
            value
        }
}


object PrimaryConstructorExtra : ExtraProperty<DFunction>, ExtraProperty.Key<DFunction, PrimaryConstructorExtra> {
    override val key: ExtraProperty.Key<DFunction, *> = this
}

data class ActualTypealias(val underlyingType: SourceSetDependent<Bound>) : ExtraProperty<DClasslike> {
    companion object : ExtraProperty.Key<DClasslike, ActualTypealias> {
        override fun mergeStrategyFor(
            left: ActualTypealias,
            right: ActualTypealias
        ) =
            MergeStrategy.Replace(ActualTypealias(left.underlyingType + right.underlyingType))
    }

    override val key: ExtraProperty.Key<DClasslike, ActualTypealias> = ActualTypealias
}

data class ConstructorValues(val values: SourceSetDependent<List<String>>) : ExtraProperty<DEnumEntry>{
    companion object : ExtraProperty.Key<DEnumEntry, ConstructorValues> {
        override fun mergeStrategyFor(left: ConstructorValues, right: ConstructorValues) =
            MergeStrategy.Replace(ConstructorValues(left.values + right.values))
    }

    override val key: ExtraProperty.Key<DEnumEntry, ConstructorValues> = ConstructorValues
}

fun SourceSetDependent<Multimap<DocumentableSource, Annotations.Annotation>>.toFileAnnotations() =
    FileAnnotations(this)

data class FileAnnotations(val content: SourceSetDependent<Multimap<DocumentableSource, Annotations.Annotation>>) : ExtraProperty<DPackage> {
    companion object : ExtraProperty.Key<DPackage, FileAnnotations> {
        override fun mergeStrategyFor(left: FileAnnotations, right: FileAnnotations): MergeStrategy<DPackage> {
            val content = left.content.toMutableMap()
            right.content.forEach { (sourceSet, fileAnnotations) ->
                val existing = content[sourceSet]
                if (existing == null) {
                    content[sourceSet] = fileAnnotations
                } else {
                    content[sourceSet] = existing + fileAnnotations
                }
            }
            return MergeStrategy.Replace(FileAnnotations(content))
        }
    }

    override val key: ExtraProperty.Key<DPackage, *> = FileAnnotations
}

private operator fun <K, V> Multimap<out K, V>.plus(multimap: Multimap<K, V>): Multimap<K, V> =
    ArrayListMultimap.create(this).apply { putAll(multimap) }

data class FromCompanionObject(val container: WithCompanion) : ExtraProperty<Documentable> {
    companion object : ExtraProperty.Key<Documentable, FromCompanionObject> {
        override fun mergeStrategyFor(left: FromCompanionObject,right: FromCompanionObject) = MergeStrategy.Remove
    }

    override val key: ExtraProperty.Key<Documentable, *> = FromCompanionObject
}