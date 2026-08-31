package com.zacksimpson.measure.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightScrollView
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

data class ViewOption(val key: String, val label: String)

private val TopPadding = 6.5f
private val BottomPadding = 4f
private val ItemSpacing = 2.25f

// centered, evenly spaced, tap a line to return its key. scrolls once the list
// is too long to fit (LightScrollView doesn't expose verticalArrangement, so the
// same top/bottom/between-item spacing a plain Column would've given via
// Arrangement.spacedBy is replicated by hand with spacers instead).
class OptionsScreen(
    sealedActivity: SealedLightActivity,
    private val options: List<ViewOption>,
) : SimpleLightScreen<String>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            LightScrollView(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Spacer(modifier = Modifier.height(TopPadding.gridUnitsAsDp()))
                options.forEachIndexed { index, option ->
                    if (index > 0) {
                        Spacer(modifier = Modifier.height(ItemSpacing.gridUnitsAsDp()))
                    }
                    LightText(
                        text = option.label,
                        variant = LightTextVariant.Heading,
                        align = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .lightClickable { goBack(option.key) },
                    )
                }
                Spacer(modifier = Modifier.height(BottomPadding.gridUnitsAsDp()))
            }
        }
    }
}
