package com.zacksimpson.measure.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightGrid
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

data class ViewOption(val key: String, val label: String)

private const val TopPaddingUnits = 6.5f
private const val BottomPaddingUnits = 4f
private const val MaxItemSpacingUnits = 2.25f

// one Heading-variant line's rendered height in grid units, measured off-device
// (step between item centers at the tuned 2.25-unit spacing was ~4.575 units,
// so height = 4.575 - 2.25 = 2.325), plus a small margin for safety.
private const val ItemHeightUnits = 2.35f
private const val SafetyMarginUnits = 0.5f

// no scrolling, ever: this never shows a list longer than one screen can hold,
// so item spacing shrinks (down from the tuned 2.25 units) as the list grows,
// rather than letting it overflow. LightGrid.HEIGHT is a width-based unit here
// like everywhere else in this screen, valid because the LP3's fixed 1080x1240
// resolution makes one width unit equal one height unit in px.
private fun itemSpacingUnits(itemCount: Int): Float {
    if (itemCount <= 1) return MaxItemSpacingUnits
    val budget = LightGrid.HEIGHT - TopPaddingUnits - BottomPaddingUnits - SafetyMarginUnits
    val fitSpacing = (budget - itemCount * ItemHeightUnits) / (itemCount - 1)
    return minOf(MaxItemSpacingUnits, fitSpacing)
}

// centered, tap a line to return its key.
class OptionsScreen(
    sealedActivity: SealedLightActivity,
    private val options: List<ViewOption>,
) : SimpleLightScreen<String>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background)
                    .padding(top = TopPaddingUnits.gridUnitsAsDp(), bottom = BottomPaddingUnits.gridUnitsAsDp()),
                verticalArrangement = Arrangement.spacedBy(itemSpacingUnits(options.size).gridUnitsAsDp()),
            ) {
                options.forEach { option ->
                    LightText(
                        text = option.label,
                        variant = LightTextVariant.Heading,
                        align = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .lightClickable { goBack(option.key) },
                    )
                }
            }
        }
    }
}
