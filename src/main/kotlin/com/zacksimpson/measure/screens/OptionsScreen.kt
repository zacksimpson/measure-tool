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
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

data class ViewOption(val key: String, val label: String)

// centered, evenly spaced, tap a line to return its key.
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
                    .padding(top = 6.5f.gridUnitsAsDp(), bottom = 4f.gridUnitsAsDp()),
                verticalArrangement = Arrangement.spacedBy(2.25f.gridUnitsAsDp()),
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
