package org.kotlintestmpp

expect fun getCurrentDate(): String

fun getDate(): String {
    return "Today's Date is ${getCurrentDate()}"
}

