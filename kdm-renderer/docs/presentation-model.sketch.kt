/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// ============================================================================
// НАБРОСОК, НЕ КОД. Файл лежит вне source set и не участвует в сборке.
//
// Presentation model: дерево страниц с готовым контентом.
// Kotlin строит это дерево из KDM. React читает JSON и делает HTML.
// Один файл JSON = одна страница.
//
// Правила, зашитые в типы:
//   1. Тип узла описывает СМЫСЛ, не вёрстку. Нет узлов "вкладка", "секция".
//   2. Вариант для платформ — явный список фрагментов у тех данных,
//      которые по платформам различаются. Один вариант = различий нет.
//   3. Порядок всего задаёт Kotlin. React не сортирует.
//   4. Ссылки резолвит Kotlin. В модели — готовые URL или явная ошибка.
//   5. Неизвестный kind не ломает React (как unknown-теги KDoc).
//
// Сериализация — как в KDM (см. KdJson):
//   Json { classDiscriminator = "kind"; ignoreUnknownKeys = true }
//
// Читателю-фронтендеру: это исходник типов для TS. TS-типы генерируются
// из этих классов; внизу файла — как выглядит union на стороне TS.
// ============================================================================

package org.jetbrains.kotlin.documentation.presentation

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/// Название набора исходников: "commonMain", "jvmMain".
/// Порядок в списках фрагментов задаёт Kotlin. Он один и тот же во всём дереве.
typealias FragmentName = String

// ----------------------------------------------------------------------------
// 1. Сайт и дерево страниц
// ----------------------------------------------------------------------------

/**
 * Корень. Один сайт = один или несколько модулей.
 *
 * Запись на диск: одна страница = один JSON-файл, путь повторяет URL.
 * Из PageNode в файл НЕ пишутся [PageNode.children] — иначе каждый файл
 * понесёт всё дерево. Скелет дерева (kind, name, url, children без контента)
 * пишется один раз в отдельный файл `nav.json` — React читает его для
 * бокового меню. Остальные общие файлы: индекс поиска, список платформ.
 */
@Serializable
data class PresentationSite(
    val schemaVersion: String,
    val modules: List<PageNode>,
)

/**
 * Одна страница. Растёт из Page прототипа kdm-pages (PageSet.kt):
 * kind/represents/children взяты оттуда, добавлены url, title и content.
 */
@Serializable
data class PageNode(
    val kind: PageKind,
    /** Готовый URL страницы: "/kmp/org.example/-foo/". Вычислен на шаге page set. */
    val url: String,
    /** Имя для заголовка страницы: "Foo". Без разметки. */
    val name: String,
    /** Готовый <title>: "Foo - MyLib 2.0". Собирает Kotlin. */
    val title: String,
    /** Путь от страницы до корня сайта: "../../". Нужен для адресов ассетов. */
    val pathToRoot: String,
    /** Какие элементы KDM страница представляет. Для отладки и package-list. */
    val represents: List<ElementRef> = emptyList(),
    /** Контент страницы. Какой набор узлов ждать — см. контракт по [PageKind]. */
    val content: List<ContentNode> = emptyList(),
    /** Дерево. В постраничные файлы не пишется (см. [PresentationSite]). */
    val children: List<PageNode> = emptyList(),
)

/**
 * Вид страницы. Формирует ожидаемый набор узлов в content:
 *   MODULE    — DocBlock (описание модуля) + группы PACKAGES
 *   PACKAGE   — DocBlock + группы TYPES / FUNCTIONS / PROPERTIES / ...
 *   CLASSLIKE — Declaration + группы CONSTRUCTORS / TYPES / ENTRIES / ...
 *               (+ EXTENSION_* и INHERITORS — их собрал Kotlin из
 *                обратных индексов, ADR-0001)
 *   CALLABLE  — Declaration (перегрузки = один Declaration, несколько
 *               вариантов и/или несколько variants внутри)
 * Q-020: у enum-entry сегодня полноценная страница CLASSLIKE — уточнить,
 * оставить ли так.
 */
@Serializable
enum class PageKind { MODULE, PACKAGE, CLASSLIKE, CALLABLE }

/**
 * Ссылка на элемент KDM: набор исходников + непрозрачный id.
 * Скопировано из PageSet.kt, поле id — opaque, разбирасть его нельзя.
 */
@Serializable
data class ElementRef(
    val fragment: FragmentName,
    val id: String,
)

// ----------------------------------------------------------------------------
// 2. Механизм вариантов по платформам (главная структура модели)
// ----------------------------------------------------------------------------

