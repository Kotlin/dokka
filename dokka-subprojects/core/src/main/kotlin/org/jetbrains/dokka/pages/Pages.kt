/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.pages

public interface MultimoduleRootPage : ContentPage

public interface ModulePage : ContentPage, WithDocumentables

public interface PackagePage : ContentPage, WithDocumentables

public interface ClasslikePage : ContentPage, WithDocumentables

public interface MemberPage : ContentPage, WithDocumentables
