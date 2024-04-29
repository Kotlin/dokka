package org.jetbrains.dokka.uitest.kdoc

import org.jetbrains.dokka.uitest.ClassWithSuchLongNameThatItLikelyWontFitInOneLineAnywhere
import org.jetbrains.dokka.uitest.types.SimpleKotlinClass
import org.jetbrains.dokka.uitest.overload

/**
 * This functions throws.
 *
 * @throws IllegalStateException if anything unexpected happens
 * @exception ArrayIndexOutOfBoundsException if array's index is out of bounds
 */
fun throws() {}

/**
 * This documentation contains additional info
 *
 * @author Dokka maintainers
 * @since 1.9.22
 * @see SimpleKotlinClass for a different class, this has description
 * @return string literal "foo"
 */
fun additionalInfo(): String = "foo"

/**
 * This function contains a (runnable?) sample.
 *
 * @sample org.jetbrains.dokka.uitest.internal.functionToBeUsedAsSample
 */
fun sample() {}

/**
 * Describes a function with a parameter
 *
 * @param foo the foo parameter
 */
fun params(foo: Int) {}

/**
 * Describes an extension function
 *
 * @receiver this documents the receiver of the extension function, which is [SimpleKotlinClass]
 */
fun SimpleKotlinClass.extension(): String {
    return "foo"
}

/**
 * This documentation links to [SimpleKotlinClass]
 * and [this class](SimpleJvmClass)
 * and [website](https://kotl.in/dokka)
 * and [SimpleJvmClass][even this]
 *
 * I cannot reference a specific [overload] though.
 */
fun links() {}

/**
 * @see SimpleKotlinClass
 * @param s String
 * @throws IllegalStateException
 * @param i
 * @throws IllegalArgumentException if the argument is illegal
 * @see sample if you wanna see a sample
 * @see ClassWithSuchLongNameThatItLikelyWontFitInOneLineAnywhere omg
 * @see functionWithAReallyLongNameSimilarToThatClassWithALongNameButThisOneIsAFunction omg2
 */
fun multipleLinkSections(s: String, i: Int) {}

fun functionWithAReallyLongNameSimilarToThatClassWithALongNameButThisOneIsAFunction() {}

/**
 * This demonstrates all KDoc tags in one place
 *
 * @see SimpleKotlinClass
 * @param s String
 * @throws IllegalStateException
 * @param i
 * @throws IllegalArgumentException if the argument is illegal
 * @see sample if you wanna see a sample
 * @see ClassWithSuchLongNameThatItLikelyWontFitInOneLineAnywhere omg
 * @see functionWithAReallyLongNameSimilarToThatClassWithALongNameButThisOneIsAFunction omg2
 * @receiver this documents the receiver of the extension function, which is [SimpleKotlinClass]
 * @throws IllegalStateException if anything unexpected happens
 * @exception ArrayIndexOutOfBoundsException if array's index is out of bounds
 * @author Dokka maintainers
 * @since 1.9.22
 * @see SimpleKotlinClass for a different class, this has description
 * @sample org.jetbrains.dokka.uitest.internal.functionToBeUsedAsSample
 * @return string literal "foo"
 */
fun SimpleKotlinClass.allKDocTagsInOnePlace(i: Int, s: String) {}
