package org.jetbrains.dokka.analysis.kotlin.symbols.compiler

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.psi.PsiAnnotation
import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.analysis.java.BreakingAbstractionKotlinLightMethodChecker
import org.jetbrains.dokka.analysis.java.JavaAnalysisPlugin
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java.KotlinInheritDocTagContentProvider
import org.jetbrains.dokka.analysis.kotlin.symbols.KotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.ProjectKotlinAnalysis
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java.DescriptorKotlinDocCommentCreator
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.java.KotlinDocCommentParser
import org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs.ModuleAndPackageDocumentationReader
import org.jetbrains.dokka.analysis.kotlin.symbols.services.DefaultSamplesTransformer
import org.jetbrains.dokka.analysis.kotlin.symbols.services.SymbolExternalDocumentablesProvider
import org.jetbrains.dokka.analysis.kotlin.symbols.services.SymbolFullClassHierarchyBuilder
import org.jetbrains.dokka.analysis.kotlin.symbols.translators.DefaultSymbolToDocumentableTranslator
import org.jetbrains.dokka.plugability.DokkaPlugin
import org.jetbrains.dokka.plugability.DokkaPluginApiPreview
import org.jetbrains.dokka.plugability.PluginApiPreviewAcknowledgement
import org.jetbrains.kotlin.analysis.kotlin.internal.InternalKotlinAnalysisPlugin
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation

@Suppress("unused")
class SymbolsAnalysisPlugin : DokkaPlugin() {

    val kotlinAnalysis by extensionPoint<KotlinAnalysis>()

    internal  val defaultKotlinAnalysis by extending {
        kotlinAnalysis providing { ctx ->
            ProjectKotlinAnalysis(
                sourceSets = ctx.configuration.sourceSets,
                context = ctx
            )
        }
    }


    val symbolToDocumentableTranslator by extending {
        CoreExtensions.sourceToDocumentableTranslator providing ::DefaultSymbolToDocumentableTranslator
    }




    private val javaAnalysisPlugin by lazy { plugin<JavaAnalysisPlugin>() }

    internal val projectProvider by extending {
        javaAnalysisPlugin.projectProvider providing { KotlinAnalysisProjectProvider() }
    }
/*
    internal val sourceRootsExtractor by extending {
        javaAnalysisPlugin.sourceRootsExtractor providing { KotlinAnalysisSourceRootsExtractor() }
    }
*/
    internal val kotlinDocCommentCreator by extending {
        javaAnalysisPlugin.docCommentCreators providing {
            DescriptorKotlinDocCommentCreator()
        }
    }

    internal val kotlinDocCommentParser by extending {
        javaAnalysisPlugin.docCommentParsers providing { context ->
            KotlinDocCommentParser(
                context
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


    internal val symbolAnalyzerImpl by extending {
        plugin<InternalKotlinAnalysisPlugin>().documentableSourceLanguageParser providing { CompilerDocumentableSourceLanguageParser() }
    }
    internal val symbolFullClassHierarchyBuilder by extending {
        plugin<InternalKotlinAnalysisPlugin>().fullClassHierarchyBuilder providing { SymbolFullClassHierarchyBuilder() }
    }

    internal val symbolSyntheticDocumentableDetector by extending {
        plugin<InternalKotlinAnalysisPlugin>().syntheticDocumentableDetector providing { SymbolSyntheticDocumentableDetector() }
    }

    internal val moduleAndPackageDocumentationReader by extending {
        plugin<InternalKotlinAnalysisPlugin>().moduleAndPackageDocumentationReader providing ::ModuleAndPackageDocumentationReader
    }

/* internal val kotlinToJavaMapper by extending {
     plugin<InternalKotlinAnalysisPlugin>().kotlinToJavaService providing { DescriptorKotlinToJavaMapper() }
 }

 intern val descriptorInheritanceBuilder by extending {
     plugin<InternalKotlinAnalysisPlugin>().inheritanceBuilder providing { DescriptorInheritanceBuilder() }
 }
*/
 internal val symbolExternalDocumentablesProvider by extending {
     plugin<InternalKotlinAnalysisPlugin>().externalDocumentablesProvider providing ::SymbolExternalDocumentablesProvider
 }

    internal val defaultSamplesTransformer by extending {
        CoreExtensions.pageTransformer providing ::DefaultSamplesTransformer
    }

 @OptIn(DokkaPluginApiPreview::class)
 override fun pluginApiPreviewAcknowledgement(): PluginApiPreviewAcknowledgement = PluginApiPreviewAcknowledgement
}
