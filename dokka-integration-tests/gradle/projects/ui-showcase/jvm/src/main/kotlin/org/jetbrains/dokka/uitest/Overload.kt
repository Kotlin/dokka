package org.jetbrains.dokka.uitest

/**
 * The documentation
 *
 * @return nothing, void
 */
fun overload() {}

/**
 * The documentation - 1
 *
 * @param s1 first param
 * @since 1.9.22
 */
fun overload(s1: String) {}

/**
 * The documentation - 1
 *
 * @param s1 first param
 * @param s2 the new one
 * @author Dokka maintainers
 */
fun overload(s1: String, s2: String) {}

fun overload(s1: String, s2: String, s3: String): String = "foo"

/**
 * This has duplicate docs with s6
 */
fun overload(s1: String, s2: String, s3: String, s4: String, s5: String) {}

/**
 * This has duplicate docs with s5
 */
fun overload(s1: String, s2: String, s3: String, s4: String, s5: String, s6: String) {}
