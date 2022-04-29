package org.jetbrains.dokka.base.templating

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdScalarSerializer
import com.fasterxml.jackson.databind.type.TypeFactory
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef
import org.jetbrains.dokka.base.DokkaBase
import java.io.File

// THIS IS COPIED FROM BASE SINCE IT NEEDS TO BE INSTANTIATED ON THE SAME CLASS LOADER AS PLUGINS

private val objectMapper = run {
    val module = SimpleModule().apply {
        addSerializer(FileSerializer)
    }
    jacksonObjectMapper()
        .apply {
            typeFactory = PluginTypeFactory()
        }
        .registerModule(module)
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
}

@PublishedApi
internal class TypeReference<T> private constructor(
    internal val jackson: com.fasterxml.jackson.core.type.TypeReference<T>
) {
    companion object {
        internal inline operator fun <reified T> invoke(): TypeReference<T> = TypeReference(jacksonTypeRef())
    }
}

fun toJsonString(value: Any): String = objectMapper.writeValueAsString(value)

inline fun <reified T : Any> parseJson(json: String): T = parseJson(json, TypeReference())

@PublishedApi
internal fun <T : Any> parseJson(json: String, typeReference: TypeReference<T>): T =
    objectMapper.readValue(json, typeReference.jackson)


private object FileSerializer : StdScalarSerializer<File>(File::class.java) {
    override fun serialize(value: File, g: JsonGenerator, provider: SerializerProvider) {
        g.writeString(value.path)
    }
}

@Suppress("DEPRECATION") // for TypeFactory constructor, no way to use non-deprecated one, it's essentially identical
private class PluginTypeFactory: TypeFactory(null) {
    override fun findClass(className: String): Class<out Any>? =
        Class.forName(className, true, DokkaBase::class.java.classLoader) ?: super.findClass(className)
}