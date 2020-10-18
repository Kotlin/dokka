package org.jetbrains.dokka.kotlinAsJava

import org.jetbrains.dokka.CoreExtensions
import org.jetbrains.dokka.base.DokkaBase
import org.jetbrains.dokka.kotlinAsJava.signatures.JavaSignatureProvider
import org.jetbrains.dokka.kotlinAsJava.transformers.KotlinAsJavaDocumentableTransformer
import org.jetbrains.dokka.kotlinAsJava.translators.KotlinAsJavaDocumentableToPageTranslator
import org.jetbrains.dokka.plugability.DokkaPlugin

class KotlinAsJavaPlugin : DokkaPlugin() {
    val kotlinAsJavaDocumentableTransformer by extending {
        CoreExtensions.documentableTransformer with KotlinAsJavaDocumentableTransformer()
    }
    val javaSignatureProvider by extending {
        with(plugin<DokkaBase>()) {
            signatureProvider providing ::JavaSignatureProvider override kotlinSignatureProvider
        }
    }
    val kotlinAsJavaDocumentableToPageTranslator by extending {
        CoreExtensions.documentableToPageTranslator providing ::KotlinAsJavaDocumentableToPageTranslator  override
            plugin<DokkaBase>().documentableToPageTranslator
    }
}