/*
 * Copyright 2014-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.kotlin.documentation

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

public enum class KdSourceLanguage {
    KOTLIN, JAVA
    // C/OBJ_C - cinterop
    // TYPE_SCRIPT - dukat generated
}

public enum class KdVariance {
    IN, OUT
}

public enum class KdActuality {
    ACTUAL, EXPECT
}

// TODO: kind vs separate class vs flags - take a look on kotlin spec

public enum class KdClassKind {
    CLASS, ENUM_CLASS, ANNOTATION_CLASS, OBJECT, INTERFACE,

    JAVA_RECORD // ???
}

public enum class KdVariableKind {
    PROPERTY, FIELD, ENUM_ENTRY
}
