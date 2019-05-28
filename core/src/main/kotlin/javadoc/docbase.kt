package org.jetbrains.dokka.javadoc

import com.sun.javadoc.*
import org.jetbrains.dokka.*
import java.lang.reflect.Modifier.*
import java.util.*
import kotlin.reflect.KClass

private interface HasModule {
    val module: ModuleNodeAdapter
}

private interface HasDocumentationNode {
    val node: DocumentationNode
}

open class DocumentationNodeBareAdapter(override val node: DocumentationNode) : Doc, HasDocumentationNode {
    private var rawCommentText_: String? = null

    override fun name(): String = node.name
    override fun position(): SourcePosition? = SourcePositionAdapter(node)

    override fun inlineTags(): Array<out Tag>? = emptyArray()
    override fun firstSentenceTags(): Array<out Tag>? = emptyArray()
    override fun tags(): Array<out Tag> = emptyArray()
    override fun tags(tagname: String?): Array<out Tag>? = tags().filter { it.kind() == tagname || it.kind() == "@$tagname" }.toTypedArray()
    override fun seeTags(): Array<out SeeTag>? = tags().filterIsInstance<SeeTag>().toTypedArray()
    override fun commentText(): String = ""

    override fun setRawCommentText(rawDocumentation: String?) {
        rawCommentText_ = rawDocumentation ?: ""
    }

    override fun getRawCommentText(): String = rawCommentText_ ?: ""

    override fun isError(): Boolean = false
    override fun isException(): Boolean = node.kind == NodeKind.Exception
    override fun isEnumConstant(): Boolean = node.kind == NodeKind.EnumItem
    override fun isEnum(): Boolean = node.kind == NodeKind.Enum
    override fun isMethod(): Boolean = node.kind == NodeKind.Function
    override fun isInterface(): Boolean = node.kind == NodeKind.Interface
    override fun isField(): Boolean = node.kind == NodeKind.Field
    override fun isClass(): Boolean = node.kind == NodeKind.Class
    override fun isAnnotationType(): Boolean = node.kind == NodeKind.AnnotationClass
    override fun isConstructor(): Boolean = node.kind == NodeKind.Constructor
    override fun isOrdinaryClass(): Boolean = node.kind == NodeKind.Class
    override fun isAnnotationTypeElement(): Boolean = node.kind == NodeKind.Annotation

    override fun compareTo(other: Any?): Int = when (other) {
        !is DocumentationNodeAdapter -> 1
        else -> node.name.compareTo(other.node.name)
    }

    override fun equals(other: Any?): Boolean = node.qualifiedName() == (other as? DocumentationNodeAdapter)?.node?.qualifiedName()
    override fun hashCode(): Int = node.name.hashCode()

    override fun isIncluded(): Boolean = node.kind != NodeKind.ExternalClass
}


// TODO think of source position instead of null
// TODO tags
open class DocumentationNodeAdapter(override val module: ModuleNodeAdapter, node: DocumentationNode) : DocumentationNodeBareAdapter(node), HasModule {
    override fun inlineTags(): Array<out Tag> = buildInlineTags(module, this, node.content).toTypedArray()
    override fun firstSentenceTags(): Array<out Tag> = buildInlineTags(module, this, node.summary).toTypedArray()

    override fun tags(): Array<out Tag> {
        val result = ArrayList<Tag>(buildInlineTags(module, this, node.content))
        node.content.sections.flatMapTo(result) {
            when (it.tag) {
                ContentTags.SeeAlso -> buildInlineTags(module, this, it)
                else -> emptyList<Tag>()
            }
        }

        node.deprecation?.let {
            val content = it.content.asText()
            result.add(TagImpl(this, "deprecated", content ?: ""))
        }

        return result.toTypedArray()
    }
}

// should be extension property but can't because of KT-8745
private fun <T> nodeAnnotations(self: T): List<AnnotationDescAdapter> where T : HasModule, T : HasDocumentationNode
        = self.node.annotations.map { AnnotationDescAdapter(self.module, it) }

