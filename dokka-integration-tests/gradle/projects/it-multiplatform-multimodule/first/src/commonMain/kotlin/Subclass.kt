/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package it.mpp.first

/**
 * This is an annotation which should be used for opt-in
 * e.g [SealedSerializationApi](https://github.com/Kotlin/kotlinx.serialization/blob/99be48514c1d0a975bb80d7bd37df429a9670064/core/commonMain/src/kotlinx/serialization/ApiLevels.kt#L50)
 */
@MustBeDocumented
@RequiresOptIn
annotation class SomeApi

/**
 * Class which should be somehow used by dependent module
 */
@OptIn(ExperimentalSubclassOptIn::class)
@SubclassOptInRequired(SomeApi::class)
interface Subclass
