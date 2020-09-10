package org.jetbrains.dokka.pages

interface Content : ContentNode

interface Text: Content {
    val text: String
}

interface BreakLine: Content

interface Header: Content, ContentComposite {
    val level: Int
}

interface Code: Content, ContentComposite

interface Link: Content, ContentComposite

interface Table: Content, ContentComposite

interface ElementList: Content, ContentComposite

interface Group: Content, ContentComposite

interface DivergentGroup: Content, ContentComposite

interface DivergentInstance: Content

interface PlatformHinted: Content

