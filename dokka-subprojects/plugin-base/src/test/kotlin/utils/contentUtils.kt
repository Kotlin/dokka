/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package utils

import matchers.content.*
import org.jetbrains.dokka.model.dfs
import org.jetbrains.dokka.pages.*
import kotlin.test.assertEquals

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
    if (visibility.isNotBlank()) +"$visibility "
    if (modifier.isNotBlank()) +"$modifier "
    +("${keywords.joinToString("") { "$it " }}fun ")
    link { +name }
    +"("
    if (params.isNotEmpty()) {
        group {
            params.forEachIndexed { id, (n, t) ->
                group {
                    t.annotations.forEach {
                        unwrapAnnotation(it)
                    }
                    t.keywords.forEach {
                        +it
                    }

                    +"$n: "
                    group { link { +(t.type) } }
                    if (id != params.lastIndex)
                        +", "
                }
            }
        }
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

fun ContentMatcherBuilder<*>.classSignature(
    annotations: Map<String, Set<String>>,
    visibility: String,
    modifier: String,
    keywords: Set<String>,
    name: String,
    vararg params: Pair<String, ParamAttributes>,
    parent: String? = null
) = group {
    annotations.entries.forEach {
        group {
            unwrapAnnotation(it)
        }
    }
    if (visibility.isNotBlank()) +"$visibility "
    if (modifier.isNotBlank()) +"$modifier "
    +("${keywords.joinToString("") { "$it " }}class ")
    link { +name }
    if (params.isNotEmpty()) {
        +"("
        group {
            params.forEachIndexed { id, (n, t) ->
                group {
                    t.annotations.forEach {
                        unwrapAnnotation(it)
                    }
                    t.keywords.forEach {
                        +it
                    }

                    +"$n: "
                    group { link { +(t.type) } }
                    if (id != params.lastIndex)
                        +", "
                }
            }
        }
        +")"
    }
    if (parent != null) {
        +(" : ")
        link {
            +(parent)
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
    if (visibility != null && visibility.isNotBlank()) +"$visibility "
    if (modifier != null && modifier.isNotBlank()) +"$modifier "
    +("${keywords.joinToString("") { "$it " }}fun ")
    group {
        link { +receiver }
    }
    +"."
    link { +name }
    +"("
    if (params.isNotEmpty()) {
        group {
            params.forEachIndexed { id, (n, t) ->
                group {
                    t.annotations.forEach {
                        unwrapAnnotation(it)
                    }
                    t.keywords.forEach {
                        +it
                    }

                    +"$n: "
                    group { link { +(t.type) } }
                    if (id != params.lastIndex)
                        +", "
                }
            }
        }
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
    type: String? = null,
    value: String? = null
) {
    group {
        header { +"Package-level declarations" }
        skipAllNotMatching()
    }
    tabbedGroup {
        group {
            skipAllNotMatching()
            tab(BasicTabbedContentType.PROPERTY) {
                header{ + "Properties" }
                table {
                    group {
                        link { +name }
                        divergentGroup {
                            divergentInstance {
                                divergent {
                                    group {
                                        group {
                                            annotations.entries.forEach {
                                                group {
                                                    unwrapAnnotation(it)
                                                }
                                            }
                                            if (visibility.isNotBlank()) +"$visibility "
                                            if (modifier.isNotBlank()) +"$modifier "
                                            +("${keywords.joinToString("") { "$it " }}$preposition ")
                                            link { +name }
                                            if (type != null) {
                                                +(": ")
                                                group {
                                                    link {
                                                        +(type)
                                                    }
                                                }
                                            }
                                            if (value != null) {
                                                +(" = $value")
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
    }
}


fun ContentMatcherBuilder<*>.typealiasSignature(name: String, expressionTarget: String) {
    group {
        header { +"Package-level declarations" }
        skipAllNotMatching()
    }
    group {
        group {
            tab(BasicTabbedContentType.TYPE) {
                header{ + "Types" }
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
                                                    group {
                                                        link { +name }
                                                    }
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
}

fun ContentMatcherBuilder<*>.pWrapped(text: String) =
    group {// TODO: remove it when double wrapping for descriptions will be resolved
        group { +text }
    }

fun ContentMatcherBuilder<*>.unnamedTag(tag: String, content: ContentMatcherBuilder<ContentGroup>.() -> Unit) =
    group {
        header(4) { +tag }
        content()
    }

fun ContentMatcherBuilder<*>.comment(content: ContentMatcherBuilder<ContentGroup>.() -> Unit) =
    group {
        group {
            content()
        }
    }

fun ContentMatcherBuilder<*>.unwrapAnnotation(elem: Map.Entry<String, Set<String>>) {
    group {
        +"@"
        link { +elem.key }
        if(elem.value.isNotEmpty()) {
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
}
inline fun<reified T> PageNode.contentPage(name: String, block: T.() -> Unit) {
    (dfs { it.name == name } as? T).assertNotNull("The page `$name` is not found").block()
}

fun ClasslikePageNode.assertHasFunctions(vararg expectedFunctionName: String) {
    val functions = this.findSectionWithName("Functions").assertNotNull("Functions")
    val functionsName = functions.children.map { (it.dfs { it is ContentText } as ContentText).text }
    assertEquals(expectedFunctionName.toList(), functionsName)
}

fun ClasslikePageNode.findSectionWithName(name: String) : ContentNode? {
    var sectionHeader: ContentHeader? = null
    return content.dfs { node ->
        node.children.filterIsInstance<ContentHeader>().any { header ->
            header.children.firstOrNull { it is ContentText && it.text == name }?.also { sectionHeader = header } != null
        }
    }?.children?.dropWhile { child -> child != sectionHeader  }?.drop(1)?.firstOrNull()
}

data class ParamAttributes(
    val annotations: Map<String, Set<String>>,
    val keywords: Set<String>,
    val type: String
)

fun RootPageNode.findTestType(packageName: String, name: String) =
    children.single { it.name == packageName }.children.single { it.name == name } as ContentPage
