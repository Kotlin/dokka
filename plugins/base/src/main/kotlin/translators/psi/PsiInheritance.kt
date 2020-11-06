package org.jetbrains.dokka.base.translators.psi

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.idea.refactoring.fqName.getKotlinFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

internal fun PsiClass.implementsInterface(fqName: FqName): Boolean {
    return allInterfaces().any { it.getKotlinFqName() == fqName }
}

internal fun PsiClass.allInterfaces(): Sequence<PsiClass> {
    return sequence {
        this.yieldAll(interfaces.toList())
        interfaces.forEach { yieldAll(it.allInterfaces()) }
    }
}

/**
 * Workaround for failing [PsiMethod.findSuperMethods].
 * This might be resolved once ultra light classes are enabled for dokka
 * See [KT-39518](https://youtrack.jetbrains.com/issue/KT-39518)
 */
internal fun PsiMethod.findSuperMethodsOrEmptyArray(logger: DokkaLogger): Array<PsiMethod> {
    return try {
        /*
        We are not even attempting to call "findSuperMethods" on all methods called "getGetter" or "getSetter"
        on any object implementing "kotlin.reflect.KProperty", since we know that those methods will fail
        (KT-39518). Just catching the exception is not good enough, since "findSuperMethods" will
        print the whole exception to stderr internally and then spoil the console.
         */
        val kPropertyFqName = FqName("kotlin.reflect.KProperty")
        if (
            this.parent?.safeAs<PsiClass>()?.implementsInterface(kPropertyFqName) == true &&
            (this.name == "getSetter" || this.name == "getGetter")
        ) {
            logger.warn("Skipped lookup of super methods for ${getKotlinFqName()} (KT-39518)")
            return emptyArray()
        }
        findSuperMethods()
    } catch (exception: Throwable) {
        logger.warn("Failed to lookup of super methods for ${getKotlinFqName()} (KT-39518)")
        emptyArray()
    }
}