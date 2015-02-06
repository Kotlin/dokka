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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.lazy.ResolveSession

private fun getAnnotationsPath(paths: KotlinPaths, arguments: K2JVMCompilerArguments): MutableList<File> {
    val annotationsPath = arrayListOf<File>()
    annotationsPath.add(paths.getJdkAnnotationsPath())
    val annotationPaths = arguments.annotations
    if (annotationPaths != null) {
        for (element in annotationPaths.split(File.pathSeparatorChar)) {
            annotationsPath.add(File(element))
        }
    }
    return annotationsPath
}

fun JetCoreEnvironment.analyze(): ResolveSession {
    val globalContext = GlobalContext()
    val project = getProject()
    val sourceFiles = getSourceFiles()

    val module = object : ModuleInfo {
        override val name: Name = Name.special("<module>")
        override fun dependencies(): List<ModuleInfo> = listOf(this)
    }
    val resolverForProject = JvmAnalyzerFacade.setupResolverForProject(
            globalContext,
            project,
            listOf(module),
            { ModuleContent(sourceFiles, GlobalSearchScope.allScope(project)) },
            JvmPlatformParameters { module }
    )
    return resolverForProject.resolverForModule(module).lazyResolveSession
}

fun DeclarationDescriptor.isUserCode() =
        when (this) {
            is PackageViewDescriptor -> false
            is PackageFragmentDescriptor -> false
            is PropertyAccessorDescriptor -> !isDefault()
            is CallableMemberDescriptor -> getKind() == CallableMemberDescriptor.Kind.DECLARATION
            else -> true
        }
