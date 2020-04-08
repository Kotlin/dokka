package utils

import matchers.content.*
import org.jetbrains.dokka.pages.ContentGroup

//TODO: Try to unify those functions after update to 1.4
fun ContentMatcherBuilder<*>.signature(
    name: String,
    returnType: String? = null,
    vararg params: Pair<String, String>
) =
    platformHinted {
        group { // TODO: remove it when double wrapping for signatures will be resolved
            +"final fun"
            link { +name }
            +"("
            params.forEachIndexed { id, (n, t) ->
                +"$n:"
                group { link { +t } }
                if (id != params.lastIndex)
                    +", "
            }
            +")"
            returnType?.let { +": $it" }
        }
    }

fun ContentMatcherBuilder<*>.signatureWithReceiver(
    receiver: String,
    name: String,
    returnType: String? = null,
    vararg params: Pair<String, String>
) =
    platformHinted {
        group { // TODO: remove it when double wrapping for signatures will be resolved
            +"final fun"
            group {
                link { +receiver }
            }
            +"."
            link { +name }
            +"("
            params.forEach { (n, t) ->
                +"$n:"
                group { link { +t } }
            }
            +")"
            returnType?.let { +": $it" }
        }
    }


fun ContentMatcherBuilder<*>.pWrapped(text: String) =
    group {// TODO: remove it when double wrapping for descriptions will be resolved
        group { +text }
    }

fun ContentMatcherBuilder<*>.unnamedTag(tag: String, content: ContentMatcherBuilder<ContentGroup>.() -> Unit) =
    group {
        header(4) { +tag }
        group { content() }
    }