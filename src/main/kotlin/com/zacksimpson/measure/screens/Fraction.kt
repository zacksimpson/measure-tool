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
