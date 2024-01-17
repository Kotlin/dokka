/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl.properties

import org.gradle.api.provider.MapProperty
import org.jetbrains.dokka.gradle.dsl.DokkaDelicateApi
import org.jetbrains.dokka.gradle.dsl.DokkaGradlePluginDsl

// API/DSL used to execute dokka
//@DokkaDelicateApi
@DokkaGradlePluginDsl
public interface DokkaProperties {
    // TODO: add more type-safety (as in dokkatoo)
    public val properties: MapProperty<String, String>
    public fun booleanProperty(name: String, value: Boolean)
    public fun intProperty(name: String, value: Int)
    public fun stringProperty(name: String, value: String)
    public fun fileProperty(name: String, value: Any) // file
    public fun fileCollectionProperty(name: String, value: Any) // files

    // arrays
    public fun booleanArrayProperty(name: String, vararg values: Boolean)
    public fun booleanArrayProperty(name: String, values: Array<Boolean>)
    public fun booleanArrayProperty(name: String, values: List<Boolean>)
    // ... for other primitives

    // objects
    public fun objectArrayProperty(name: String, value: MutableList<DokkaProperties.() -> Unit>.() -> Unit)
    public fun objectProperty(name: String, block: DokkaProperties.() -> Unit)
}

@DokkaGradlePluginDsl
public interface DokkaCustomPropertiesConfigurationContainer {
    @DokkaDelicateApi
    public fun customProperties(block: DokkaProperties.() -> Unit)
}

private fun DokkaProperties.s() {
    objectArrayProperty("something") {
        add {
            booleanProperty("123", false)
        }
    }
}
