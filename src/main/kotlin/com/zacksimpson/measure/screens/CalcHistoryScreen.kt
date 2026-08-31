package com.zacksimpson.measure.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightBarButton
import com.thelightphone.sdk.ui.LightBottomBar
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightText
import com.thelightphone.sdk.ui.LightTextVariant
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.zacksimpson.measure.data.CalcHistoryEntry

// most recent results across every calculator, newest first. plain read-only list,
// no picker semantics, so this doesn't reuse OptionsScreen.
class CalcHistoryScreen(
    sealedActivity: SealedLightActivity,
    private val history: List<CalcHistoryEntry>,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 1.5f.gridUnitsAsDp()),
                ) {
                    if (history.isEmpty()) {
                        HistoryRow("No history yet")
                    } else {
                        history.forEach { entry -> HistoryRow(entry.result) }
                    }
                }
                LightBottomBar(
                    items = listOf(LightBarButton.LightIcon(icon = LightIcons.CLOSE, onClick = { goBack(Unit) })),
                )
            }
        }
    }

    @Composable
    private fun HistoryRow(text: String) {
        LightText(
            text = text,
            variant = LightTextVariant.Heading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.25f.gridUnitsAsDp(), vertical = 0.6f.gridUnitsAsDp()),
        )
    }
}
