package org.jetbrains.dokka.tc.plugin

import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.BasePlatformInfo
import org.jetbrains.dokka.model.Class
import org.jetbrains.dokka.model.ClassPlatformInfo
import org.jetbrains.dokka.model.Documentable
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.model.Module
import org.jetbrains.dokka.model.Package
import org.jetbrains.dokka.model.Parameter
import org.jetbrains.dokka.model.PlatformInfo
import org.jetbrains.dokka.model.Property
import org.jetbrains.dokka.model.doc.DocumentationNode
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.descriptors.DescriptorToDocumentationTranslator
import org.jetbrains.dokka.transformers.descriptors.KotlinClassKindTypes
import org.jetbrains.dokka.transformers.descriptors.KotlinTypeWrapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.idea.kdoc.findKDoc
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperclassesWithoutAny
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperInterfaces
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope

object TCDescriptorToDocumentationTranslator : DescriptorToDocumentationTranslator {
  override fun invoke(
    packageFragments: Iterable<PackageFragmentDescriptor>,
    platformData: PlatformData,
    context: DokkaContext
  ) = TCDescriptorVisitor(platformData, context.platforms[platformData]?.facade!!, context).run {
    packageFragments.map { visitPackageFragmentDescriptor(it, DRI.topLevel) }
  }.let { Module(it) }
}

class TCDescriptorVisitor(
  private val platformData: PlatformData,
  private val resolutionFacade: DokkaResolutionFacade,
  private val ctx: DokkaContext
) : DeclarationDescriptorVisitorEmptyBodies<Documentable, DRI>() {

  override fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor, parent: DRI): Nothing {
    throw IllegalStateException("${javaClass.simpleName} should never enter ${descriptor.javaClass.simpleName}")
  }

  override fun visitPackageFragmentDescriptor(
    descriptor: PackageFragmentDescriptor,
    parent: DRI
  ): Package {
    val dri = DRI(packageName = descriptor.fqName.asString())
    val scope = descriptor.getMemberScope()
    return Package(
      dri,
      scope.functions(dri),
      scope.properties(dri),
      scope.classes(dri)
    )
  }

  override fun visitClassDescriptor(descriptor: ClassDescriptor, parent: DRI): Class {
    val dri = parent.withClass(descriptor.name.asString())
    val scope = descriptor.getMemberScope(emptyList())
    val descriptorData = descriptor.takeUnless { it.isExpect }?.resolveClassDescriptionData()
    return Class(
      dri,
      descriptor.name.asString(),
      KotlinClassKindTypes.valueOf(descriptor.kind.toString()),
      descriptor.constructors.map { visitConstructorDescriptor(it, dri) },
      scope.functions(dri),
      scope.properties(dri),
      scope.classes(dri),
      descriptor.takeIf { it.isExpect }?.resolveClassDescriptionData(),
      listOfNotNull(descriptorData),
      mutableSetOf() // TODO Implement following method to return proper results getXMLDRIs(descriptor, descriptorData).toMutableSet()
    )
  }

  override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, parent: DRI): Property {
    val dri = parent.copy(callable = Callable.from(descriptor))
    return Property(
      dri,
      descriptor.name.asString(),
      descriptor.extensionReceiverParameter?.let { visitReceiverParameterDescriptor(it, dri) },
      descriptor.takeIf { it.isExpect }?.resolveDescriptorData(),
      listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData())
    )
  }

  override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, parent: DRI): Function {
    val dri = parent.copy(callable = Callable.from(descriptor))
    return Function(
      dri,
      descriptor.name.asString(),
      descriptor.returnType?.let { KotlinTypeWrapper(it) },
      false,
      descriptor.extensionReceiverParameter?.let { visitReceiverParameterDescriptor(it, dri) },
      descriptor.valueParameters.mapIndexed { index, desc -> parameter(index, desc, dri) },
      descriptor.takeIf { it.isExpect }?.resolveDescriptorData(),
      listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData())
    )
  }

  override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, parent: DRI): Function {
    val dri = parent.copy(callable = Callable.from(descriptor))
    return Function(
      dri,
      "<init>",
      KotlinTypeWrapper(descriptor.returnType),
      true,
      null,
      descriptor.valueParameters.mapIndexed { index, desc -> parameter(index, desc, dri) },
      descriptor.takeIf { it.isExpect }?.resolveDescriptorData(),
      listOfNotNull(descriptor.takeUnless { it.isExpect }?.resolveDescriptorData())
    )
  }

  override fun visitReceiverParameterDescriptor(
    descriptor: ReceiverParameterDescriptor,
    parent: DRI
  ) = Parameter(
    parent.copy(target = 0),
    null,
    KotlinTypeWrapper(descriptor.type),
    listOf(descriptor.resolveDescriptorData())
  )

  private fun parameter(index: Int, descriptor: ValueParameterDescriptor, parent: DRI) =
    Parameter(
      parent.copy(target = index + 1),
      descriptor.name.asString(),
      KotlinTypeWrapper(descriptor.type),
      listOf(descriptor.resolveDescriptorData())
    )

  private fun MemberScope.functions(parent: DRI): List<Function> =
    getContributedDescriptors(DescriptorKindFilter.FUNCTIONS) { true }
      .filterIsInstance<FunctionDescriptor>()
      .map { visitFunctionDescriptor(it, parent) }

  private fun MemberScope.properties(parent: DRI): List<Property> =
    getContributedDescriptors(DescriptorKindFilter.VALUES) { true }
      .filterIsInstance<PropertyDescriptor>()
      .map { visitPropertyDescriptor(it, parent) }

  private fun MemberScope.classes(parent: DRI): List<Class> =
    getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) { true }
      .filterIsInstance<ClassDescriptor>()
      .map { visitClassDescriptor(it, parent) }

  private fun DeclarationDescriptor.resolveDescriptorData(): PlatformInfo {
    println("Inspecting ${this.containingDeclaration}\n")
    val doc = findKDoc()
    println("Found Doc:\n${doc?.knownTag}")
    val visitor: TcVisitor = TcVisitor.default(
      resolutionFacade,
      this,
      ctx,
      ctx.platforms.toList().firstOrNull { it.first.platformType == platformData.platformType })
    val docHeader = visitor.parseKDoc(doc)
    tailrec fun DocumentationNode.visit(acc: String = ""): String =
      if (children.isEmpty()) acc else visit(children.fold(acc) { acc, docType -> "$acc\n${docType.root}" })
    println(docHeader.visit("DocHead and all children:\n"))
    return BasePlatformInfo(docHeader, listOf(platformData))
  }

  private fun ClassDescriptor.resolveClassDescriptionData(): ClassPlatformInfo {
    return ClassPlatformInfo(resolveDescriptorData(),
      (getSuperInterfaces() + getAllSuperclassesWithoutAny()).map { DRI.from(it) })
  }
}