// Идея: всё, что может различаться между платформами, лежит не списком
// плоских полей, а списком ВАРИАНТОВ. У каждого варианта — свой список
// фрагментов. Пример из ui-showcase: класс KmpClass, сигнатура одна для
// commonMain+linuxMain, у jsMain — своя:
//
//   Declaration(variants = [
//     DeclarationVariant(
//       fragments = ["commonMain", "linuxMain"],
//       signature = [fun, KmpClass, (, ), ...]),
//     DeclarationVariant(
//             fragments = ["jsMain"],
//       signature = [expect, fun, ...]),
//   ])
//
// Один вариант = страница без расхождений, UI рисует её без переключателя.
// Бейджи платформ React выводит из объединения fragments всех вариантов —
// отдельного узла для бейджей нет. Вкладки или секции — выбор React.
// Замена вкладок на секции этот JSON не меняет.

/**
 * Заголовок объявления: сигнатура + документация + пометки.
 * Главный предмет страницы CLASSLIKE или CALLABLE.
 */
@Serializable
data class DeclarationNode(
    val variants: List<DeclarationVariant>,
) : ContentNode

@Serializable
data class DeclarationVariant(
    val fragments: List<FragmentName>,
    /** Готовые куски заголовка. Собирает Kotlin (порт signature providers). */
    val signature: List<SigToken>,
    /** Полная документация. Форма — см. шапку DocNode (временная). */
    val documentation: List<DocNode>? = null,
    val deprecation: Deprecation? = null,
    /** "@since" / @SinceKotlin. Поле, не текст (E-009). */
    val since: String? = null,
    /** Ссылка на исходники. Полный URL собирает Kotlin из настроек (G-05). */
    val sourceLink: SourceLink? = null,
)

// ----------------------------------------------------------------------------
// 3. Узлы контента. Закрытый словарь + запасной выход.
// ----------------------------------------------------------------------------

@Serializable
sealed interface ContentNode {
    // kind = "declaration"   — главный предмет страницы
    // kind = "memberGroup"   — список членов/строк
    // kind = "docBlock"      — документация модуля или пакета
    // kind = "errorNote"     — явная ошибка, не текст-заглушка
    // kind = "custom"        — расширение третьих лиц
}

/**
 * Список однородных строк: члены класса, пакеты модуля, расширения, наследники.
 * Одним механизмом покрываются E-030 (секции на страницах классов и пакетов),
 * E-033 (наследники), E-034 (расширения), E-036/E-037 (строки на страницах
 * модулей и пакетов).
 */
@Serializable
data class MemberGroup(
    val groupKind: MemberGroupKind,
    val members: List<MemberEntry>,
) : ContentNode

/**
 * Словарь секций — из инвентаризации текущего справочника (40-page-anatomy).
 * UI решает, показать группу вкладкой или секцией; состав групп — данные.
 */
@Serializable
enum class MemberGroupKind {
    MODULES,               // строки на сводной странице
    PACKAGES,              // строки на странице модуля
    TYPES,                 // классы/интерфейсы/объекты/перечисления/псевдонимы
    ENTRIES,               // элементы enum
    CONSTRUCTORS,
    FUNCTIONS,
    PROPERTIES,
    COMPANION_FUNCTIONS,
    COMPANION_PROPERTIES,
    EXTENSION_FUNCTIONS,   // собрано из обратного индекса (ADR-0001)
    EXTENSION_PROPERTIES,
    INHERITORS,            // собрано из обратного индекса (ADR-0001)
}

/**
 * Одна строка списка. Например: функция в секции FUNCTIONS.
 */
@Serializable
data class MemberEntry(
    /** Якорь внутри страницы-родителя: "#reverse". Ссылки на член — url#anchor. */
    val anchor: String,
    /** Адрес страницы самого члена. Нет страницы — null: унаследованный чужой
     *  член (kotlin/Any/equals) или элемент без своей страницы (Q-025). */
    val pageUrl: String? = null,
    /** Откуда член. Нужен для правила "не сливать перегрузки из разных
     *  модулей и пакетов" (ADR-0001) и для пометки "inherited from X". */
    val origin: Origin? = null,
    val variants: List<MemberVariant>,
)

/**
 * Вариант строки. У строки, как и у объявления, сигнатура и краткое
 * описание могут различаться по платформам (E-044: одна строка на 5
 * фрагментов; расходятся — несколько вариантов).
 */