private fun DocumentationNode.hasAnnotation(klass: KClass<*>) = klass.qualifiedName in annotations.map { it.qualifiedName() }
private fun DocumentationNode.hasModifier(name: String) = details(NodeKind.Modifier).any { it.name == name }


class PackageAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : DocumentationNodeAdapter(module, node), PackageDoc {
    private val allClasses = listOf(node).collectAllTypesRecursively()

    override fun findClass(className: String?): ClassDoc? =
        allClasses.get(className)?.let { ClassDocumentationNodeAdapter(module, it) }

    override fun annotationTypes(): Array<out AnnotationTypeDoc> = emptyArray()
    override fun annotations(): Array<out AnnotationDesc> = node.members(NodeKind.AnnotationClass).map { AnnotationDescAdapter(module, it) }.toTypedArray()
    override fun exceptions(): Array<out ClassDoc> = node.members(NodeKind.Exception).map { ClassDocumentationNodeAdapter(module, it) }.toTypedArray()
    override fun ordinaryClasses(): Array<out ClassDoc> = node.members(NodeKind.Class).map { ClassDocumentationNodeAdapter(module, it) }.toTypedArray()
    override fun interfaces(): Array<out ClassDoc> = node.members(NodeKind.Interface).map { ClassDocumentationNodeAdapter(module, it) }.toTypedArray()
    override fun errors(): Array<out ClassDoc> = emptyArray()
    override fun enums(): Array<out ClassDoc> = node.members(NodeKind.Enum).map { ClassDocumentationNodeAdapter(module, it) }.toTypedArray()
    override fun allClasses(filter: Boolean): Array<out ClassDoc> = allClasses.values.map { ClassDocumentationNodeAdapter(module, it) }.toTypedArray()
    override fun allClasses(): Array<out ClassDoc> = allClasses(true)

    override fun isIncluded(): Boolean = node.name in module.allPackages
}

class AnnotationTypeDocAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : ClassDocumentationNodeAdapter(module, node), AnnotationTypeDoc {
    override fun elements(): Array<out AnnotationTypeElementDoc>? = emptyArray() // TODO
}

class AnnotationDescAdapter(val module: ModuleNodeAdapter, val node: DocumentationNode) : AnnotationDesc {
    override fun annotationType(): AnnotationTypeDoc? = AnnotationTypeDocAdapter(module, node.links.find { it.kind == NodeKind.AnnotationClass } ?: node) // TODO ?????
    override fun isSynthesized(): Boolean = false
    override fun elementValues(): Array<out AnnotationDesc.ElementValuePair>? = emptyArray() // TODO
}

open class ProgramElementAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : DocumentationNodeAdapter(module, node), ProgramElementDoc {
    override fun isPublic(): Boolean = node.hasModifier("public") || node.hasModifier("internal")
    override fun isPackagePrivate(): Boolean = false
    override fun isStatic(): Boolean = node.hasModifier("static")
    override fun modifierSpecifier(): Int = visibilityModifier or (if (isStatic) STATIC else 0)
    private val visibilityModifier
        get() = when {
            isPublic -> PUBLIC
            isPrivate -> PRIVATE
            isProtected -> PROTECTED
            else -> 0
        }
    override fun qualifiedName(): String? = node.qualifiedName()
    override fun annotations(): Array<out AnnotationDesc>? = nodeAnnotations(this).toTypedArray()
    override fun modifiers(): String? = "public ${if (isStatic) "static" else ""}".trim()
    override fun isProtected(): Boolean = node.hasModifier("protected")

    override fun isFinal(): Boolean = node.hasModifier("final")

    override fun containingPackage(): PackageDoc? {
        if (node.kind == NodeKind.Type) {
            return null
        }

        var owner: DocumentationNode? = node
        while (owner != null) {
            if (owner.kind == NodeKind.Package) {
                return PackageAdapter(module, owner)
            }
            owner = owner.owner
        }

        return null
    }

