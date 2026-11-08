package pl.antek.punkty.logic

import kotlin.math.abs

fun plural(n: Int, one: String, few: String, many: String): String {
    val a = abs(n)
    return when {
        a == 1 -> one
        a % 10 in 2..4 && a % 100 !in 12..14 -> few
        else -> many
    }
}

fun pointsWord(n: Int) = plural(n, "punkt", "punkty", "punktów")
fun daysWord(n: Int) = if (abs(n) == 1) "dzień" else "dni"
fun timesWord(n: Int) = plural(n, "raz", "razy", "razy")

fun signed(n: Int) = if (n > 0) "+$n" else if (n < 0) "−${abs(n)}" else "0"
