package org.jetbrains.dokka

import org.jetbrains.kotlin.cli.common.arguments.*
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.utils.*
import java.io.*
import org.jetbrains.kotlin.resolve.jvm.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.context.GlobalContext
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

fun KotlinCoreEnvironment.analyze(): ResolveSession {
    val projectContext = ProjectContext(project)
    val sourceFiles = getSourceFiles()

    val module = object : ModuleInfo {
        override val name: Name = Name.special("<module>")
        override fun dependencies(): List<ModuleInfo> = listOf(this)
    }
    val resolverForProject = JvmAnalyzerFacade.setupResolverForProject(
            projectContext,
            listOf(module),
            { ModuleContent(sourceFiles, GlobalSearchScope.allScope(project)) },
            JvmPlatformParameters { module }
    )
    return resolverForProject.resolverForModule(module).lazyResolveSession
}
