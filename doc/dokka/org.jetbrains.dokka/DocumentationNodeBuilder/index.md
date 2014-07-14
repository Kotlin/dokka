[dokka](../../index.md) / [org.jetbrains.dokka](../index.md) / [DocumentationNodeBuilder](index.md)

# DocumentationNodeBuilder

```
class DocumentationNodeBuilder
```
## Members
| Name | Summary |
|------|---------|
|[*.init*](_init_.md)|`public DocumentationNodeBuilder(context: BindingContext)`<br>|
|[addModality](addModality.md)|`fun addModality(descriptor: MemberDescriptor, data: DocumentationNode): Unit`<br>|
|[addType](addType.md)|`fun addType(descriptor: DeclarationDescriptor, t: JetType, data: DocumentationNode): Unit`<br>|
|[addVisibility](addVisibility.md)|`fun addVisibility(descriptor: MemberDescriptor, data: DocumentationNode): Unit`<br>|
|[context](context.md)|`val context: BindingContext`<br>|
|[reference](reference.md)|`fun reference(from: DocumentationNode, to: DocumentationNode, kind: Kind): Unit`<br>|
|[visitClassDescriptor](visitClassDescriptor.md)|`open public fun visitClassDescriptor(descriptor: ClassDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitConstructorDescriptor](visitConstructorDescriptor.md)|`open public fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitDeclarationDescriptor](visitDeclarationDescriptor.md)|`open public fun visitDeclarationDescriptor(descriptor: DeclarationDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitFunctionDescriptor](visitFunctionDescriptor.md)|`open public fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPackageFragmentDescriptor](visitPackageFragmentDescriptor.md)|`open public fun visitPackageFragmentDescriptor(descriptor: PackageFragmentDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPackageViewDescriptor](visitPackageViewDescriptor.md)|`open public fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitPropertyDescriptor](visitPropertyDescriptor.md)|`open public fun visitPropertyDescriptor(descriptor: PropertyDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitReceiverParameterDescriptor](visitReceiverParameterDescriptor.md)|`open public fun visitReceiverParameterDescriptor(descriptor: ReceiverParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitTypeParameterDescriptor](visitTypeParameterDescriptor.md)|`open public fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
|[visitValueParameterDescriptor](visitValueParameterDescriptor.md)|`open public fun visitValueParameterDescriptor(descriptor: ValueParameterDescriptor, data: DocumentationNode): DocumentationNode`<br>|
