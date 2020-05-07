package utils

import matchers.content.*
import org.jetbrains.dokka.pages.ContentGroup
import kotlin.text.Typography.nbsp

//TODO: Try to unify those functions after update to 1.4
fun ContentMatcherBuilder<*>.functionSignature(
    annotations: Set<String>,
    visibility: String,
    modifier: String,
    keywords: Set<String>,
    name: String,
    returnType: String? = null,
    vararg params: Pair<String, Map<String, Set<String>>>
) =
    platformHinted {
        group { // TODO: remove it when double wrapping for signatures will be resolved
            group {
                annotations.forEach {
                    group {
                        link { +it }
                    }
                }
                +("$visibility $modifier ${keywords.joinToString("") { "$it " }} fun")
                link { +name }
                +"("
                params.forEachIndexed { id, (n, t) ->

                    t["Annotations"]?.forEach {
                        link { +it }
                    }
                    t["Keywords"]?.forEach {
                        +it
                    }

                    +"$n:"
                    group { link { +(t["Type"]?.single() ?: "") } }
                    if (id != params.lastIndex)
                        +", "
                }
                +")"
                if (returnType != null) {
                    +(": ")
                    group {
                        link {
                            +(returnType)
                        }
                    }
                }
            }
        }
    }

fun ContentMatcherBuilder<*>.functionSignatureWithReceiver(

    annotations: Set<String>,
    visibility: String?,
    modifier: String?,
    keywords: Set<String>,
    receiver: String,
    name: String,
    returnType: String? = null,
    vararg params: Pair<String, Map<String, Set<String>>>
) =
    platformHinted {
        group { // TODO: remove it when double wrapping for signatures will be resolved
            group {
                annotations.forEach {
                    group {
                        link { +it }
                    }
                }
                +("$visibility $modifier ${keywords.joinToString("") { "$it " }} fun")
                group {
                    link { +receiver }
                }
                +"."
                link { +name }
                +"("
                params.forEachIndexed { id, (n, t) ->

                    t["Annotations"]?.forEach {
                        +("$it ")
                    }
                    t["Keywords"]?.forEach {
                        +("$it ")
                    }

                    +"$n:"
                    group { link { +(t["Type"]?.single() ?: "") } }
                    if (id != params.lastIndex)
                        +", "
                }
                +")"
                if (returnType != null) {
                    +(": ")
                    group {
                        link {
                            +(returnType)
                        }
                    }
                }
            }
        }
    }

fun ContentMatcherBuilder<*>.propertySignature(
    annotations: Set<String>,
    visibility: String,
    modifier: String,
    keywords: Set<String>,
    preposition: String,
    name: String,
    type: String? = null
) {
    group {
        header { +"Package test" }
    }
    group {
        header { +"Properties" }
        table {
            group {
                link { +name }
                group {
                    platformHinted {
                        group {
                            group {
                                annotations.forEach {
                                    group {
                                        link { +it }
                                    }
                                }
                                +("$visibility $modifier ${keywords.joinToString("") { "$it " }} $preposition")
                                link { +name }
                                if (type != null) {
                                    +(": ")
                                    group {
                                        link {
                                            +(type)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    group {

                    }
                }
            }
        }
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