    override fun containingClass(): ClassDoc? {
        if (node.kind == NodeKind.Type) {
            return null
        }

        var owner = node.owner
        while (owner != null) {
            if (owner.kind in NodeKind.classLike) {
                return ClassDocumentationNodeAdapter(module, owner)
            }
            owner = owner.owner
        }

        return null
    }

    override fun isPrivate(): Boolean = node.hasModifier("private")
    override fun isIncluded(): Boolean = containingPackage()?.isIncluded ?: false && containingClass()?.let { it.isIncluded } ?: true
}

open class TypeAdapter(override val module: ModuleNodeAdapter, override val node: DocumentationNode) : Type, HasDocumentationNode, HasModule {
    private val javaLanguageService = JavaLanguageService()

    override fun qualifiedTypeName(): String = javaLanguageService.getArrayElementType(node)?.qualifiedNameFromType() ?: node.qualifiedNameFromType()
    override fun typeName(): String = (javaLanguageService.getArrayElementType(node)?.simpleName() ?: node.simpleName()) + dimension()
    override fun simpleTypeName(): String = typeName() // TODO difference typeName() vs simpleTypeName()

    override fun dimension(): String = Collections.nCopies(javaLanguageService.getArrayDimension(node), "[]").joinToString("")
    override fun isPrimitive(): Boolean = simpleTypeName() in setOf("int", "long", "short", "byte", "char", "double", "float", "boolean", "void")

    override fun asClassDoc(): ClassDoc? = if (isPrimitive) null else
        elementType?.asClassDoc() ?:
        when (node.kind) {
            in NodeKind.classLike,
            NodeKind.ExternalClass,
            NodeKind.Exception -> module.classNamed(qualifiedTypeName()) ?: ClassDocumentationNodeAdapter(module, node)

            else -> when {
                node.links.isNotEmpty() -> TypeAdapter(module, node.links.first()).asClassDoc()
                else -> ClassDocumentationNodeAdapter(module, node) // TODO ?
            }
        }

    override fun asTypeVariable(): TypeVariable? = if (node.kind == NodeKind.TypeParameter) TypeVariableAdapter(module, node) else null
    override fun asParameterizedType(): ParameterizedType? =
        if (node.details(NodeKind.Type).isNotEmpty() && javaLanguageService.getArrayElementType(node) == null)
            ParameterizedTypeAdapter(module, node)
        else
            null

    override fun asAnnotationTypeDoc(): AnnotationTypeDoc? = if (node.kind == NodeKind.AnnotationClass) AnnotationTypeDocAdapter(module, node) else null
    override fun asAnnotatedType(): AnnotatedType? = if (node.annotations.isNotEmpty()) AnnotatedTypeAdapter(module, node) else null
    override fun getElementType(): Type? = javaLanguageService.getArrayElementType(node)?.let { et -> TypeAdapter(module, et) }
    override fun asWildcardType(): WildcardType? = null

    override fun toString(): String = qualifiedTypeName() + dimension()
    override fun hashCode(): Int = node.name.hashCode()
    override fun equals(other: Any?): Boolean = other is TypeAdapter && toString() == other.toString()
}

class NotAnnotatedTypeAdapter(typeAdapter: AnnotatedTypeAdapter) : Type by typeAdapter {
    override fun asAnnotatedType() = null
}

class AnnotatedTypeAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : TypeAdapter(module, node), AnnotatedType {
    override fun underlyingType(): Type? = NotAnnotatedTypeAdapter(this)
    override fun annotations(): Array<out AnnotationDesc> = nodeAnnotations(this).toTypedArray()
}

class WildcardTypeAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : TypeAdapter(module, node), WildcardType {
    override fun extendsBounds(): Array<out Type> = node.details(NodeKind.UpperBound).map { TypeAdapter(module, it) }.toTypedArray()
    override fun superBounds(): Array<out Type> = node.details(NodeKind.LowerBound).map { TypeAdapter(module, it) }.toTypedArray()
}

class TypeVariableAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : TypeAdapter(module, node), TypeVariable {
    override fun owner(): ProgramElementDoc = node.owner!!.let<DocumentationNode, ProgramElementDoc> { owner ->
        when (owner.kind) {
            NodeKind.Function,
            NodeKind.Constructor -> ExecutableMemberAdapter(module, owner)

            NodeKind.Class,
            NodeKind.Interface,
            NodeKind.Enum -> ClassDocumentationNodeAdapter(module, owner)

            else -> ProgramElementAdapter(module, node.owner!!)
        }
    }

    override fun bounds(): Array<out Type>? = node.details(NodeKind.UpperBound).map { TypeAdapter(module, it) }.toTypedArray()
    override fun annotations(): Array<out AnnotationDesc>? = node.members(NodeKind.Annotation).map { AnnotationDescAdapter(module, it) }.toTypedArray()

    override fun qualifiedTypeName(): String = node.name
    override fun simpleTypeName(): String = node.name
    override fun typeName(): String = node.name

    override fun hashCode(): Int = node.name.hashCode()
    override fun equals(other: Any?): Boolean = other is Type && other.typeName() == typeName() && other.asTypeVariable()?.owner() == owner()

    override fun asTypeVariable(): TypeVariableAdapter = this
}

class ParameterizedTypeAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : TypeAdapter(module, node), ParameterizedType {
    override fun typeArguments(): Array<out Type> = node.details(NodeKind.Type).map { TypeVariableAdapter(module, it) }.toTypedArray()
    override fun superclassType(): Type? =
        node.lookupSuperClasses(module)
            .firstOrNull { it.kind == NodeKind.Class || it.kind == NodeKind.ExternalClass }
            ?.let { ClassDocumentationNodeAdapter(module, it) }

    override fun interfaceTypes(): Array<out Type> =
        node.lookupSuperClasses(module)
            .filter { it.kind == NodeKind.Interface }
            .map { ClassDocumentationNodeAdapter(module, it) }
            .toTypedArray()

    override fun containingType(): Type? = when (node.owner?.kind) {
        NodeKind.Package -> null
        NodeKind.Class,
        NodeKind.Interface,
        NodeKind.Object,
        NodeKind.Enum -> ClassDocumentationNodeAdapter(module, node.owner!!)

        else -> null
    }
}

class ParameterAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : DocumentationNodeAdapter(module, node), Parameter {
    override fun typeName(): String? = type()?.typeName()
    override fun type(): Type? = TypeAdapter(module, node.detail(NodeKind.Type))
    override fun annotations(): Array<out AnnotationDesc> = nodeAnnotations(this).toTypedArray()
}

class ReceiverParameterAdapter(module: ModuleNodeAdapter, val receiverType: DocumentationNode, val parent: ExecutableMemberAdapter) : DocumentationNodeAdapter(module, receiverType), Parameter {
    override fun typeName(): String? = receiverType.name
    override fun type(): Type? = TypeAdapter(module, receiverType)
    override fun annotations(): Array<out AnnotationDesc> = nodeAnnotations(this).toTypedArray()
    override fun name(): String = tryName("receiver")

    private tailrec fun tryName(name: String): String = when (name) {
        in parent.parameters().drop(1).map { it.name() } -> tryName("$$name")
        else -> name
    }
}

fun classOf(fqName: String, kind: NodeKind = NodeKind.Class) = DocumentationNode(fqName.substringAfterLast(".", fqName), Content.Empty, kind).let { node ->
    val pkg = fqName.substringBeforeLast(".", "")
    if (pkg.isNotEmpty()) {
        node.append(DocumentationNode(pkg, Content.Empty, NodeKind.Package), RefKind.Owner)
    }

    node
}

private fun DocumentationNode.hasNonEmptyContent() =
    this.content.summary !is ContentEmpty || this.content.description !is ContentEmpty || this.content.sections.isNotEmpty()


