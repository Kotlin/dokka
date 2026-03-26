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

public enum class KdNullability {
    NULLABLE,
    NOT_NULLABLE,
    DEFINITELY_NOT_NULLABLE,
    FLEXIBLE // e.g. platform types
}

// TODO: kind vs separate class vs flags - take a look on kotlin spec

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

// all kotlin targets (or platforms/families TBD) - should not be a enum really
public enum class KdTarget {
    JVM,
    JS,
    WASM_JS,
    WASM_WASI,
    MACOS_ARM64,
    MACOS_X64,
    IOS_ARM64,
    IOS_X64,
    IOS_SIMULATOR_ARM64,
    LINUX_ARM64,
    LINUX_X64,
    MINGW_X64,
    ANDROID_NATIVE_ARM32,
    ANDROID_NATIVE_ARM64,
    // and other targets ...
}
