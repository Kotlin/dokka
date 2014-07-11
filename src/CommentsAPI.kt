package com.jetbrains.dokka

import org.jetbrains.jet.lang.descriptors.*
import org.jetbrains.jet.lexer.*
import org.jetbrains.jet.lang.resolve.*
import org.jetbrains.jet.kdoc.psi.api.*
import org.jetbrains.jet.lang.psi.JetDeclaration

fun BindingContext.getDocumentation(descriptor: DeclarationDescriptor): KDoc? {
    val psiElement = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
    if (psiElement == null) throw IllegalArgumentException("$descriptor doesn't have connection to source code, is it synthetic?")

    return psiElement.previousSiblings().takeWhile { it !is JetDeclaration }.firstOrNull { it is KDoc } as KDoc?
}
