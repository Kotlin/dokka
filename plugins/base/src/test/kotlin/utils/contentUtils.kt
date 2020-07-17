package utils

import matchers.content.*
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.pages.ContentGroup
import kotlin.text.Typography.nbsp

//TODO: Try to unify those functions after update to 1.4
fun ContentMatcherBuilder<*>.functionSignature(
    annotations: Map<String, Set<String>>,
    visibility: String,
    modifier: String,
    keywords: Set<String>,
    name: String,
    returnType: String? = null,
    vararg params: Pair<String, ParamAttributes>
) =
    platformHinted {
        bareSignature(annotations, visibility, modifier, keywords, name, returnType, *params)
    }

fun ContentMatcherBuilder<*>.bareSignature(
    annotations: Map<String, Set<String>>,
    visibility: String,
    modifier: String,
    keywords: Set<String>,
    name: String,
    returnType: String? = null,
    vararg params: Pair<String, ParamAttributes>
) = group {
    annotations.entries.forEach {
        group {
            unwrapAnnotation(it)
        }
    }
    +("$visibility $modifier ${keywords.joinToString("") { "$it " }} fun")
    link { +name }
    +"("
    params.forEachIndexed { id, (n, t) ->

        t.annotations.forEach {
            unwrapAnnotation(it)
        }
        t.keywords.forEach {
            +it
        }

        +"$n:"
        group { link { +(t.type) } }
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

fun ContentMatcherBuilder<*>.functionSignatureWithReceiver(
    annotations: Map<String, Set<String>>,
    visibility: String?,
    modifier: String?,
    keywords: Set<String>,
    receiver: String,
    name: String,
    returnType: String? = null,
    vararg params: Pair<String, ParamAttributes>
) =
    platformHinted {
        bareSignatureWithReceiver(annotations, visibility, modifier, keywords, receiver, name, returnType, *params)
    }

fun ContentMatcherBuilder<*>.bareSignatureWithReceiver(
    annotations: Map<String, Set<String>>,
    visibility: String?,
    modifier: String?,
    keywords: Set<String>,
    receiver: String,
    name: String,
    returnType: String? = null,
    vararg params: Pair<String, ParamAttributes>
) = group { // TODO: remove it when double wrapping for signatures will be resolved
    annotations.entries.forEach {
        group {
            unwrapAnnotation(it)
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

        t.annotations.forEach {
            unwrapAnnotation(it)
        }
        t.keywords.forEach {
            +it
        }

        +"$n:"
        group { link { +(t.type) } }
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

fun ContentMatcherBuilder<*>.propertySignature(
    annotations: Map<String, Set<String>>,
    visibility: String,
    modifier: String,
    keywords: Set<String>,
    preposition: String,
    name: String,
    type: String? = null
) {
    group {
        header { +"Package test" }
        skipAllNotMatching()
    }
    group {
        group {
            skipAllNotMatching()
            header { +"Properties" }
            table {
                group {
                    link { +name }
                    platformHinted {
                        group {
                            annotations.entries.forEach {
                                group {
                                    unwrapAnnotation(it)
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
            }
        }
    }
}


fun ContentMatcherBuilder<*>.typealiasSignature(name: String, expressionTarget: String) {
    group {
        header { +"Package test" }
        skipAllNotMatching()
    }
    group {
        group {
            skipAllNotMatching()
            header { +"Types" }
            table {
                group {
                    link { +name }
                    divergentGroup {
                        divergentInstance {
                            group {
                                group {
                                    group {
                                        group {
                                            +"typealias "
                                            group {
                                                link { +name }
                                                skipAllNotMatching()
                                            }
                                            +" = "
                                            group {
                                                link { +expressionTarget }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        skipAllNotMatching()
                    }
                }
                skipAllNotMatching()
            }
            skipAllNotMatching()
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

fun ContentMatcherBuilder<*>.unwrapAnnotation(elem: Map.Entry<String, Set<String>>) {
    group {
        +"@"
        link { +elem.key }
        +"("
        elem.value.forEach {
            group {
                +("$it = ")
                skipAllNotMatching()
            }
        }
        +")"
    }
}

data class ParamAttributes(
    val annotations: Map<String, Set<String>>,
    val keywords: Set<String>,
    val type: String
)
