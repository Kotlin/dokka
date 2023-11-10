/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.resolve

import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

internal class CommonKlibModuleInfo(
    override val name: Name,
    val kotlinLibrary: KotlinLibrary,
    private val dependOnModules: List<ModuleInfo>
) : ModuleInfo {
    override fun dependencies(): List<ModuleInfo> = dependOnModules

    override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns = ModuleInfo.DependencyOnBuiltIns.LAST

    override val platform: TargetPlatform
        get() = CommonPlatforms.defaultCommonPlatform

    override val analyzerServices: PlatformDependentAnalyzerServices
        get() = CommonPlatformAnalyzerServices
}
