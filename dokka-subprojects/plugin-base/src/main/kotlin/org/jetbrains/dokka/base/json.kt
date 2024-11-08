/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.base

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import com.fasterxml.jackson.module.kotlin.jsonMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.properties.ExtraProperty
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.pages.HtmlContent
import org.jetbrains.dokka.toCompactJsonString
import org.jetbrains.dokka.toPrettyJsonString
import org.jetbrains.dokka.utilities.CORE_OBJECT_MAPPER
import java.io.File

internal fun DModule.dumpToJson(
    configuration: DokkaConfiguration
) {
    val mapper = jsonMapper {
        addModule(kotlinModule())
        addModule(SimpleModule().apply {
            addSerializer<DRI> {
                writeString(it.toString())
            }
            addSerializer<DocumentableSource> {
                when (val lineNumber = it.computeLineNumber()) {
                    null -> writeNull()
                    else -> writeString("${it.path}:$lineNumber")
                }
            }
            addSerializer<Visibility> {
                writeString(
                    when (it) {
                        is JavaVisibility -> "java:${it.name}"
                        is KotlinVisibility -> "kotlin:${it.name}"
                    }
                )
            }
            addSerializer<Modifier> {
                writeString(
                    when (it) {
                        is JavaModifier -> "java:${it.name}"
                        is KotlinModifier -> "kotlin:${it.name}"
                    }
                )
            }
            addSerializer<ExtraModifiers> {
                writeString(
                    when (it) {
                        is ExtraModifiers.JavaOnlyModifiers -> "java:${it.name}"
                        is ExtraModifiers.KotlinOnlyModifiers -> "kotlin:${it.name}"
                    }
                )
            }

            // extras
            addKeySerializer<ExtraProperty.Key<*, *>> {
                writeFieldName(it::class.java.name.substringBeforeLast("\$Companion").substringAfterLast("."))
            }
            // jackson can't serialize an object which references itself
            addSerializer<PrimaryConstructorExtra> { writeString("PrimaryConstructorExtra") }
            addSerializer<IsVar> { writeString("IsVar") }
            addSerializer<ObviousMember> { writeString("ObviousMember") }
            addSerializer<HtmlContent> { writeString("HtmlContent") }

            // remove additional nesting of map field in extra fields
            addSerializer<PropertyContainer<*>> { writeObject(it.map) }
            // extras

            // sourceset
            addSerializer<DokkaConfiguration.DokkaSourceSet> { writeString(it.sourceSetID.toString()) }
            addKeySerializer<DokkaConfiguration.DokkaSourceSet> { writeFieldName(it.sourceSetID.toString()) }
            // sourceset
        })
        serializationInclusion(JsonInclude.Include.NON_EMPTY)
    }

    val moduleSchema = JsonSchemaGenerator(mapper).generateSchema(DModule::class.java)
    val configurationSchema = JsonSchemaGenerator(CORE_OBJECT_MAPPER).generateSchema(DokkaConfiguration::class.java)

    println("DOKKA_JSON_DUMP: $name")
    val root = File("build/dokka-ir-poc")
    root.mkdirs()
    mapper
        .writeValue(root.resolve("$name-ir.json"), this)
    mapper.writerWithDefaultPrettyPrinter()
        .writeValue(root.resolve("$name-ir-pretty.json"), this)

    root.resolve("$name-configuration.json").writeText(configuration.toCompactJsonString())
    root.resolve("$name-configuration-pretty.json").writeText(configuration.toPrettyJsonString())

    // schema is the same for any module
    mapper
        .writeValue(root.resolve("$name-ir-schema.json"), moduleSchema)
    mapper.writerWithDefaultPrettyPrinter()
        .writeValue(root.resolve("$name-ir-schema-pretty.json"), moduleSchema)
    mapper
        .writeValue(root.resolve("$name-configuration-schema.json"), configurationSchema)
    mapper.writerWithDefaultPrettyPrinter()
        .writeValue(root.resolve("$name-configuration-schema-pretty.json"), configurationSchema)
    println("DOKKA_JSON_DUMP: DONE")
}

private inline fun <reified T> SimpleModule.addSerializer(crossinline block: JsonGenerator.(value: T) -> Unit) {
    addSerializer(T::class.java, serializer(block))
}

private inline fun <reified T> SimpleModule.addKeySerializer(crossinline block: JsonGenerator.(value: T) -> Unit) {
    addKeySerializer(T::class.java, serializer(block))
}

private inline fun <reified T> serializer(crossinline block: JsonGenerator.(value: T) -> Unit): JsonSerializer<T> {
    return object : StdSerializer<T>(T::class.java) {
        override fun serialize(value: T, gen: JsonGenerator, provider: SerializerProvider): Unit = gen.block(value)
    }
}