open class ExecutableMemberAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : ProgramElementAdapter(module, node), ExecutableMemberDoc {

    override fun isSynthetic(): Boolean = false
    override fun isNative(): Boolean = node.annotations.any { it.name == "native" }

    override fun thrownExceptions(): Array<out ClassDoc> = emptyArray() // TODO
    override fun throwsTags(): Array<out ThrowsTag> =
        node.content.sections
            .filter { it.tag == ContentTags.Exceptions && it.subjectName != null }
            .map { ThrowsTagAdapter(this, ClassDocumentationNodeAdapter(module, classOf(it.subjectName!!, NodeKind.Exception)), it.children) }
            .toTypedArray()

    override fun isVarArgs(): Boolean = node.details(NodeKind.Parameter).last().hasModifier("vararg")

    override fun isSynchronized(): Boolean = node.annotations.any { it.name == "synchronized" }

    override fun paramTags(): Array<out ParamTag> =
        collectParamTags(NodeKind.Parameter, sectionFilter = { it.subjectName in parameters().map { it.name() } })

    override fun thrownExceptionTypes(): Array<out Type> = emptyArray()
    override fun receiverType(): Type? = receiverNode()?.let { receiver -> TypeAdapter(module, receiver) }
    override fun flatSignature(): String = node.details(NodeKind.Parameter).map { JavaLanguageService().renderType(it) }.joinToString(", ", "(", ")")
    override fun signature(): String = node.details(NodeKind.Parameter).map { JavaLanguageService().renderType(it) }.joinToString(", ", "(", ")") // TODO it should be FQ types

    override fun parameters(): Array<out Parameter> =
        ((receiverNode()?.let { receiver -> listOf<Parameter>(ReceiverParameterAdapter(module, receiver, this)) } ?: emptyList())
                + node.details(NodeKind.Parameter).map { ParameterAdapter(module, it) }
                ).toTypedArray()

    override fun typeParameters(): Array<out TypeVariable> = node.details(NodeKind.TypeParameter).map { TypeVariableAdapter(module, it) }.toTypedArray()

    override fun typeParamTags(): Array<out ParamTag> =
        collectParamTags(NodeKind.TypeParameter, sectionFilter = { it.subjectName in typeParameters().map { it.simpleTypeName() } })

    private fun receiverNode() = node.details(NodeKind.Receiver).let { receivers ->
        when {
            receivers.isNotEmpty() -> receivers.single().detail(NodeKind.Type)
            else -> null
        }
    }
}

class ConstructorAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : ExecutableMemberAdapter(module, node), ConstructorDoc {
    override fun name(): String = node.owner?.name ?: throw IllegalStateException("No owner for $node")

    override fun containingClass(): ClassDoc? {
        return super.containingClass()
    }
}

class MethodAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : ExecutableMemberAdapter(module, node), MethodDoc {
    override fun overrides(meth: MethodDoc?): Boolean = false // TODO

    override fun overriddenType(): Type? = node.overrides.firstOrNull()?.owner?.let { owner -> TypeAdapter(module, owner) }

    override fun overriddenMethod(): MethodDoc? = node.overrides.map { MethodAdapter(module, it) }.firstOrNull()
    override fun overriddenClass(): ClassDoc? = overriddenMethod()?.containingClass()

    override fun isAbstract(): Boolean = false // TODO

    override fun isDefault(): Boolean = false

    override fun returnType(): Type = TypeAdapter(module, node.detail(NodeKind.Type))

    override fun tags(tagname: String?) = super.tags(tagname)

    override fun tags(): Array<out Tag> {
        val tags = super.tags().toMutableList()
        node.content.findSectionByTag(ContentTags.Return)?.let {
            tags += ReturnTagAdapter(module, this, it.children)
        }

        return tags.toTypedArray()
    }
}

