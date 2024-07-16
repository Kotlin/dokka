package dev.adamko.dokkatoo.dependencies

import org.gradle.api.Named
import org.gradle.api.attributes.*
import org.gradle.api.attributes.Bundling.BUNDLING_ATTRIBUTE
import org.gradle.api.attributes.Category.CATEGORY_ATTRIBUTE
import org.gradle.api.attributes.LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE
import org.gradle.api.attributes.Usage.USAGE_ATTRIBUTE
import org.gradle.api.attributes.java.TargetJvmEnvironment
import org.gradle.api.attributes.java.TargetJvmEnvironment.TARGET_JVM_ENVIRONMENT_ATTRIBUTE
import org.gradle.kotlin.dsl.*

/**
 * Dumb hack to work around Gradle bugs/issues/deficiencies.
 *
 * Basically, even though a [Configuration][org.gradle.api.artifacts.Configuration] might specify
 * some [Attribute]s, Gradle can, in some situations, randomly ignore them, leading to
 * files leaking between Configurations unexpectedly. This is a particular problem with JARs.
 * Dokkatoo needs to both resolve JARs from Maven Central, and also provide JARs to other
 * subprojects.
 *
 * To work around this:
 *
 * 1. When requesting or providing attributes, Dokkatoo adds a prefix ([AttributeHackPrefix]) to
 *   the JAR specific Attribute values.
 * 2. When Dokkatoo shares files, the prefix prevents Gradle from getting confused with other
 *   Configurations with similar Attributes.
 * 3. Dokkatoo adds some [AttributeCompatibilityRule]s for the JAR attributes, so that Dokkatoo
 *   can ignore the prefix when **consuming**.
 */
internal abstract class AttributeHackCompatibilityRule<T : Named> : AttributeCompatibilityRule<T> {
  override fun execute(details: CompatibilityCheckDetails<T>): Unit = details.run {
    val consumerName = consumerValue?.name?.substringAfter(AttributeHackPrefix) ?: return
    val producerName = producerValue?.name?.substringAfter(AttributeHackPrefix) ?: return
    if (consumerName == producerName) {
      compatible()
    }
  }
}

internal const val AttributeHackPrefix = "Dokkatoo~"

internal class UsageHackRule : AttributeHackCompatibilityRule<Usage>()
internal class CategoryHackRule : AttributeHackCompatibilityRule<Category>()
internal class BundlingHackRule : AttributeHackCompatibilityRule<Bundling>()
internal class TargetJvmEnvironmentHackRule : AttributeHackCompatibilityRule<TargetJvmEnvironment>()
internal class LibraryElementsHackRule : AttributeHackCompatibilityRule<LibraryElements>()

/**
 * @see AttributeHackCompatibilityRule
 */
internal fun DependencyHandlerScope.applyAttributeHacks() {
  attributesSchema {
    attribute(USAGE_ATTRIBUTE) {
      compatibilityRules.add(UsageHackRule::class)
    }
    attribute(CATEGORY_ATTRIBUTE) {
      compatibilityRules.add(CategoryHackRule::class)
    }
    attribute(BUNDLING_ATTRIBUTE) {
      compatibilityRules.add(BundlingHackRule::class)
    }
    attribute(TARGET_JVM_ENVIRONMENT_ATTRIBUTE) {
      compatibilityRules.add(TargetJvmEnvironmentHackRule::class)
    }
    attribute(LIBRARY_ELEMENTS_ATTRIBUTE) {
      compatibilityRules.add(LibraryElementsHackRule::class)
    }
  }
}
