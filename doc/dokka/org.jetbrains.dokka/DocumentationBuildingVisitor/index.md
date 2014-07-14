[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [DocumentationBuildingVisitor](index.md)

# DocumentationBuildingVisitor

```
class DocumentationBuildingVisitor
```
## Members
| Name | Summary |
|------|---------|
|[*.init*](_init_.md)|`public DocumentationBuildingVisitor(context: BindingContext, worker: DeclarationDescriptorVisitor<DocumentationNode, DocumentationNode>)`<br>|
|[context](context.md)|`val context: BindingContext`<br>|
|[createDocumentation](createDocumentation.md)|`private fun createDocumentation(descriptor: DeclarationDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[processCallable](processCallable.md)|`private fun processCallable(descriptor: CallableDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitChild](visitChild.md)|`private fun visitChild(descriptor: DeclarationDescriptor, data: DocumentationNode): Unit`<br>|
|[visitChildren](visitChildren.md)|`private fun visitChildren(descriptors: Collection<DeclarationDescriptor>, data: DocumentationNode): Unit`<br>|
|[visitClassDescriptor](visitClassDescriptor.md)|`open public fun visitClassDescriptor(descriptor: ClassDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitConstructorDescriptor](visitConstructorDescriptor.md)|`open public fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitFunctionDescriptor](visitFunctionDescriptor.md)|`open public fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitModuleDeclaration](visitModuleDeclaration.md)|`open public fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPackageFragmentDescriptor](visitPackageFragmentDescriptor.md)|`open public fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPackageViewDescriptor](visitPackageViewDescriptor.md)|`open public fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPropertyDescriptor](visitPropertyDescriptor.md)|`open public fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPropertyGetterDescriptor](visitPropertyGetterDescriptor.md)|`open public fun visitPropertyGetterDescriptor(descriptor: PropertyGetterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPropertySetterDescriptor](visitPropertySetterDescriptor.md)|`open public fun visitPropertySetterDescriptor(descriptor: PropertySetterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitReceiverParameterDescriptor](visitReceiverParameterDescriptor.md)|`open public fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitScriptDescriptor](visitScriptDescriptor.md)|`open public fun visitScriptDescriptor(scriptDescriptor: ScriptDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitTypeParameterDescriptor](visitTypeParameterDescriptor.md)|`open public fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitValueParameterDescriptor](visitValueParameterDescriptor.md)|`open public fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitVariableDescriptor](visitVariableDescriptor.md)|`open public fun visitVariableDescriptor(descriptor: VariableDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[worker](worker.md)|`private val worker: DeclarationDescriptorVisitor<DocumentationNode, DocumentationNode>`<br>|
