package  org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.analysis.DokkaResolutionFacade
import org.jetbrains.dokka.links.Callable
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withClass
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.Function
import org.jetbrains.dokka.pages.PlatformData
import org.jetbrains.dokka.plugability.DokkaContext
import org.jetbrains.dokka.transformers.descriptors.DescriptorToDocumentationTranslator
import org.jetbrains.dokka.transformers.descriptors.DokkaDescriptorVisitor
import org.jetbrains.kotlin.descriptors.*

object KotlinAsJavaDescriptorToDocumentationTranslator : DescriptorToDocumentationTranslator {
    override fun invoke(
        moduleName: String,
        packageFragments: Iterable<PackageFragmentDescriptor>,
        platformData: PlatformData,
        context: DokkaContext
    ): Module =
        KotlinAsJavaDokkaDescriptorVisitor(platformData, context.platforms[platformData]?.facade!!).run {
            packageFragments.map { visitPackageFragmentDescriptor(it, DRI.topLevel) }
        }.let { Module(moduleName, it) }
}

class KotlinAsJavaDokkaDescriptorVisitor(
    platformData: PlatformData,
    resolutionFacade: DokkaResolutionFacade
) : DokkaDescriptorVisitor(platformData, resolutionFacade) {
    override fun visitPackageFragmentDescriptor(
        descriptor: PackageFragmentDescriptor,
        parent: DRI
    ): Package {
        val dri = DRI(packageName = descriptor.fqName.asString())
        DescriptorCache.add(dri, descriptor)
        return super.visitPackageFragmentDescriptor(descriptor, parent)
    }

    override fun visitClassDescriptor(descriptor: ClassDescriptor, parent: DRI): Classlike {
        val dri = parent.withClass(descriptor.name.asString())
        DescriptorCache.add(dri, descriptor)
        return super.visitClassDescriptor(descriptor, parent)
    }

    override fun visitPropertyDescriptor(descriptor: PropertyDescriptor, parent: DRI): Property {
        val dri = parent.copy(callable = Callable.from(descriptor))
        DescriptorCache.add(dri, descriptor)
        return super.visitPropertyDescriptor(descriptor, parent)
    }

    override fun visitFunctionDescriptor(descriptor: FunctionDescriptor, parent: DRI): Function {
        val dri = parent.copy(callable = Callable.from(descriptor))
        DescriptorCache.add(dri, descriptor)
        return super.visitFunctionDescriptor(descriptor, parent)
    }

    override fun visitConstructorDescriptor(descriptor: ConstructorDescriptor, parent: DRI): Function {
        val dri = parent.copy(callable = Callable.from(descriptor))
        DescriptorCache.add(dri, descriptor)
        return super.visitConstructorDescriptor(descriptor, parent)
    }

    override fun visitPropertyAccessorDescriptor(
        descriptor: PropertyAccessorDescriptor,
        propertyDescriptor: PropertyDescriptor,
        parent: DRI
    ): Function {
        val dri = parent.copy(callable = Callable.from(descriptor))
        DescriptorCache.add(dri, descriptor)
        return super.visitPropertyAccessorDescriptor(descriptor, propertyDescriptor, parent)
    }
}