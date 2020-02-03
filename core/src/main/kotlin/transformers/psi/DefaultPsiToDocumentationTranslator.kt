package org.jetbrains.dokka.transformers.psi

import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import org.jetbrains.dokka.JavadocParser
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.JavaClassReference
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.utilities.DokkaLogger

object DefaultPsiToDocumentationTranslator : PsiToDocumentationTranslator {

    override fun invoke(
        moduleName: String,
        psiFiles: List<PsiJavaFile>,
        platformData: PlatformData,
        context: DokkaContext
    ): Module {
        val docParser = DokkaPsiParser(platformData, context.logger)
        return Module(moduleName,
            psiFiles.map { psiFile ->
                val dri = DRI(packageName = psiFile.packageName)
                Package(
                    dri,
                    emptyList(),
                    emptyList(),
                    psiFile.classes.map { docParser.parseClass(it, dri) }
                )
            }
        )
    }

    class DokkaPsiParser(
        private val platformData: PlatformData,
        logger: DokkaLogger
    ) {

        private val javadocParser: JavadocParser = JavadocParser(logger)

        private fun getComment(psi: PsiNamedElement): List<PlatformInfo> {
            val comment = javadocParser.parseDocumentation(psi)
            return listOf(BasePlatformInfo(comment, listOf(platformData)))
        }

        fun parseClass(psi: PsiClass, parent: DRI): Class = with(psi) {
            val kind = when {
                isAnnotationType -> JavaClassKindTypes.ANNOTATION_CLASS
                isInterface -> JavaClassKindTypes.INTERFACE
                isEnum -> JavaClassKindTypes.ENUM_CLASS
                else -> JavaClassKindTypes.CLASS
            }
            val dri = parent.withClass(name.toString())
            /*superTypes.filter { !ignoreSupertype(it) }.forEach {
            node.appendType(it, NodeKind.Supertype)
            val superClass = it.resolve()
            if (superClass != null) {
                link(superClass, node, RefKind.Inheritor)
            }
        }*/
            return Class(
                dri,
                name.orEmpty(),
                kind,
                constructors.map { parseFunction(it, dri, true) },
                methods.mapNotNull { if (!it.isConstructor) parseFunction(it, dri) else null },
                fields.mapNotNull { parseField(it, dri) },
                innerClasses.map { parseClass(it, dri) },
                null,
                emptyList(),
                mutableSetOf()
            )
        }

        private fun parseFunction(psi: PsiMethod, parent: DRI, isConstructor: Boolean = false): Function {
            val dri = parent.copy(callable = Callable(
                psi.name,
                JavaClassReference(psi.containingClass?.name.orEmpty()),
                psi.parameterList.parameters.map { parameter ->
                    JavaClassReference(parameter.type.canonicalText)
                }
            )
            )
            return Function(
                dri,
                if (isConstructor) "<init>" else psi.name,
                psi.returnType?.let { JavaTypeWrapper(type = it) },
                isConstructor,
                null,
                psi.parameterList.parameters.mapIndexed { index, psiParameter ->
                    Parameter(
                        dri.copy(target = index + 1),
                        psiParameter.name,
                        JavaTypeWrapper(psiParameter.type),
                        getComment(psi)
                    )
                },
                null,
                getComment(psi)
            )
        }

        private fun parseField(psi: PsiField, parent: DRI): Property {
            val dri = parent.copy(
                callable = Callable(
                    psi.name,
                    JavaClassReference(psi.containingClass?.name.orEmpty()),
                    emptyList()
                )
            )
            return Property(
                dri,
                psi.name,
                null,
                null,
                getComment(psi)
            )
        }
    }
}

enum class JavaClassKindTypes : ClassKind {
    CLASS,
    INTERFACE,
    ENUM_CLASS,
    ENUM_ENTRY,
    ANNOTATION_CLASS;
}

class JavaTypeWrapper(
    type: PsiType
) : TypeWrapper {

    override val constructorFqName: String?
    override val constructorNamePathSegments: List<String>
    override val arguments: List<JavaTypeWrapper>
    override val dri: DRI?

    init {
        if (type is PsiClassReferenceType) {
            val resolved = type.resolve()
            constructorFqName = resolved?.qualifiedName
            constructorNamePathSegments = resolved?.qualifiedName?.split('.') ?: emptyList()
            arguments = type.parameters.mapNotNull {
                if (it is PsiClassReferenceType) JavaTypeWrapper(it) else null
            }
            dri = fromPsi(type)
        } else if (type is PsiEllipsisType) {
            constructorFqName = type.canonicalText
            constructorNamePathSegments = listOf(type.canonicalText) // TODO
            arguments = emptyList()
            dri = DRI("java.lang", "Object") // TODO
        } else if (type is PsiArrayType) {
            constructorFqName = type.canonicalText
            constructorNamePathSegments = listOf(type.canonicalText)
            arguments = emptyList()
            dri = (type as? PsiClassReferenceType)?.let { fromPsi(it) } // TODO
        } else {
            type as PsiPrimitiveType
            constructorFqName = type.name
            constructorNamePathSegments = type.name.split('.')
            arguments = emptyList()
            dri = null
        }
    }

    private fun fromPsi(type: PsiClassReferenceType): DRI {
        val className = type.className
        val pkg = type.canonicalText.removeSuffix(className).removeSuffix(".")
        return DRI(packageName = pkg, classNames = className)
    }

    override fun toString(): String {
        return constructorFqName.orEmpty()
    }
}