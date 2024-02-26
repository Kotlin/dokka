/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.psi.PsiAnnotation
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.BreakingAbstractionKotlinLightMethodChecker
import org.jetbrains.dokka.analysis.java.JavaAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.DokkaAnalysisConfiguration
import org.jetbrains.dokka.analysis.kotlin.KotlinAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.ProjectKotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.*
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.moduledocs.ModuleAndPackageDocumentationReader
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.java.*
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.translator.DefaultDescriptorToDocumentableTranslator
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.translator.DescriptorExternalDocumentablesProvider
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin
import org.jetbrains.dokka.plugability.*
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation

@Suppress("unused")
@InternalDokkaApi
public class CompilerDescriptorAnalysisPlugin : DokkaPlugin() {

    @InternalDokkaApi
    public val kdocFinder: ExtensionPoint<KDocFinder> by extensionPoint()

    @InternalDokkaApi
    public val descriptorFinder: ExtensionPoint<DescriptorFinder> by extensionPoint()

    @InternalDokkaApi
    public val klibService: ExtensionPoint<KLibService> by extensionPoint()

    @InternalDokkaApi
    public val compilerExtensionPointProvider: ExtensionPoint<CompilerExtensionPointProvider> by extensionPoint()

    @InternalDokkaApi
    public val mockApplicationHack: ExtensionPoint<MockApplicationHack> by extensionPoint()

    @InternalDokkaApi
    public val analysisContextCreator: ExtensionPoint<AnalysisContextCreator> by extensionPoint()

    @InternalDokkaApi
    public val kotlinAnalysis: ExtensionPoint<KotlinAnalysis> by extensionPoint()

    internal val documentableAnalyzerImpl by extending {
        plugin<InternalKotlinAnalysisPlugin>().documentableSourceLanguageParser providing { CompilerDocumentableSourceLanguageParser() }
    }

    internal val defaultKotlinAnalysis by extending {
        @OptIn(DokkaPluginApiPreview::class)
        kotlinAnalysis providing { ctx ->
            val configuration = configuration<CompilerDescriptorAnalysisPlugin, DokkaAnalysisConfiguration>(ctx)
                ?: DokkaAnalysisConfiguration()
            ProjectKotlinAnalysis(
                sourceSets = ctx.configuration.sourceSets,
                context = ctx,
                analysisConfiguration = configuration
            )
        }
    }

    internal val descriptorToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing ::DefaultDescriptorToDocumentableTranslator
    }


    internal val descriptorFullClassHierarchyBuilder by extending {
        plugin<InternalKotlinAnalysisPlugin>().fullClassHierarchyBuilder providing { DescriptorFullClassHierarchyBuilder() }
    }

    internal val descriptorSampleAnalysisEnvironmentCreator by extending {
        plugin<KotlinAnalysisPlugin>().sampleAnalysisEnvironmentCreator providing ::DescriptorSampleAnalysisEnvironmentCreator
    }

    internal val descriptorSyntheticDocumentableDetector by extending {
        plugin<InternalKotlinAnalysisPlugin>().syntheticDocumentableDetector providing { DescriptorSyntheticDocumentableDetector() }
    }

    internal val moduleAndPackageDocumentationReader by extending {
        plugin<InternalKotlinAnalysisPlugin>().moduleAndPackageDocumentationReader providing ::ModuleAndPackageDocumentationReader
    }

    internal val kotlinToJavaMapper by extending {
        plugin<InternalKotlinAnalysisPlugin>().kotlinToJavaService providing { DescriptorKotlinToJavaMapper() }
    }

    internal val descriptorInheritanceBuilder by extending {
        plugin<InternalKotlinAnalysisPlugin>().inheritanceBuilder providing { DescriptorInheritanceBuilder() }
    }

    internal val descriptorExternalDocumentableProvider by extending {
        plugin<KotlinAnalysisPlugin>().externalDocumentableProvider providing ::DescriptorExternalDocumentablesProvider
    }

    private val javaAnalysisPlugin by lazy { plugin<JavaAnalysisPlugin>() }

    internal val projectProvider by extending {
        javaAnalysisPlugin.projectProvider providing { KotlinAnalysisProjectProvider() }
    }

    internal val sourceRootsExtractor by extending {
        javaAnalysisPlugin.sourceRootsExtractor providing { KotlinAnalysisSourceRootsExtractor() }
    }

    internal val kotlinDocCommentCreator by extending {
        javaAnalysisPlugin.docCommentCreators providing {
            DescriptorKotlinDocCommentCreator(querySingle { kdocFinder }, querySingle { descriptorFinder })
        }
    }

    internal val kotlinDocCommentParser by extending {
        javaAnalysisPlugin.docCommentParsers providing { context ->
            DescriptorKotlinDocCommentParser(
                context,
                context.logger
            )
        }
    }

    internal val inheritDocTagProvider by extending {
        javaAnalysisPlugin.inheritDocTagContentProviders providing ::KotlinInheritDocTagContentProvider
    }

    internal val kotlinLightMethodChecker by extending {
        javaAnalysisPlugin.kotlinLightMethodChecker providing {
            object : BreakingAbstractionKotlinLightMethodChecker {
                override fun isLightAnnotation(annotation: PsiAnnotation): Boolean {
                    return annotation is KtLightAbstractAnnotation
                }

                override fun isLightAnnotationAttribute(attribute: JvmAnnotationAttribute): Boolean {
                    return attribute is KtLightAbstractAnnotation
                }
            }
        }
    }

    internal val disposeKotlinAnalysisPostAction by extending {
        CoreExtensions.postActions with PostAction { querySingle { kotlinAnalysis }.close() }
    }

    internal val sourceRootIndependentChecker by extending {
        CoreExtensions.preGenerationCheck providing ::K1SourceRootIndependentChecker
    }

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
