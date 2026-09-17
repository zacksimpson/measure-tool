package com.zacksimpson.measure.screens

import kotlin.math.abs

data class Fraction(val numerator: Long, val denominator: Long) {
    fun reduced(): Fraction {
        if (numerator == 0L) return Fraction(0, 1)
        val g = gcd(abs(numerator), abs(denominator))
        val sign = if (denominator < 0) -1 else 1
        return Fraction(sign * numerator / g, sign * denominator / g)
    }
}

private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

// unicode has dedicated single-character glyphs for these; anything else (sixteenths,
// thirty-seconds, and so on -- the common carpentry denominators) has no such glyph,
// so it stays as plain "n/d" text.
private val vulgarFractionGlyphs: Map<Pair<Long, Long>, String> = mapOf(
    (1L to 2L) to "½",
    (1L to 3L) to "⅓", (2L to 3L) to "⅔",
    (1L to 4L) to "¼", (3L to 4L) to "¾",
    (1L to 5L) to "⅕", (2L to 5L) to "⅖", (3L to 5L) to "⅗", (4L to 5L) to "⅘",
    (1L to 6L) to "⅙", (5L to 6L) to "⅚",
    (1L to 7L) to "⅐",
    (1L to 8L) to "⅛", (3L to 8L) to "⅜", (5L to 8L) to "⅝", (7L to 8L) to "⅞",
    (1L to 9L) to "⅑",
    (1L to 10L) to "⅒",
)

// renders a proper fraction as its single-character glyph when one exists ("¾"),
// falling back to plain "n/d" text otherwise. numerator/denominator are assumed
// already reduced and non-negative.
fun fractionGlyph(numerator: Long, denominator: Long): String =
    vulgarFractionGlyphs[numerator to denominator] ?: "$numerator/$denominator"

// matches a bare "n/d" fraction inside an already-built display string, so live
// entry can be re-rendered with glyphs without touching how it's parsed.
private val fractionGlyphPattern = Regex("""(\d+)/(\d+)""")

fun applyFractionGlyphs(text: String): String =
    fractionGlyphPattern.replace(text) { match ->
        fractionGlyph(match.groupValues[1].toLong(), match.groupValues[2].toLong())
    }

// joins a mixed number's whole part with its (already glyph-converted) fraction
// part: butted right up against a single glyph ("12¾"), or with a space whenever
// it's still plain digits -- a fallback "n/d" ("12 3/16", not "123/16") or a
// fraction that's still mid-entry ("12 3", not "123", before the "/" lands).
fun joinMixedNumber(whole: String, fractionDisplay: String): String {
    val isGlyph = fractionDisplay.length == 1 && !fractionDisplay[0].isDigit()
    val separator = if (fractionDisplay.isEmpty() || isGlyph) "" else " "
    return "$whole$separator$fractionDisplay"
}

enum class Operator {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE;

    fun apply(a: Fraction, b: Fraction): Fraction = when (this) {
        ADD -> Fraction(a.numerator * b.denominator + b.numerator * a.denominator, a.denominator * b.denominator)
        SUBTRACT -> Fraction(a.numerator * b.denominator - b.numerator * a.denominator, a.denominator * b.denominator)
        MULTIPLY -> Fraction(a.numerator * b.numerator, a.denominator * b.denominator)
        DIVIDE -> Fraction(a.numerator * b.denominator, a.denominator * b.numerator)
    }.reduced()
}