@Serializable
data class MemberVariant(
    val fragments: List<FragmentName>,
    val signature: List<SigToken>,
    /** Первое предложение документации. Выделяет Kotlin (E-020). */
    val brief: List<DocNode>? = null,
    val deprecation: Deprecation? = null,
    /** У унаследованных строк — ссылка на исходники чужого класса. */
    val sourceLink: SourceLink? = null,
)

/**
 * Провенанс члена. Вычисляет сборщик: в KDM поля нет (Q-025).
 */
@Serializable
data class Origin(
    val module: String,
    val packageFqName: String,
    /** Полное имя класса-родителя, если член унаследован: "kotlin.Any".
     *  null — член объявлен в этом модуле. */
    val inheritedFrom: String? = null,
)

/**
 * Описание модуля или пакета (из includes, F-044). На страницах классов
 * документация лежит внутри DeclarationNode, отдельного узла не нужно.
 */
@Serializable
data class DocBlock(
    val documentation: List<DocNode>,
) : ContentNode

/**
 * Явная ошибка вместо заглушки текстом. Примеры:
 *   code = "unresolved-supertype", text = "Supertype not found: Foo"
 *   code = "unresolved-link",      text = "[Bar]"
 * Смысл: фронтенд может показать предупреждение, а не молча потерять данные.
 */
@Serializable
data class ErrorNote(
    val code: String,
    val text: String,
) : ContentNode

/**
 * Запасной выход для расширений (Q-004, Dackka). React знает имя — рендерит
 * своими компонентами; не знает — пропускает узел, не падает.
 */
@Serializable
data class CustomNode(
    val name: String,      // "mycompany.graph"
    val data: JsonObject, // произвольные данные
) : ContentNode

// ----------------------------------------------------------------------------
// 4. Сигнатура: готовые куски. Собирает Kotlin, React только оборачивает.
// ----------------------------------------------------------------------------

/**
 * Один кусок заголовка объявления. Реакт мапит kind -> span/класс и склеивает
 * text. Пробелы — тоже куски, отдельного "форматирования" нет.
 */
@Serializable
sealed interface SigToken

@Serializable data class KeywordToken(val text: String) : SigToken   // fun, val, public, expect
@Serializable data class NameToken(val text: String) : SigToken       // имя объявления, имя параметра
@Serializable data class TypeToken(val text: String, val link: LinkTarget? = null) : SigToken // Int, List<T>
@Serializable data class LiteralToken(val text: String) : SigToken    // "HELLO", 42, null
@Serializable data class OperatorToken(val text: String) : SigToken   // =, ->, +, ?:
@Serializable data class PunctToken(val text: String) : SigToken      // ( ) : , . < >
@Serializable data class SpaceToken(val text: String) : SigToken      // " " — чтобы React не угадывал

// ----------------------------------------------------------------------------
// 5. Ссылки. Резолвит Kotlin. В модели — готовый URL или явная ошибка.
// ----------------------------------------------------------------------------

@Serializable
sealed interface LinkTarget

/** Ссылка внутри сайта. URL готов, до якоря доведён. */
@Serializable data class InternalLink(val url: String) : LinkTarget

/** Ссылка наружу: Javadoc, stdlib. Адрес найден по package-list (ADR-0003). */
@Serializable data class ExternalLink(val url: String) : LinkTarget

/** Не разрешилась. Причина — данные; текст ссылки — данные. UI показывает
 *  текст без ссылки и может подсветить проблему. */
@Serializable data class UnresolvedLink(val reason: String) : LinkTarget

// ----------------------------------------------------------------------------
// 6. Документация. ВРЕМЕННАЯ ФОРМА.
//    Форма KDoc в KDM ещё не готова (KT-88346, G-04). Когда будет готова —
//    заменить весь этот раздел на форму KDM как есть, без второго словаря.
//    Наследование документации (KT-88347) и инлайн @sample (G-11) тоже
//    делает сборщик, до записи в модель.
// ----------------------------------------------------------------------------

@Serializable
sealed interface DocNode

@Serializable data class Paragraph(val children: List<DocNode>) : DocNode
@Serializable data class Text(val text: String) : DocNode
@Serializable data class Strong(val children: List<DocNode>) : DocNode
@Serializable data class Emphasis(val children: List<DocNode>) : DocNode
@Serializable data class InlineCode(val text: String) : DocNode
@Serializable data class CodeBlock(val language: String?, val code: String) : DocNode
@Serializable data class DocLink(val text: String, val target: LinkTarget) : DocNode
@Serializable data class Image(val url: String, val alt: String?) : DocNode
@Serializable data class DocList(val ordered: Boolean, val items: List<DocNode>) : DocNode
@Serializable data class Table(val head: List<DocNode>, val rows: List<List<DocNode>>) : DocNode

