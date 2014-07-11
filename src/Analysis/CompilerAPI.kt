package org.jetbrains.dokka

import org.jetbrains.jet.cli.common.arguments.*
import org.jetbrains.jet.cli.common.messages.*
import org.jetbrains.jet.cli.jvm.*
import org.jetbrains.jet.cli.jvm.compiler.*
import org.jetbrains.jet.utils.*
import java.io.*
import org.jetbrains.jet.lang.resolve.java.*
import com.google.common.base.*
import com.intellij.psi.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.lang.psi.*
import org.jetbrains.jet.analyzer.*
import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.jet.lang.resolve.scopes.WritableScope
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler
import org.jetbrains.jet.lang.types.TypeUtils

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

fun JetCoreEnvironment.analyze(messageCollector: MessageCollector): BindingContext {
    val project = getProject()
    val sourceFiles = getSourceFiles()

    val analyzerWithCompilerReport = AnalyzerWithCompilerReport(messageCollector)
    analyzerWithCompilerReport.analyzeAndReport(sourceFiles) {
        val support = CliLightClassGenerationSupport.getInstanceForCli(project)!!
        val sharedTrace = support.getTrace()
        val sharedModule = support.getModule()
        val compilerConfiguration = getConfiguration()!!
        AnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(project, sourceFiles, sharedTrace,
                                                             Predicates.alwaysTrue<PsiFile>(),
                                                             sharedModule,
                                                             compilerConfiguration.get(JVMConfigurationKeys.MODULE_IDS),
                                                             compilerConfiguration.get(JVMConfigurationKeys.INCREMENTAL_CACHE_BASE_DIR))
    }

    val exhaust = analyzerWithCompilerReport.getAnalyzeExhaust()
    assert(exhaust != null) { "AnalyzeExhaust should be non-null, compiling: " + sourceFiles }

    return exhaust!!.getBindingContext()
}

fun AnalyzerWithCompilerReport.analyzeAndReport(files: List<JetFile>, analyser: () -> AnalyzeExhaust) = analyzeAndReport(analyser, files)

fun BindingContext.getPackageFragment(file: JetFile) = get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file)

fun DeclarationDescriptor.isUserCode() =
        when (this) {
            is PackageFragmentDescriptor -> false
            is PropertyAccessorDescriptor -> !isDefault()
            is CallableMemberDescriptor -> getKind() == CallableMemberDescriptor.Kind.DECLARATION
            else -> true
        }

public fun getFunctionInnerScope(outerScope: JetScope, descriptor: FunctionDescriptor): JetScope {
    val redeclarationHandler = object : RedeclarationHandler {
        override fun handleRedeclaration(first: DeclarationDescriptor, second: DeclarationDescriptor) {
            // TODO: check if we can ignore redeclarations
        }
    }

    val parameterScope = WritableScopeImpl(outerScope, descriptor, redeclarationHandler, "Function KDoc scope")
    val receiver = descriptor.getReceiverParameter()
    if (receiver != null) {
        parameterScope.setImplicitReceiver(receiver)
    }
    for (typeParameter in descriptor.getTypeParameters()) {
        parameterScope.addTypeParameterDescriptor(typeParameter)
    }
    for (valueParameterDescriptor in descriptor.getValueParameters()) {
        parameterScope.addVariableDescriptor(valueParameterDescriptor)
    }
    parameterScope.addLabeledDeclaration(descriptor)
    parameterScope.changeLockLevel(WritableScope.LockLevel.READING)
    return parameterScope
}

fun BindingContext.getResolutionScope(descriptor: DeclarationDescriptor): JetScope {
    when (descriptor) {
        is PackageFragmentDescriptor -> return descriptor.getMemberScope()
        is PackageViewDescriptor -> return descriptor.getMemberScope()

        is ClassDescriptorWithResolutionScopes -> return descriptor.getScopeForMemberDeclarationResolution()
        is ClassDescriptor -> {
            val typeParameters: List<TypeParameterDescriptor> = descriptor.getTypeConstructor().getParameters()
            return descriptor.getMemberScope(TypeUtils.getDefaultTypeProjections(typeParameters))
        }
        is FunctionDescriptor -> return getFunctionInnerScope(getResolutionScope(descriptor.getContainingDeclaration()), descriptor)
    }

    if (descriptor is DeclarationDescriptorNonRoot)
        return getResolutionScope(descriptor.getContainingDeclaration())

    throw IllegalArgumentException("Cannot find resolution scope for root $descriptor")
}
