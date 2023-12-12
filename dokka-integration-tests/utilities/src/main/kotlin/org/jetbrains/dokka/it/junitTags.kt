/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.it

import org.junit.jupiter.api.Tag
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.*


@Target(TYPE, FUNCTION, CLASS)
@Retention(RUNTIME)
@Tag("integration-test")
annotation class IntegrationTest
