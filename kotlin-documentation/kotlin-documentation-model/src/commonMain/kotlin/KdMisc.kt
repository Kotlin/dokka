/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

// TODO: we should think about how to expose enum-like things in Kotlin in respect to backward compatibility - we need open enums

public enum class KdVisibility {
    PUBLIC, PROTECTED, INTERNAL, PRIVATE,

    // java specific visibilities
    // should we prefix them with `JAVA_*`
    PACKAGE_PROTECTED, PACKAGE_PRIVATE
}

public enum class KdModality {
    FINAL, SEALED, OPEN, ABSTRACT;
    // non-sealed in java?
}

public enum class KdVariance {
    IN, OUT
}

public enum class KdActuality {
    ACTUAL, EXPECT
}

public enum class KdNullability {
    NULLABLE,
    NOT_NULLABLE,
    DEFINITELY_NOT_NULLABLE,
    FLEXIBLE // e.g. platform types
}

// TODO: kind vs separate class vs flags - take a look on kotlin spec
// TODO: probably replace those `kinds` with separate classes, so that for the json consumer, all entities will be represented as single `type` field in json

public enum class KdClassKind {
    CLASS, ENUM_CLASS, ANNOTATION_CLASS, OBJECT, INTERFACE,

    JAVA_RECORD // ???
}

public enum class KdFunctionKind {
    PRIMARY_CONSTRUCTOR, CONSTRUCTOR, FUNCTION
}

public enum class KdVariableKind {
    PROPERTY, FIELD/*JVM?*/, ENUM_ENTRY
}