class FieldAdapter(module: ModuleNodeAdapter, node: DocumentationNode) : ProgramElementAdapter(module, node), FieldDoc {
    override fun isSynthetic(): Boolean = false

    override fun constantValueExpression(): String? = node.detailOrNull(NodeKind.Value)?.let { it.name }
    override fun constantValue(): Any? = constantValueExpression()

    override fun type(): Type = TypeAdapter(module, node.detail(NodeKind.Type))
    override fun isTransient(): Boolean = node.hasAnnotation(Transient::class)
    override fun serialFieldTags(): Array<out SerialFieldTag> = emptyArray()

    override fun isVolatile(): Boolean = node.hasAnnotation(Volatile::class)
}
open class ClassDocumentationNodeAdapter(module: ModuleNodeAdapter, val classNode: DocumentationNode)
    : ProgramElementAdapter(module, classNode),
    Type by TypeAdapter(module, classNode),
    ClassDoc,
    AnnotationTypeDoc {

    override fun elements(): Array<out AnnotationTypeElementDoc>? = emptyArray() // TODO

    override fun name(): String {
        val parent = classNode.owner
        if (parent?.kind in NodeKind.classLike) {
            return parent!!.name + "." + classNode.name
        }
        return classNode.simpleName()
    }

    override fun qualifiedName(): String? {
        return super.qualifiedName()
    }
    override fun constructors(filter: Boolean): Array<out ConstructorDoc> = classNode.members(NodeKind.Constructor).map { ConstructorAdapter(module, it) }.toTypedArray()
    override fun constructors(): Array<out ConstructorDoc> = constructors(true)
    override fun importedPackages(): Array<out PackageDoc> = emptyArray()
    override fun importedClasses(): Array<out ClassDoc>? = emptyArray()
    override fun typeParameters(): Array<out TypeVariable> = classNode.details(NodeKind.TypeParameter).map { TypeVariableAdapter(module, it) }.toTypedArray()
    override fun asTypeVariable(): TypeVariable? = if (classNode.kind == NodeKind.Class) TypeVariableAdapter(module, classNode) else null
    override fun isExternalizable(): Boolean = interfaces().any { it.qualifiedName() == "java.io.Externalizable" }
    override fun definesSerializableFields(): Boolean = false
    override fun methods(filter: Boolean): Array<out MethodDoc> = classNode.members(NodeKind.Function).map { MethodAdapter(module, it) }.toTypedArray() // TODO include get/set methods
    override fun methods(): Array<out MethodDoc> = methods(true)
    override fun enumConstants(): Array<out FieldDoc>? = classNode.members(NodeKind.EnumItem).map { FieldAdapter(module, it) }.toTypedArray()
    override fun isAbstract(): Boolean = classNode.details(NodeKind.Modifier).any { it.name == "abstract" }
    override fun interfaceTypes(): Array<out Type> = classNode.lookupSuperClasses(module)
        .filter { it.kind == NodeKind.Interface }
        .map { ClassDocumentationNodeAdapter(module, it) }
        .toTypedArray()

    override fun interfaces(): Array<out ClassDoc> = classNode.lookupSuperClasses(module)
        .filter { it.kind == NodeKind.Interface }
        .map { ClassDocumentationNodeAdapter(module, it) }
        .toTypedArray()

    override fun typeParamTags(): Array<out ParamTag> =
        collectParamTags(NodeKind.TypeParameter, sectionFilter = { it.subjectName in typeParameters().map { it.simpleTypeName() } })

    override fun fields(): Array<out FieldDoc> = fields(true)
    override fun fields(filter: Boolean): Array<out FieldDoc> = classNode.members(NodeKind.Field).map { FieldAdapter(module, it) }.toTypedArray()

    override fun findClass(className: String?): ClassDoc? = null // TODO !!!
    override fun serializableFields(): Array<out FieldDoc> = emptyArray()
    override fun superclassType(): Type? = classNode.lookupSuperClasses(module).singleOrNull { it.kind == NodeKind.Class }?.let { ClassDocumentationNodeAdapter(module, it) }
    override fun serializationMethods(): Array<out MethodDoc> = emptyArray() // TODO
    override fun superclass(): ClassDoc? = classNode.lookupSuperClasses(module).singleOrNull { it.kind == NodeKind.Class }?.let { ClassDocumentationNodeAdapter(module, it) }
    override fun isSerializable(): Boolean = false // TODO
    override fun subclassOf(cd: ClassDoc?): Boolean {
        if (cd == null) {
            return false
        }

        val expectedFQName = cd.qualifiedName()
        val types = arrayListOf(classNode)
        val visitedTypes = HashSet<String>()

        while (types.isNotEmpty()) {
            val type = types.removeAt(types.lastIndex)
            val fqName = type.qualifiedName()

            if (expectedFQName == fqName) {
                return true
            }

            visitedTypes.add(fqName)
            types.addAll(type.details(NodeKind.Supertype).filter { it.qualifiedName() !in visitedTypes })
        }

        return false
    }

    override fun innerClasses(): Array<out ClassDoc> = classNode.members(NodeKind.Class).map { ClassDocumentationNodeAdapter(module, it) }.toTypedArray()
    override fun innerClasses(filter: Boolean): Array<out ClassDoc> = innerClasses()
}

