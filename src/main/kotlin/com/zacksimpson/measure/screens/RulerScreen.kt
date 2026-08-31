package com.zacksimpson.measure.screens

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.sp
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.SimpleLightScreen
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable

// draws in raw device pixels off the panel's actual reported physical density, not
// Compose's dp/density bucket (480dpi -> 3.0x), which on this hardware is a rounded
// UI-scaling value and doesn't match the true pixel pitch (measured via adb dumpsys
// display on an LP3: yDpi=321.387). LocalContext is sandboxed off in Light SDK apps,
// so this is hardcoded rather than read at runtime, same as targeting one fixed panel.
// metric on the left (mm, cm labeled), imperial on the right (1/16", in labeled).
private const val PHYSICAL_Y_DPI = 321.387f

// both ends have a 60px-radius hardware corner clip (see dumpsys display), so ticks
// stop short of it on both ends instead of reserving a bottom bar's fixed height.
private const val TOP_MARGIN_PX = 70f
private const val BOTTOM_MARGIN_PX = 70f
private const val LABEL_GAP_PX = 6f

// no bottom bar here (it would eat into the ruler's length), so the LIST icon is
// placed by hand at the exact spot the bar used to put a close icon, measured off
// a screenshot: a 2-grid-unit icon centered at (539.5, 1159.5) raw px.
private const val LIST_ICON_SIZE_UNITS = 2f
private const val LIST_ICON_CENTER_X_PX = 539.5f
private const val LIST_ICON_CENTER_Y_PX = 1159.5f

class RulerScreen(sealedActivity: SealedLightActivity) : SimpleLightScreen<Unit>(sealedActivity) {

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val contentColor = LightThemeTokens.colors.content
        val pxPerMm = PHYSICAL_Y_DPI / 25.4f
        val pxPerSixteenth = PHYSICAL_Y_DPI / 16f
        val labelTextSizePx = with(LocalDensity.current) { 13.sp.toPx() }
        val labelPaint = remember(contentColor, labelTextSizePx) {
            Paint().apply {
                isAntiAlias = true
                color = contentColor.toArgb()
                textSize = labelTextSizePx
            }
        }

        val density = LocalDensity.current
        val listIconSizePx = with(density) { LIST_ICON_SIZE_UNITS.gridUnitsAsDp().toPx() }
        val listIconOffsetX = with(density) { (LIST_ICON_CENTER_X_PX - listIconSizePx / 2f).toDp() }
        val listIconOffsetY = with(density) { (LIST_ICON_CENTER_Y_PX - listIconSizePx / 2f).toDp() }

        LightTheme(colors = themeColors) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val bottomLimit = size.height - BOTTOM_MARGIN_PX

                    var mm = 0
                    while (true) {
                        val y = TOP_MARGIN_PX + mm * pxPerMm
                        if (y > bottomLimit) break
                        val isCm = mm % 10 == 0
                        val isHalfCm = mm % 5 == 0
                        val length = if (isCm) 65f else if (isHalfCm) 42f else 24f
                        drawLine(
                            color = contentColor,
                            start = Offset(0f, y),
                            end = Offset(length, y),
                            strokeWidth = if (isCm) 4f else 2.5f,
                        )
                        if (isCm) {
                            val label = (mm / 10).toString()
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(
                                    label,
                                    length + LABEL_GAP_PX,
                                    y + labelPaint.textSize / 3f,
                                    labelPaint,
                                )
                            }
                        }
                        mm++
                    }

                    var sixteenth = 0
                    while (true) {
                        val y = TOP_MARGIN_PX + sixteenth * pxPerSixteenth
                        if (y > bottomLimit) break
                        val isInch = sixteenth % 16 == 0
                        val isHalfInch = sixteenth % 8 == 0
                        val isQuarterInch = sixteenth % 4 == 0
                        val isEighthInch = sixteenth % 2 == 0
                        val length = when {
                            isInch -> 65f
                            isHalfInch -> 48f
                            isQuarterInch -> 40f
                            isEighthInch -> 32f
                            else -> 24f
                        }
                        drawLine(
                            color = contentColor,
                            start = Offset(size.width - length, y),
                            end = Offset(size.width, y),
                            strokeWidth = if (isInch) 4f else 2.5f,
                        )
                        if (isInch) {
                            val label = (sixteenth / 16).toString()
                            val labelWidth = labelPaint.measureText(label)
                            drawIntoCanvas { canvas ->
                                canvas.nativeCanvas.drawText(
                                    label,
                                    size.width - length - LABEL_GAP_PX - labelWidth,
                                    y + labelPaint.textSize / 3f,
                                    labelPaint,
                                )
                            }
                        }
                        sixteenth++
                    }
                }

                LightIcon(
                    icon = LightIcons.LIST,
                    size = LIST_ICON_SIZE_UNITS,
                    modifier = Modifier
                        .offset(x = listIconOffsetX, y = listIconOffsetY)
                        .lightClickable { openToolsMenu("ruler") },
                )
            }
        }
    }
}
