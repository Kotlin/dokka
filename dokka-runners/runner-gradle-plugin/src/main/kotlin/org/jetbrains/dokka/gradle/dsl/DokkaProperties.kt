/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.gradle.dsl

import org.gradle.api.provider.MapProperty

// API/DSL used to execute dokka
// TODO: add more type-safety (as in dokkatoo?)
//  the final value of property depends on a type, can be provider
@DokkaGradlePluginDsl
public interface DokkaProperties {
    public val properties: MapProperty<String, Any>

    public fun property(name: String, value: Any) {}

    // optional: creates via `ObjectFactory` = `objects.newInstance<DokkaProperties>
    // for nested properties
    public fun createPropertiesObject(block: DokkaProperties.() -> Unit = {}): DokkaProperties = error("")
}
