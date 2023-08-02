package org.jetbrains.dokka.analysis.kotlin.descriptors.compiler

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.psi.PsiAnnotation
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.BreakingAbstractionKotlinLightMethodChecker
import org.jetbrains.dokka.analysis.java.JavaAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.configuration.ProjectKotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.*
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.impl.moduledocs.ModuleAndPackageDocumentationReader
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.java.*
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.translator.DefaultDescriptorToDocumentableTranslator
import org.jetbrains.dokka.analysis.kotlin.descriptors.compiler.translator.DefaultExternalDocumentablesProvider
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.dokka.plugability.querySingle
import org.jetbrains.dokka.renderers.PostAction
import org.jetbrains.dokka.analysis.kotlin.internal.InternalKotlinAnalysisPlugin
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation

@InternalDokkaApi
class CompilerDescriptorAnalysisPlugin : DokkaPlugin() {

    val kdocFinder by extensionPoint<KDocFinder>()

    val descriptorFinder by extensionPoint<DescriptorFinder>()

    val klibService by extensionPoint<KLibService>()

    val compilerExtensionPointProvider by extensionPoint<CompilerExtensionPointProvider>()

    val mockApplicationHack by extensionPoint<MockApplicationHack>()

    val analysisContextCreator by extensionPoint<AnalysisContextCreator>()

    val kotlinAnalysis by extensionPoint<KotlinAnalysis>()

    internal val documentableAnalyzerImpl by extending {
        plugin<InternalKotlinAnalysisPlugin>().documentableSourceLanguageParser providing { CompilerDocumentableSourceLanguageParser() }
    }

    internal  val defaultKotlinAnalysis by extending {
        kotlinAnalysis providing { ctx ->
            ProjectKotlinAnalysis(
                sourceSets = ctx.configuration.sourceSets,
                context = ctx
            )
        }
    }

    internal  val descriptorToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing ::DefaultDescriptorToDocumentableTranslator
    }

    internal val defaultSamplesTransformer by extending {
        CoreExtensions.pageTransformer providing ::DefaultSamplesTransformer
    }

    internal val descriptorFullClassHierarchyBuilder by extending {
        plugin<InternalKotlinAnalysisPlugin>().fullClassHierarchyBuilder providing { DescriptorFullClassHierarchyBuilder() }
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

    internal val defaultExternalDocumentablesProvider by extending {
        plugin<InternalKotlinAnalysisPlugin>().externalDocumentablesProvider providing ::DefaultExternalDocumentablesProvider
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

    @OptIn(DokkaPluginApiPreview::class)
    override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
