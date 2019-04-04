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

package org.jetbrains.dokka.Utilities

/**
 * Finds the first sentence of a string, accounting for periods that may occur in parenthesis.
 */
fun String.firstSentence(): String {

    // First, search for location of first period and first parenthesis.
    val firstPeriodIndex = this.indexOf('.')
    val openParenIndex = this.indexOf('(')

    // If there is no opening parenthesis found or if it occurs after the occurrence of the first period, just return
    // the first sentence, or the entire string if no period is found.
    if (openParenIndex == -1 || openParenIndex > firstPeriodIndex) {
        return if (firstPeriodIndex != -1) {
            this.substring(0, firstPeriodIndex + 1)
        } else {
            this
        }
    }

    // At this point we know that the opening parenthesis occurs before the first period, so we look for the matching
    // closing parenthesis.
    val closeParenIndex = this.indexOf(')', openParenIndex)

    // If a matching closing parenthesis is found, take that substring and recursively process the rest of the string.
    // This is to accommodate periods inside of parenthesis.  If a matching closing parenthesis is not found, return the
    // original string.
    return if (closeParenIndex != -1) {
        this.substring(0, closeParenIndex) + this.substring(closeParenIndex, this.length).firstSentence()
    } else {
        this
    }

}