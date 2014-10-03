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
import org.jetbrains.jet.lang.resolve.scopes.*

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

fun JetCoreEnvironment.analyze(): AnalyzeExhaust {
    val project = getProject()
    val sourceFiles = getSourceFiles()
    val support = CliLightClassGenerationSupport.getInstanceForCli(project)!!
    val sharedTrace = support.getTrace()
    val sharedModule = support.newModule()
    val exhaust = TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(project, sourceFiles, sharedTrace,
                                                         Predicates.alwaysFalse<PsiFile>(),
                                                         sharedModule,
                                                         null,
                                                         null)
    return exhaust
}

fun BindingContext.getPackageFragment(file: JetFile): PackageFragmentDescriptor? = get(BindingContext.FILE_TO_PACKAGE_FRAGMENT, file)

fun DeclarationDescriptor.isUserCode() =
        when (this) {
            is PackageViewDescriptor -> false
            is PackageFragmentDescriptor -> false
            is PropertyAccessorDescriptor -> !isDefault()
            is CallableMemberDescriptor -> getKind() == CallableMemberDescriptor.Kind.DECLARATION
            else -> true
        }

public fun getPackageInnerScope(descriptor: PackageFragmentDescriptor): JetScope {
    val module = descriptor.getContainingDeclaration()
    val packageView = module.getPackage(descriptor.fqName)
    val packageScope = packageView!!.getMemberScope()
    return packageScope
}

public fun getClassInnerScope(outerScope: JetScope, descriptor: ClassDescriptor): JetScope {
    val redeclarationHandler = RedeclarationHandler.DO_NOTHING

    val headerScope = WritableScopeImpl(outerScope, descriptor, redeclarationHandler, "Class ${descriptor.getName()} header scope")
    for (typeParameter in descriptor.getTypeConstructor().getParameters()) {
        headerScope.addTypeParameterDescriptor(typeParameter)
    }
    for (constructor in descriptor.getConstructors()) {
        headerScope.addFunctionDescriptor(constructor)
    }
    headerScope.addLabeledDeclaration(descriptor)
    headerScope.changeLockLevel(WritableScope.LockLevel.READING)

    val classScope = ChainedScope(descriptor, "Class ${descriptor.getName()} scope", descriptor.getDefaultType().getMemberScope(), headerScope)
    return classScope
}

public fun getFunctionInnerScope(outerScope: JetScope, descriptor: FunctionDescriptor): JetScope {
    val redeclarationHandler = RedeclarationHandler.DO_NOTHING

    val functionScope = WritableScopeImpl(outerScope, descriptor, redeclarationHandler, "Function ${descriptor.getName()} scope")
    val receiver = descriptor.getExtensionReceiverParameter()
    if (receiver != null) {
        functionScope.setImplicitReceiver(receiver)
    }
    for (typeParameter in descriptor.getTypeParameters()) {
        functionScope.addTypeParameterDescriptor(typeParameter)
    }
    for (valueParameterDescriptor in descriptor.getValueParameters()) {
        functionScope.addVariableDescriptor(valueParameterDescriptor)
    }
    functionScope.addLabeledDeclaration(descriptor)
    functionScope.changeLockLevel(WritableScope.LockLevel.READING)
    return functionScope
}

public fun getPropertyInnerScope(outerScope: JetScope, descriptor: PropertyDescriptor): JetScope {
    val redeclarationHandler = RedeclarationHandler.DO_NOTHING

    val propertyScope = WritableScopeImpl(outerScope, descriptor, redeclarationHandler, "Property ${descriptor.getName()} scope")
    val receiver = descriptor.getExtensionReceiverParameter()
    if (receiver != null) {
        propertyScope.setImplicitReceiver(receiver)
    }
    for (typeParameter in descriptor.getTypeParameters()) {
        propertyScope.addTypeParameterDescriptor(typeParameter)
    }
    for (valueParameterDescriptor in descriptor.getValueParameters()) {
        propertyScope.addVariableDescriptor(valueParameterDescriptor)
    }
    for (accessor in descriptor.getAccessors()) {
        propertyScope.addFunctionDescriptor(accessor)
    }
    propertyScope.addLabeledDeclaration(descriptor)
    propertyScope.changeLockLevel(WritableScope.LockLevel.READING)
    return propertyScope
}

fun BindingContext.getResolutionScope(descriptor: DeclarationDescriptor): JetScope {
    when (descriptor) {
        is PackageFragmentDescriptor ->
            return getPackageInnerScope(descriptor)

        is PackageViewDescriptor ->
            return descriptor.getMemberScope()

        is ClassDescriptor ->
            return getClassInnerScope(getResolutionScope(descriptor.getContainingDeclaration()), descriptor)

        is FunctionDescriptor ->
            return getFunctionInnerScope(getResolutionScope(descriptor.getContainingDeclaration()), descriptor)

        is PropertyDescriptor ->
            return getPropertyInnerScope(getResolutionScope(descriptor.getContainingDeclaration()), descriptor)
    }

    if (descriptor is DeclarationDescriptorNonRoot)
        return getResolutionScope(descriptor.getContainingDeclaration())

    throw IllegalArgumentException("Cannot find resolution scope for root $descriptor")
}