fun DocumentationNode.lookupSuperClasses(module: ModuleNodeAdapter) =
    details(NodeKind.Supertype)
        .map { it.links.firstOrNull() }.mapNotNull { module.allTypes[it?.qualifiedName()] }

fun List<DocumentationNode>.collectAllTypesRecursively(): Map<String, DocumentationNode> {
    val result = hashMapOf<String, DocumentationNode>()

    fun DocumentationNode.collectTypesRecursively() {
        val classLikeMembers = NodeKind.classLike.flatMap { members(it) }
        classLikeMembers.forEach {
            result.put(it.qualifiedName(), it)
            it.collectTypesRecursively()
        }
    }

    forEach { it.collectTypesRecursively() }
    return result
}

class ModuleNodeAdapter(val module: DocumentationModule, val reporter: DocErrorReporter, val outputPath: String) : DocumentationNodeBareAdapter(module), DocErrorReporter by reporter, RootDoc {
    val allPackages = module.members(NodeKind.Package).associateBy { it.name }
    val allTypes = module.members(NodeKind.Package).collectAllTypesRecursively()

    override fun packageNamed(name: String?): PackageDoc? = allPackages[name]?.let { PackageAdapter(this, it) }

    override fun classes(): Array<out ClassDoc> =
        allTypes.values.map { ClassDocumentationNodeAdapter(this, it) }.toTypedArray()

    override fun options(): Array<out Array<String>> = arrayOf(
        arrayOf("-d", outputPath),
        arrayOf("-docencoding", "UTF-8"),
        arrayOf("-charset", "UTF-8"),
        arrayOf("-keywords")
    )

    override fun specifiedPackages(): Array<out PackageDoc>? = module.members(NodeKind.Package).map { PackageAdapter(this, it) }.toTypedArray()

    override fun classNamed(qualifiedName: String?): ClassDoc? =
        allTypes[qualifiedName]?.let { ClassDocumentationNodeAdapter(this, it) }

    override fun specifiedClasses(): Array<out ClassDoc> = classes()
}

private fun DocumentationNodeAdapter.collectParamTags(kind: NodeKind, sectionFilter: (ContentSection) -> Boolean) =
    (node.details(kind)
        .filter(DocumentationNode::hasNonEmptyContent)
        .map { ParamTagAdapter(module, this, it.name, true, it.content.children) }

            + node.content.sections
        .filter(sectionFilter)
        .map {
            ParamTagAdapter(module, this, it.subjectName ?: "?", true,
                it.children.filterNot { contentNode -> contentNode is LazyContentBlock }
            )
        }
    )
    .distinctBy { it.parameterName }
    .toTypedArray()