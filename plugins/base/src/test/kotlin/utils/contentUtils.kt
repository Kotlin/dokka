package utils

import matchers.content.*

fun ContentMatcherBuilder<*>.signature(name: String, vararg params: Pair<String, String>) =
    signature(name, null, *params)

fun ContentMatcherBuilder<*>.signature(name: String, returnType: String?, vararg params: Pair<String, String>) =
    platformHinted {
        group { // TODO: remove it when double wrapping for signatures will be resolved
            +"final fun"
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

fun ContentMatcherBuilder<*>.pWrapped(text: String) = group {// TODO: remove it when double wrapping for signatures will be resolved
    group { +text }
    br()
}