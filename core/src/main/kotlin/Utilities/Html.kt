package org.jetbrains.dokka

import java.net.URI


/**
 * Replaces symbols reserved in HTML with their respective entities.
 * Replaces & with &amp;, < with &lt; and > with &gt;
 */
fun String.htmlEscape(): String = replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")

// A URI consists of several parts (as described in https://docs.oracle.com/javase/7/docs/api/java/net/URI.html ):
// [scheme:][//authority][path][?query][#fragment]
//
// The anchorEnchoded() function encodes the given string to make it a legal value for <fragment>
fun String.anchorEncoded(): String {
    return URI(null, null, this).getRawFragment()
}
