---
layout: post
title: DocumentationNodeBuilder
---
[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [DocumentationNodeBuilder](index.md)

# DocumentationNodeBuilder

```
class DocumentationNodeBuilder
```
## Members
| Name | Summary |
|------|---------|
|[*.init*](_init_.md)|&nbsp;&nbsp;`public DocumentationNodeBuilder(context: BindingContext)`<br>|
|[addModality](addModality.md)|&nbsp;&nbsp;`fun addModality(descriptor: MemberDescriptor, data: DocumentationNode): Unit`<br>|
|[addType](addType.md)|&nbsp;&nbsp;`fun addType(descriptor: DeclarationDescriptor, t: JetType, data: DocumentationNode): Unit`<br>|
|[addVisibility](addVisibility.md)|&nbsp;&nbsp;`fun addVisibility(descriptor: MemberDescriptor, data: DocumentationNode): Unit`<br>|
|[context](context.md)|&nbsp;&nbsp;`val context: BindingContext`<br>|
|[reference](reference.md)|&nbsp;&nbsp;`fun reference(from: DocumentationNode, to: DocumentationNode, kind: Kind): Unit`<br>|
|[visitClassDescriptor](visitClassDescriptor.md)|&nbsp;&nbsp;`open public fun visitClassDescriptor(descriptor: ClassDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitConstructorDescriptor](visitConstructorDescriptor.md)|&nbsp;&nbsp;`open public fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitDeclarationDescriptor](visitDeclarationDescriptor.md)|&nbsp;&nbsp;`open public fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitFunctionDescriptor](visitFunctionDescriptor.md)|&nbsp;&nbsp;`open public fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPackageFragmentDescriptor](visitPackageFragmentDescriptor.md)|&nbsp;&nbsp;`open public fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPackageViewDescriptor](visitPackageViewDescriptor.md)|&nbsp;&nbsp;`open public fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPropertyDescriptor](visitPropertyDescriptor.md)|&nbsp;&nbsp;`open public fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitReceiverParameterDescriptor](visitReceiverParameterDescriptor.md)|&nbsp;&nbsp;`open public fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitTypeParameterDescriptor](visitTypeParameterDescriptor.md)|&nbsp;&nbsp;`open public fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitValueParameterDescriptor](visitValueParameterDescriptor.md)|&nbsp;&nbsp;`open public fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
