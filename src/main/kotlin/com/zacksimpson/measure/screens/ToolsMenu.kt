package com.zacksimpson.measure.screens

import com.thelightphone.sdk.SimpleLightScreen
import com.zacksimpson.measure.MainScreen

// every calculator's LIST icon opens the same menu. shared here instead of
// duplicated per screen since, unlike the calculator grids, this content and
// routing never differs by which screen opened it.
private val toolOptions = listOf(
    ViewOption("standard", "Standard"),
    ViewOption("fraction-calc", "Fraction Calc"),
    ViewOption("convert-units", "Convert Units"),
    ViewOption("ruler", "Ruler"),
)

// currentKey is the tool the calling screen already is, so selecting it is a
// no-op instead of navigating to itself.
fun SimpleLightScreen<*>.openToolsMenu(currentKey: String) {
    navigateTo(
        screenFactory = { OptionsScreen(it, toolOptions) },
        resultCallback = { key ->
            if (key == currentKey) return@navigateTo
            when (key) {
                "standard" -> navigateTo(screenFactory = { MainScreen(it) })
                "fraction-calc" -> navigateTo(screenFactory = { FractionCalcScreen(it) })
                "convert-units" -> navigateTo(screenFactory = { ConvertUnitsScreen(it) })
                "ruler" -> navigateTo(screenFactory = { RulerScreen(it) })
                else -> {
                    val label = toolOptions.first { it.key == key }.label
                    navigateTo(screenFactory = { UnimplementedScreen(it, "$label: not built yet.") })
                }
            }
        },
    )
}
