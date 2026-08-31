package com.zacksimpson.measure.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
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
import com.thelightphone.sdk.ui.lightClickable
import com.zacksimpson.measure.data.CalcHistoryRepository

// long-press menu for a calculator's display: copy the current value, or see recent
// results. fixed two-item menu with its own actions instead of a goBack(key) picker.
class ResultActionsScreen(
    sealedActivity: SealedLightActivity,
    private val value: String,
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val clipboardManager = LocalClipboardManager.current
        val historyRepo = remember { CalcHistoryRepository(lightContext.dataStore) }
        val history by historyRepo.entries.collectAsState(initial = emptyList())

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
                    ActionRow(
                        text = "Copy",
                        onClick = {
                            clipboardManager.setText(AnnotatedString(value))
                            navigateTo(
                                screenFactory = { ToastScreen(it, "Copied") },
                                resultCallback = { goBack(Unit) },
                            )
                        },
                    )
                    ActionRow(
                        text = "View History",
                        onClick = { navigateTo(screenFactory = { CalcHistoryScreen(it, history) }) },
                    )
                }
                LightBottomBar(
                    items = listOf(LightBarButton.LightIcon(icon = LightIcons.CLOSE, onClick = { goBack(Unit) })),
                )
            }
        }
    }

    @Composable
    private fun ActionRow(text: String, onClick: () -> Unit) {
        LightText(
            text = text,
            variant = LightTextVariant.Heading,
            modifier = Modifier
                .fillMaxWidth()
                .lightClickable(onClick = onClick)
                .padding(horizontal = 2.25f.gridUnitsAsDp(), vertical = 0.6f.gridUnitsAsDp()),
        )
    }
}
