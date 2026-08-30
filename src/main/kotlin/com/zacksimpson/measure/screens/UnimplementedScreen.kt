package com.zacksimpson.measure.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightFullscreenModal
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController

// stand-in for menu taps that don't have a real destination yet, swap this out
// once a real screen exists.
class UnimplementedScreen(
    sealedActivity: SealedLightActivity,
    private val message: String = "Not built yet.",
) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()

        LightTheme(colors = themeColors) {
            LightFullscreenModal(
                message = message,
                onClose = { goBack(Unit) },
            )
        }
    }
}
