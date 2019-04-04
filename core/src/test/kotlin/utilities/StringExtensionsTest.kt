/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.dokka.tests.utilities

import org.jetbrains.dokka.Utilities.firstSentence
import org.junit.Assert.assertEquals
import org.junit.Test

class StringExtensionsTest {

    @Test
    fun firstSentence_emptyString() {
        assertEquals("", "".firstSentence())
    }

    @Test
    fun incompleteSentence() {
        assertEquals("Hello there", "Hello there".firstSentence())
    }

    @Test
    fun incompleteSentence_withParenthesis() {
        assertEquals("Hello there (hi)", "Hello there (hi)".firstSentence())
        assertEquals("Hello there (hi.)", "Hello there (hi.)".firstSentence())
    }

    @Test
    fun incompleteSentence_apiLevel() {
        assertEquals("API level 8 (Android 2.2, Froyo)", "API level 8 (Android 2.2, Froyo)".firstSentence())
    }

    @Test
    fun unmatchedClosingParen() {
        assertEquals(
                "A notation either declares, by name, the format of an unparsed entity (see \n",
                "A notation either declares, by name, the format of an unparsed entity (see \n".firstSentence()
        )
    }

    @Test
    fun unmatchedClosingParen_withFullFirstSentence() {
        assertEquals(
                "This interface represents a notation declared in the DTD.",
                ("This interface represents a notation declared in the DTD. A notation either declares, by name, " +
                        "the format of an unparsed entity (see \n").firstSentence()
        )
    }

    @Test
    fun firstSentence_singleSentence() {
        assertEquals("Hello there.", "Hello there.".firstSentence())
    }

    @Test
    fun firstSentence_multipleSentences() {
        assertEquals("Hello there.", "Hello there. How are you?".firstSentence())
    }

    @Test
    fun firstSentence_singleSentence_withParenthesis() {
        assertEquals("API level 28 (Android Pie).", "API level 28 (Android Pie).".firstSentence())
    }

    @Test
    fun firstSentence_multipleSentences_withParenthesis() {
        assertEquals(
                "API level 28 (Android Pie).",
                "API level 28 (Android Pie). API level 27 (Android Oreo)".firstSentence()
        )
    }

    @Test
    fun firstSentence_singleSentence_withPeriodInParenthesis() {
        assertEquals("API level 28 (Android 9.0 Pie).", "API level 28 (Android 9.0 Pie).".firstSentence())
    }

    @Test
    fun firstSentence_multipleSentences_withPeriodInParenthesis() {
        assertEquals(
                "API level 28 (Android 9.0 Pie).",
                "API level 28 (Android 9.0 Pie). API level 27 (Android 8.0 Oreo).".firstSentence()
        )
    }

    @Test
    fun parenthesisWithperiod_notFirstSentence() {
        assertEquals("Foo bar.", "Foo bar. Baz (Wow)".firstSentence())
        assertEquals("Foo bar.", "Foo bar. Baz (Wow).".firstSentence())
    }

    @Test
    fun periodInsideParenthesis() {
        assertEquals(
                "A ViewGroup is a special view that can contain other views (called children.) " +
                        "The view group is the base class for layouts and views containers.",
                ("A ViewGroup is a special view that can contain other views (called children.) " +
                        "The view group is the base class for layouts and views containers. " +
                        "This class also defines the android.view.ViewGroup.LayoutParams class " +
                        "which serves as the base class for layouts parameters.").firstSentence()
        )
        assertEquals("Foo (Foo.) bar.", "Foo (Foo.) bar. Baz.".firstSentence())
        assertEquals("Foo (Foo.) bar (bar.) baz.", "Foo (Foo.) bar (bar.) baz. Wow".firstSentence())
        assertEquals("Foo (Foo.) bar (bar.) baz (baz.) Wow", "Foo (Foo.) bar (bar.) baz (baz.) Wow".firstSentence())
    }
}