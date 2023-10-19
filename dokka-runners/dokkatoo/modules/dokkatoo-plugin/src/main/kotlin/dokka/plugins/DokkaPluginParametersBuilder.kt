package org.jetbrains.dokka.dokkatoo.dokka.plugins

import org.jetbrains.dokka.dokkatoo.internal.DokkaPluginParametersContainer
import org.jetbrains.dokka.dokkatoo.internal.DokkatooInternalApi
import java.io.File
import javax.inject.Inject
import kotlinx.serialization.json.*
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.kotlin.dsl.*


/**
 * Dynamically create some configuration to control the behaviour of a Dokka Plugin.
 *
 * @param[pluginFqn] The fully-qualified name of a Dokka Plugin. For example, the FQN of the
 * [Dokka Base plugin](https://github.com/Kotlin/dokka/tree/master/plugins/base#readme)
 * is `org.jetbrains.dokka.base.DokkaBase`
 */
fun DokkaPluginParametersContainer.pluginParameters(
  pluginFqn: String,
  configure: DokkaPluginParametersBuilder.() -> Unit
) {
  containerWithType(DokkaPluginParametersBuilder::class)
    .maybeCreate(pluginFqn)
    .configure()
}


/**
 * Dynamically create some configuration to control the behaviour of a Dokka Plugin.
 *
 * This type of builder is necessary to respect
 * [Gradle incremental build annotations](https://docs.gradle.org/current/userguide/incremental_build.html#sec:task_input_output_annotations).
 *
 * @param[pluginFqn] The fully-qualified name of a Dokka Plugin. For example, the Dokka Base plugin's FQN is `org.jetbrains.dokka.base.DokkaBase`
 */
abstract class DokkaPluginParametersBuilder
@Inject
@DokkatooInternalApi
constructor(
  name: String,
  @get:Input
  override val pluginFqn: String,

  @Internal
  internal val objects: ObjectFactory,
) : DokkaPluginParametersBaseSpec(name, pluginFqn) {

  @get:Nested
  internal val properties = PluginConfigValue.Properties(objects.mapProperty())

  @Internal
  override fun jsonEncode(): String = properties.convertToJson().toString()

  companion object {
    private fun PluginConfigValue.convertToJson(): JsonElement =
      when (this) {
        is PluginConfigValue.DirectoryValue -> directory.asFile.orNull.convertToJson()
        is PluginConfigValue.FileValue      -> file.asFile.orNull.convertToJson()
        is PluginConfigValue.FilesValue     -> JsonArray(files.files.map { it.convertToJson() })

        is PluginConfigValue.BooleanValue   -> JsonPrimitive(boolean)
        is PluginConfigValue.NumberValue    -> JsonPrimitive(number)
        is PluginConfigValue.StringValue    -> JsonPrimitive(string)

        is PluginConfigValue.Properties     ->
          JsonObject(values.get().mapValues { (_, value) -> value.convertToJson() })

        is PluginConfigValue.Values         ->
          JsonArray(values.get().map { it.convertToJson() })
      }

    /** Creates a [JsonPrimitive] from the given [File]. */
    private fun File?.convertToJson(): JsonPrimitive =
      JsonPrimitive(this?.canonicalFile?.invariantSeparatorsPath)
  }
}


fun DokkaPluginParametersBuilder.files(
  propertyName: String,
  filesConfig: ConfigurableFileCollection.() -> Unit
) {
  val files = objects.fileCollection()
  files.filesConfig()
  properties.values.put(propertyName, PluginConfigValue.FilesValue(files))
}

//region Primitive Properties
fun DokkaPluginParametersBuilder.property(propertyName: String, value: String) {
  properties.values.put(propertyName, PluginConfigValue(value))
}

fun DokkaPluginParametersBuilder.property(propertyName: String, value: Number) {
  properties.values.put(propertyName, PluginConfigValue(value))
}

fun DokkaPluginParametersBuilder.property(propertyName: String, value: Boolean) {
  properties.values.put(propertyName, PluginConfigValue(value))
}

