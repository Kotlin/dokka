package org.jetbrains.dokka.pages

interface MultimoduleRootPage : ContentPage

interface ModulePage : ContentPage, WithDocumentables

interface PackagePage : ContentPage, WithDocumentables

interface ClasslikePage : ContentPage, WithDocumentables

interface MemberPage : ContentPage, WithDocumentables