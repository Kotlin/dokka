package org.jetbrains.dokka

import kotlinx.cli.*
import kotlin.reflect.KProperty

class ParseContext(val cli: CommandLineInterface = CommandLineInterface("dokka")) {
    private val transformActions = mutableMapOf<KProperty<*>, (String) -> Unit>()
    private val flagActions = mutableMapOf<KProperty<*>, () -> Unit>()

    fun registerFlagAction(
        keys: List<String>,
        help: String,
        property: KProperty<*>,
        invoke: () -> Unit
    ) {
        if (property !in flagActions.keys) {
            cli.flagAction(keys, help) {
                flagActions[property]!!()
            }
        }
        flagActions[property] = invoke

    }

    fun registerSingleOption(
        keys: List<String>,
        help: String,
        property: KProperty<*>,
        invoke: (String) -> Unit
    ) {
        if (property !in transformActions.keys) {
            cli.singleAction(keys, help) {
                transformActions[property]!!(it)
            }
        }
        transformActions[property] = invoke
    }

    fun registerRepeatableOption(
        keys: List<String>,
        help: String,
        property: KProperty<*>,
        invoke: (String) -> Unit
    ) {
        if (property !in transformActions.keys) {
            cli.repeatingAction(keys, help) {
                transformActions[property]!!(it)
            }
        }
        transformActions[property] = invoke
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
    class OptionDelegate<T>(
        var value: T,
        private val action: (delegate: OptionDelegate<T>, property: KProperty<*>) -> Unit
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
        operator fun provideDelegate(thisRef: Any, property: KProperty<*>): OptionDelegate<T> {
            action(this, property)
            return this
        }
    }

    fun <T> parseInto(dest: T): T {
        // TODO: constructor: (DokkaArgumentsParser) -> T
        parseContext.parse(args)
        return dest
    }

    fun <T> repeatableOption(
        keys: List<String>,
        help: String,
        transform: (String) -> T
    ) = OptionDelegate(mutableListOf<T>()) { delegate, property ->
        parseContext.registerRepeatableOption(keys, help, property) {
            delegate.value.add(transform(it))
        }
    }

    fun <T : String?> repeatableOption(
        keys: List<String>,
        help: String
    ) = repeatableOption(keys, help) { it as T }

    fun <T> repeatableFlag(
        keys: List<String>,
        help: String,
        initElement: (ParseContext) -> T
    ) = OptionDelegate(mutableListOf<T>()) { delegate, property ->
        parseContext.registerFlagAction(keys, help, property) {
            delegate.value.add(initElement(parseContext))
        }
    }

    fun <T> singleFlag(
        keys: List<String>,
        help: String,
        initElement: (ParseContext) -> T,
        transform: () -> T
    ) = OptionDelegate(initElement(parseContext)) { delegate, property ->
        parseContext.registerFlagAction(keys, help, property) {
            delegate.value = transform()
        }
    }

    fun singleFlag(
        keys: List<String>,
        help: String
    ) = singleFlag(keys, help, { false }, { true })

    fun <T : String?> stringOption(
        keys: List<String>,
        help: String,
        defaultValue: T
    ) = singleOption(keys, help, { it as T }, { defaultValue })

    fun <T> singleOption(
        keys: List<String>,
        help: String,
        transform: (String) -> T,
        initElement: (ParseContext) -> T
    ) = OptionDelegate(initElement(parseContext)) { delegate, property ->
        parseContext.registerSingleOption(keys, help, property) {
            val toAdd = transform(it)
            delegate.value = toAdd
        }
    }
}


//`(-perPackage fqName [-include-non-public] [...other flags])*` (edited)
//`(-sourceLink dir url [-urlSuffix value])*`
//`(-extLink url [packageListUrl])*`