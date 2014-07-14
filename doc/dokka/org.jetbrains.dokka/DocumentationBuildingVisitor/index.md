---
layout: post
title: DocumentationBuildingVisitor
---
[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [DocumentationBuildingVisitor](index.md)

# DocumentationBuildingVisitor

```
class DocumentationBuildingVisitor
```
## Members
| Name | Summary |
|------|---------|
|[*.init*](_init_.md)|&nbsp;&nbsp;`public DocumentationBuildingVisitor(context: BindingContext, worker: DeclarationDescriptorVisitor<DocumentationNode, DocumentationNode>)`<br>|
|[context](context.md)|&nbsp;&nbsp;`val context: BindingContext`<br>|
|[createDocumentation](createDocumentation.md)|&nbsp;&nbsp;`private fun createDocumentation(descriptor: DeclarationDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[processCallable](processCallable.md)|&nbsp;&nbsp;`private fun processCallable(descriptor: CallableDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitChild](visitChild.md)|&nbsp;&nbsp;`private fun visitChild(descriptor: DeclarationDescriptor, data: DocumentationNode): Unit`<br>|
|[visitChildren](visitChildren.md)|&nbsp;&nbsp;`private fun visitChildren(descriptors: Collection<DeclarationDescriptor>, data: DocumentationNode): Unit`<br>|
|[visitClassDescriptor](visitClassDescriptor.md)|&nbsp;&nbsp;`open public fun visitClassDescriptor(descriptor: ClassDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitConstructorDescriptor](visitConstructorDescriptor.md)|&nbsp;&nbsp;`open public fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitFunctionDescriptor](visitFunctionDescriptor.md)|&nbsp;&nbsp;`open public fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitModuleDeclaration](visitModuleDeclaration.md)|&nbsp;&nbsp;`open public fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPackageFragmentDescriptor](visitPackageFragmentDescriptor.md)|&nbsp;&nbsp;`open public fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPackageViewDescriptor](visitPackageViewDescriptor.md)|&nbsp;&nbsp;`open public fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPropertyDescriptor](visitPropertyDescriptor.md)|&nbsp;&nbsp;`open public fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPropertyGetterDescriptor](visitPropertyGetterDescriptor.md)|&nbsp;&nbsp;`open public fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPropertySetterDescriptor](visitPropertySetterDescriptor.md)|&nbsp;&nbsp;`open public fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitReceiverParameterDescriptor](visitReceiverParameterDescriptor.md)|&nbsp;&nbsp;`open public fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitScriptDescriptor](visitScriptDescriptor.md)|&nbsp;&nbsp;`open public fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitTypeParameterDescriptor](visitTypeParameterDescriptor.md)|&nbsp;&nbsp;`open public fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitValueParameterDescriptor](visitValueParameterDescriptor.md)|&nbsp;&nbsp;`open public fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitVariableDescriptor](visitVariableDescriptor.md)|&nbsp;&nbsp;`open public fun visitVariableDescriptor(descriptor: VariableDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[worker](worker.md)|&nbsp;&nbsp;`private val worker: DeclarationDescriptorVisitor<DocumentationNode, DocumentationNode>`<br>|
