package org.jetbrains.dokka.uitest.kmp.date

/**
 * Common `expect` declaration
 */
expect fun getCurrentDate(): String

/**
 * Common date util function
 */
fun getDate(): String {
    return "Today's Date is ${getCurrentDate()}"
}

