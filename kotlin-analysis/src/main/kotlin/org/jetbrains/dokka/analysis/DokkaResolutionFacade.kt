@file:OptIn(FrontendInternals::class)

package org.jetbrains.dokka.analysis

import com.google.common.collect.ImmutableMap
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.ResolverForModule
import org.jetbrains.kotlin.analyzer.ResolverForProject
import org.jetbrains.kotlin.container.getService
import org.jetbrains.kotlin.container.tryGetService
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.idea.FrontendInternals
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import org.jetbrains.kotlin.util.slicedMap.WritableSlice

class DokkaResolutionFacade(
    override val project: Project,
    override val moduleDescriptor: ModuleDescriptor,
    val resolverForModule: ResolverForModule
) : ResolutionFacade {
    override fun analyzeWithAllCompilerChecks(
        elements: Collection<KtElement>,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {
        throw UnsupportedOperationException()
    }

    @OptIn(FrontendInternals::class)
    override fun <T : Any> tryGetFrontendService(element: PsiElement, serviceClass: Class<T>): T? {
        return resolverForModule.componentProvider.tryGetService(serviceClass)
    }

    override fun resolveToDescriptor(
        declaration: KtDeclaration,
        bodyResolveMode: BodyResolveMode
    ): DeclarationDescriptor {
        return resolveSession.resolveToDescriptor(declaration)
    }

    override fun analyze(elements: Collection<KtElement>, bodyResolveMode: BodyResolveMode): BindingContext {
        throw UnsupportedOperationException()
    }

    val resolveSession: ResolveSession get() = getFrontendService(ResolveSession::class.java)

    override fun analyze(element: KtElement, bodyResolveMode: BodyResolveMode): BindingContext {
        if (element is KtDeclaration) {
            val descriptor = resolveToDescriptor(element)
            return object : BindingContext {
                override fun <K : Any?, V : Any?> getKeys(p0: WritableSlice<K, V>?): Collection<K> {
                    throw UnsupportedOperationException()
                }

                override fun getType(p0: KtExpression): KotlinType? {
                    throw UnsupportedOperationException()
                }

                override fun <K : Any?, V : Any?> get(slice: ReadOnlySlice<K, V>?, key: K): V? {
                    if (key != element) {
                        throw UnsupportedOperationException()
                    }
                    return when {
                        slice == BindingContext.DECLARATION_TO_DESCRIPTOR -> descriptor as V
                        slice == BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER && (element as KtParameter).hasValOrVar() -> descriptor as V
                        else -> null
                    }
                }

                override fun getDiagnostics(): Diagnostics {
                    throw UnsupportedOperationException()
                }

                override fun addOwnDataTo(p0: BindingTrace, p1: Boolean) {
                    throw UnsupportedOperationException()
                }

                override fun <K : Any?, V : Any?> getSliceContents(p0: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
                    throw UnsupportedOperationException()
                }

            }
        }
        throw UnsupportedOperationException()
    }

    override fun <T : Any> getFrontendService(element: PsiElement, serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }

    override fun <T : Any> getFrontendService(serviceClass: Class<T>): T {
        return resolverForModule.componentProvider.getService(serviceClass)
    }

    override fun <T : Any> getFrontendService(moduleDescriptor: ModuleDescriptor, serviceClass: Class<T>): T {
        return resolverForModule.componentProvider.getService(serviceClass)
    }

    override fun <T : Any> getIdeService(serviceClass: Class<T>): T {
        throw UnsupportedOperationException()
    }

    override fun getResolverForProject(): ResolverForProject<out ModuleInfo> {
        throw UnsupportedOperationException()
    }

}
