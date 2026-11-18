package com.salvador.salvitolauncher.ui

import android.content.Context
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import com.salvador.salvitolauncher.data.AppCategory
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Elige el texto según el idioma del launcher. */
fun tr(es: Boolean, spanish: String, english: String): String = if (es) spanish else english

fun categoryName(category: AppCategory, es: Boolean): String = when (category) {
    AppCategory.GAMES -> tr(es, "Juegos", "Games")
    AppCategory.SOCIAL -> "Social"
    AppCategory.ENTERTAINMENT -> tr(es, "Entretenimiento", "Entertainment")
    AppCategory.PRODUCTIVITY -> tr(es, "Productividad", "Productivity")
    AppCategory.UTILITIES -> tr(es, "Utilidades", "Utilities")
}

fun formatClock(context: Context, millis: Long): String {
    val pattern = if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm"
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(millis))
}

fun formatDay(millis: Long, es: Boolean): String {
    val locale = Locale.forLanguageTag(if (es) "es" else "en")
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEEdMMMM")
    return SimpleDateFormat(pattern, locale)
        .format(Date(millis))
        .replaceFirstChar { it.uppercase(locale) }
}

fun formatAlarm(millis: Long, es: Boolean): String {
    val locale = Locale.forLanguageTag(if (es) "es" else "en")
    val pattern = DateFormat.getBestDateTimePattern(locale, "EEEjm")
    return SimpleDateFormat(pattern, locale)
        .format(Date(millis))
        .replaceFirstChar { it.uppercase(locale) }
}

/** Hora actual en milisegundos, se refresca cada 15 s. */
@Composable
fun rememberNow(): State<Long> = produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
        value = System.currentTimeMillis()
        delay(15_000L)
    }
}

/** Evaluación rápida de expresiones matemáticas para Spotlight. */
fun evaluateMathExpression(expr: String): Double? {
    val clean = expr.trim().replace(" ", "").replace("x", "*").replace("X", "*").replace(",", ".")
    if (!clean.matches(Regex("""^[\d\.\+\-\*\/\(\)]+$"""))) return null
    return try {
        val tokens = clean
        object {
            var pos = -1
            var ch = 0

            fun nextChar() {
                ch = if (++pos < tokens.length) tokens[pos].code else -1
            }

            fun eat(charToEat: Int): Boolean {
                while (ch == ' '.code) nextChar()
                if (ch == charToEat) {
                    nextChar()
                    return true
                }
                return false
            }

            fun parse(): Double {
                nextChar()
                val x = parseExpression()
                if (pos < tokens.length) throw RuntimeException("Unexpected: " + ch.toChar())
                return x
            }

            fun parseExpression(): Double {
                var x = parseTerm()
                while (true) {
                    when {
                        eat('+'.code) -> x += parseTerm()
                        eat('-'.code) -> x -= parseTerm()
                        else -> return x
                    }
                }
            }

            fun parseTerm(): Double {
                var x = parseFactor()
                while (true) {
                    when {
                        eat('*'.code) -> x *= parseFactor()
                        eat('/'.code) -> x /= parseFactor()
                        else -> return x
                    }
                }
            }

            fun parseFactor(): Double {
                if (eat('+'.code)) return parseFactor()
                if (eat('-'.code)) return -parseFactor()
                var x: Double
                val startPos = pos
                if (eat('('.code)) {
                    x = parseExpression()
                    eat(')'.code)
                } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                    while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                    x = tokens.substring(startPos, pos).toDouble()
                } else {
                    throw RuntimeException("Unexpected: " + ch.toChar())
                }
                return x
            }
        }.parse()
    } catch (_: Exception) {
        null
    }
}