@JvmName("stringProperty")
fun DokkaPluginParametersBuilder.property(propertyName: String, provider: Provider<String>) {
  properties.values.put(propertyName, provider.map { PluginConfigValue(it) })
}

@JvmName("numberProperty")
fun DokkaPluginParametersBuilder.property(propertyName: String, provider: Provider<Number>) {
  properties.values.put(propertyName, provider.map { PluginConfigValue(it) })
}

@JvmName("booleanProperty")
fun DokkaPluginParametersBuilder.property(
  propertyName: String,
  provider: Provider<Boolean>
) {
  properties.values.put(propertyName, provider.map { PluginConfigValue(it) })
}
//endregion


//region List Properties
fun DokkaPluginParametersBuilder.properties(
  propertyName: String,
  build: PluginConfigValue.Values.() -> Unit
) {
  val values = PluginConfigValue.Values(objects.listProperty())
  values.build()
  properties.values.put(propertyName, values)
}

fun PluginConfigValue.Values.add(value: String) =
  values.add(PluginConfigValue(value))

fun PluginConfigValue.Values.add(value: Number) =
  values.add(PluginConfigValue(value))

fun PluginConfigValue.Values.add(value: Boolean) =
  values.add(PluginConfigValue(value))

@JvmName("addString")
fun PluginConfigValue.Values.add(value: Provider<String>) =
  values.add(PluginConfigValue(value))

@JvmName("addNumber")
fun PluginConfigValue.Values.add(value: Provider<Number>) =
  values.add(PluginConfigValue(value))

@JvmName("addBoolean")
fun PluginConfigValue.Values.add(value: Provider<Boolean>) =
  values.add(PluginConfigValue(value))
//endregion


sealed interface PluginConfigValue {

  /** An input file */
  class FileValue(
    @InputFile
    @PathSensitive(RELATIVE)
    val file: RegularFileProperty,
  ) : PluginConfigValue

  /** Input files and directories */
  class FilesValue(
    @InputFiles
    @PathSensitive(RELATIVE)
    val files: ConfigurableFileCollection,
  ) : PluginConfigValue

  /** An input directory */
  class DirectoryValue(
    @InputDirectory
    @PathSensitive(RELATIVE)
    val directory: DirectoryProperty,
  ) : PluginConfigValue

  /** Key-value properties. Analogous to a [JsonObject]. */
  class Properties(
    @Nested
    val values: MapProperty<String, PluginConfigValue>
  ) : PluginConfigValue

  /** Multiple values. Analogous to a [JsonArray]. */
  class Values(
    @Nested
    val values: ListProperty<PluginConfigValue>
  ) : PluginConfigValue

  sealed interface Primitive : PluginConfigValue

  /** A basic [String] value */
  class StringValue(@Input val string: String) : Primitive

  /** A basic [Number] value */
  class NumberValue(@Input val number: Number) : Primitive

  /** A basic [Boolean] value */
  class BooleanValue(@Input val boolean: Boolean) : Primitive
}

fun PluginConfigValue(value: String) =
  PluginConfigValue.StringValue(value)

fun PluginConfigValue(value: Number) =
  PluginConfigValue.NumberValue(value)

fun PluginConfigValue(value: Boolean) =
  PluginConfigValue.BooleanValue(value)

@Suppress("FunctionName")
@JvmName("PluginConfigStringValue")
fun PluginConfigValue(value: Provider<String>): Provider<PluginConfigValue.StringValue> =
  value.map { PluginConfigValue(it) }

@Suppress("FunctionName")
@JvmName("PluginConfigNumberValue")
fun PluginConfigValue(value: Provider<Number>): Provider<PluginConfigValue.NumberValue> =
  value.map { PluginConfigValue(it) }

@Suppress("FunctionName")
@JvmName("PluginConfigBooleanValue")
fun PluginConfigValue(value: Provider<Boolean>): Provider<PluginConfigValue.BooleanValue> =
  value.map { PluginConfigValue(it) }