/** Структурированные теги KDoc. */
@Serializable data class ParamTag(val name: String, val body: List<DocNode>) : DocNode
@Serializable data class ReturnsTag(val body: List<DocNode>) : DocNode
@Serializable data class ThrowsTag(val target: LinkTarget, val body: List<DocNode>) : DocNode
@Serializable data class SeeAlsoTag(val target: LinkTarget, val body: List<DocNode>) : DocNode
@Serializable data class ReceiverTag(val body: List<DocNode>) : DocNode
/** Тело примера подставляет сборщик (E-024); неразрешенный @sample — ErrorNote. */
@Serializable data class SampleTag(val body: List<DocNode>) : DocNode
@Serializable data class AuthorTag(val body: List<DocNode>) : DocNode
/** Неизвестный тег. Round-trip, не дроп (E-026). */
@Serializable data class UnknownTag(val name: String, val body: List<DocNode>) : DocNode

// ----------------------------------------------------------------------------
// 7. Отдельные поля-сущности
// ----------------------------------------------------------------------------

@Serializable
enum class DeprecationLevel { WARNING, ERROR }

/**
 * Устаревание — отдельная сущность, не аннотация (G-09).
 * Сообщение — Markdown-текст, поэтому DocNode (issue 2635).
 */
@Serializable
data class Deprecation(
    val level: DeprecationLevel,
    val message: List<DocNode>,
    val replaceWith: ReplaceWith? = null,
)

@Serializable
data class ReplaceWith(
    val imports: List<String>,
    val code: String,
)

/** Ссылка "see source". Полный URL уже собран. */
@Serializable
data class SourceLink(
    val url: String,
    val line: Int? = null,
)

// ----------------------------------------------------------------------------
// 8. Чего в модели НЕТ (важно).
//
//   - дерева навигации   -> отдельный nav.json (скелет PageNode)
//   - индекса поиска     -> отдельный pages.json, собирает Kotlin из модели
//   - списка платформ и их графа -> отдельный sourcesets.json
//   - темы, ассетов, иконок, шрифтов -> константы React
//   - соответствия "вид -> иконка" (F-200) -> константы React: kind из модели,
//     картинка во фронтенде
//   - порядка чего-либо  -> порядок задаёт Kotlin (K-07)
//
// ----------------------------------------------------------------------------
// 9. Как это выглядит на стороне TS (генерация из этих классов):
//
//   type ContentNode =
//     | { kind: "declaration", variants: DeclarationVariant[] }
//     | { kind: "memberGroup", groupKind: MemberGroupKind, members: MemberEntry[] }
//     | { kind: "docBlock", documentation: DocNode[] }
//     | { kind: "errorNote", code: string, text: string }
//     | { kind: "custom", name: string, data: unknown }
//     | { kind: string }                 // неизвестный kind: пропустить, не упасть
//
//   type LinkTarget =
//     | { kind: "internalLink", url: string }
//     | { kind: "externalLink", url: string }
//     | { kind: "unresolvedLink", reason: string }
//
//   type SigToken =
//     | { kind: "keywordToken", text: string }
//     | ...
//
//   Рендер страницы, грубо:
//     content.map(node => {
//       switch (node.kind) {
//         case "declaration": return <Declaration variants={node.variants} />
//         case "memberGroup": return <MemberGroup ... />
//         default: return null               // неизвестный kind молча пропущен
//       }
//     })
//
//    Declaration внутри: один вариант — просто вёрстка; больше одного —
//    вкладки, секции или что решит дизайн. JSON от этого не меняется.
// ----------------------------------------------------------------------------
// 10. Открытые вопросы, которые видны из наброска:
//
//   - ENTRIES как MemberGroupKind против отдельных страниц CLASSLIKE
//     у enum-элементов (P-07, Q-020): сейчас и то и другое.
//   - Origin.inheritedFrom вычисляет сборщик строковой хирургией по id (Q-025),
//     пока в KDM нет поля провенанса.
//   - Аннотации в сигнатуре: в наброске их нет, а в текущем справочнике они
//     видны (E-008). Вариант: AnnotationToken с телом-токенами.
//   - MemberEntry.pageUrl == null унаследованных членов: строка без страницы —
//     это то, что рисуем? (Q-023, Q-025)
//   - DocNode — временный, ждём форму KDM (KT-88346); список тегов сверить
//     с 4.5 (Return, Parameters, Throws, See also, Samples, Author, Since,
//     Receiver, Type Parameters).
// ----------------------------------------------------------------------------
