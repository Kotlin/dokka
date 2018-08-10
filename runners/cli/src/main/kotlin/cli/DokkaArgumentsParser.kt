package org.jetbrains.dokka

import kotlinx.cli.*
import kotlin.reflect.KProperty
class ParseContext(val cli: CommandLineInterface = CommandLineInterface("dokka")) {
    private val map = mutableMapOf<KProperty<*>, (String) -> Unit>()
    private val flagActions = mutableMapOf<KProperty<*>, () -> Unit>()

    fun registerFlagAction(
        keys: List<String>,
        help: String,
        invoke: () -> Unit,
        property: KProperty<*>
    ) {
        if (property !in flagActions.keys) {
            cli.flagAction(keys, help) {
                flagActions[property]!!()
            }
        }
        flagActions[property] = invoke

    }

    fun registerSingleAction(
        keys: List<String>,
        help: String,
        invoke: (String) -> Unit,
        property: KProperty<*>
    ) {
        if (property !in map.keys) {
            cli.singleAction(keys, help) {
                map[property]!!(it)
            }
        }
        map[property] = invoke
    }

    fun registerRepeatableAction(
        keys: List<String>,
        help: String,
        invoke: (String) -> Unit,
        property: KProperty<*>
    ) {
        if (property !in map.keys) {
            cli.repeatingAction(keys, help) {
                map[property]!!(it)
            }
        }
        map[property] = invoke
    }

    fun parse(args: Array<String>) {
        cli.parseArgs(*args)
    }

}

fun CommandLineInterface.singleAction(
    keys: List<String>,
    help: String,
    invoke: (String) -> Unit
) = registerAction(
    object : FlagActionBase(keys, help) {
        override fun invoke(arguments: ListIterator<String>) {
            if (arguments.hasNext()) {
                val msg = arguments.next()
                invoke(msg)
            }
        }

        override fun invoke() {
            error("should be never called")
        }
    }
)

fun CommandLineInterface.repeatingAction(
    keys: List<String>,
    help: String,
    invoke: (String) -> Unit
) = registerAction(
    object : FlagActionBase(keys, help) {
        override fun invoke(arguments: ListIterator<String>) {
            while (arguments.hasNext()) {
                val message = arguments.next()

                if (this@repeatingAction.getFlagAction(message) != null) {
                    arguments.previous()
                    break
                }
                invoke(message)
            }
        }

        override fun invoke() {
            error("should be never called")
        }
    }

)

class DokkaArgumentsParser(val args: Array<String>, val parseContext: ParseContext) {
    fun <T> parseInto(constructor: (parseContext: DokkaArgumentsParser) -> T): T {
        val res = constructor(this)
        parseContext.parse(args)
        return res
    }

    fun <T> repeatableOption(
        keys: List<String>,
        help: String,
        transform: (String) -> T
    ): OptionDelegate<MutableList<T>> {
        val list = mutableListOf<T>()
        return object : OptionDelegate<MutableList<T>>(list) {
            override fun provideDelegate(thisRef: Any, property: KProperty<*>): OptionDelegate<MutableList<T>> {
                parseContext.registerRepeatableAction(
                    keys,
                    help,
                    {
                        list.add(transform(it))
                    },
                    property

                )
                return this
            }
        }
    }

    fun <T> repeatableFlag(
        keys: List<String>,
        help: String,
        initElement: (ParseContext) -> T
    ): OptionDelegate<MutableList<T>> {
        val list = mutableListOf<T>()
        return object : OptionDelegate<MutableList<T>>(list) {
            override fun provideDelegate(thisRef: Any, property: KProperty<*>): OptionDelegate<MutableList<T>> {
                parseContext.registerFlagAction(
                    keys,
                    help,
                    {
                        list.add(initElement(parseContext))
                    },
                    property

                )
                return this
            }
        }
    }

    fun <T> singleFlag(
        keys: List<String>,
        help: String,
        initElement: (ParseContext) -> T,
        transform: () -> T
    ): OptionDelegate<T> {
        val element = initElement(parseContext)
        return object : OptionDelegate<T>(element) {
            override fun provideDelegate(thisRef: Any, property: KProperty<*>): OptionDelegate<T> {
                parseContext.registerFlagAction(
                    keys,
                    help,
                    {
                        value = transform()
                    },
                    property
                )

                return this
            }
        }

    }

    fun <T> singleOption(
        keys: List<String>,
        help: String,
        transform: ((String) -> T)? = null,
        initElement: (ParseContext) -> T
    ): OptionDelegate<T> {
        val element: T = initElement(parseContext)
        return object : OptionDelegate<T>(element) {

            override fun provideDelegate(thisRef: Any, property: KProperty<*>): OptionDelegate<T> {
                parseContext.registerSingleAction(
                    keys,
                    help,
                    {
                        val toAdd = if (transform != null) {
                            transform(it)
                        } else {
                            it as T
                        }
                        value = toAdd
                    },
                    property
                )

                return this
            }
        }
    }

    fun singleBooleanFlag(
        keys: List<String>,
        help: String
    ) = singleFlag(keys, help, { false }, { true })

    fun <T> defaultSingleOption(
        keys: List<String>,
        help: String,
        defaultValue: T
    ) = singleOption(
        keys,
        help,
        { it as T },
        { defaultValue }
    )
}

abstract class OptionDelegate<T>(var value: T) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    abstract operator fun provideDelegate(thisRef: Any, property: KProperty<*>): OptionDelegate<T>
}
