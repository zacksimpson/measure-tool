package com.zacksimpson.measure.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.LightScreen
import com.thelightphone.sdk.LightViewModel
import com.thelightphone.sdk.SealedLightActivity
import com.thelightphone.sdk.ui.LightIcon
import com.thelightphone.sdk.ui.LightIconConfiguration
import com.thelightphone.sdk.ui.LightIcons
import com.thelightphone.sdk.ui.LightTheme
import com.thelightphone.sdk.ui.LightThemeController
import com.thelightphone.sdk.ui.LightThemeTokens
import com.thelightphone.sdk.ui.designVerticalPxToSp
import com.thelightphone.sdk.ui.gridUnitsAsDp
import com.thelightphone.sdk.ui.lightClickable
import com.zacksimpson.measure.data.CalcHistoryRepository
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// same grid, spacing, and type scale as MainScreen (copied, not shared, see
// measure-tool's own notes on that choice). no arithmetic here, so the operator
// slots become from/to unit pickers and a swap button instead.
private const val MAX_DISPLAY_LENGTH = 10

enum class LengthUnit(
    val shortLabel: String,
    val fullName: String,
    val metersPerUnit: Double,
    // "mm" is two of the widest letters in the font, clips to "m" at the same
    // scale everything else uses comfortably.
    val labelScale: Float = 0.7f,
) {
    INCH("in", "Inches", 0.0254),
    FOOT("ft", "Feet", 0.3048),
    YARD("yd", "Yards", 0.9144),
    MILLIMETER("mm", "Millimeters", 0.001, labelScale = 0.6f),
    CENTIMETER("cm", "Centimeters", 0.01),
    METER("m", "Meters", 1.0),
}

class ConvertUnitsScreenViewModel(private val historyRepo: CalcHistoryRepository) : LightViewModel<Unit>() {

    private var startingNewEntry = true

    private val _display = MutableStateFlow("0")
    val display: StateFlow<String> = _display.asStateFlow()

    private val _fromUnit = MutableStateFlow(LengthUnit.INCH)
    val fromUnit: StateFlow<LengthUnit> = _fromUnit.asStateFlow()

    private val _toUnit = MutableStateFlow(LengthUnit.FOOT)
    val toUnit: StateFlow<LengthUnit> = _toUnit.asStateFlow()

    fun setFromUnit(unit: LengthUnit) {
        _fromUnit.value = unit
    }

    fun setToUnit(unit: LengthUnit) {
        _toUnit.value = unit
    }

    fun swapUnits() {
        val previousFrom = _fromUnit.value
        _fromUnit.value = _toUnit.value
        _toUnit.value = previousFrom
    }

    fun inputDigit(digit: String) {
        val current = _display.value
        val next = when {
            startingNewEntry || current == "0" -> digit
            else -> current + digit
        }
        if (next.length > MAX_DISPLAY_LENGTH) return
        _display.value = next
        startingNewEntry = false
    }

    fun inputDecimal() {
        if (startingNewEntry) {
            _display.value = "0."
            startingNewEntry = false
            return
        }
        if (_display.value.length >= MAX_DISPLAY_LENGTH) return
        if (!_display.value.contains(".")) {
            _display.value += "."
        }
    }

    fun backspace() {
        if (_display.value == "Error") {
            clear()
            return
        }
        val trimmed = _display.value.dropLast(1)
        _display.value = if (trimmed.isEmpty()) "0" else trimmed
        startingNewEntry = _display.value == "0"
    }

    fun clear() {
        startingNewEntry = true
        _display.value = "0"
    }

    fun convert() {
        val value = _display.value.toDoubleOrNull() ?: 0.0
        val meters = value * _fromUnit.value.metersPerUnit
        _display.value = formatValue(meters / _toUnit.value.metersPerUnit)
        startingNewEntry = true
        if (_display.value != "Error") {
            viewModelScope.launch { historyRepo.record(_display.value) }
        }
    }

    private fun formatValue(value: Double): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        if (value == 0.0) return "0"

        val magnitude = abs(value)
        val plain = when {
            magnitude < 1e-6 -> null
            value == value.toLong().toDouble() && magnitude < 1e15 -> value.toLong().toString()
            else -> "%.8f".format(value).trimEnd('0').trimEnd('.')
        }

        return if (plain != null && plain.length <= MAX_DISPLAY_LENGTH) {
            plain
        } else {
            "%.2e".format(value)
        }
    }
}

class ConvertUnitsScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, ConvertUnitsScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<ConvertUnitsScreenViewModel>
        get() = ConvertUnitsScreenViewModel::class.java

    override fun createViewModel(): ConvertUnitsScreenViewModel =
        ConvertUnitsScreenViewModel(CalcHistoryRepository(lightContext.dataStore))

    private val unitOptions = LengthUnit.entries.map { ViewOption(it.name, it.fullName) }

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val display by viewModel.display.collectAsState()
        val fromUnit by viewModel.fromUnit.collectAsState()
        val toUnit by viewModel.toUnit.collectAsState()

        LightTheme(colors = themeColors) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(LightThemeTokens.colors.background),
            ) {
                DisplayRow(
                    value = display,
                    onBackspace = viewModel::backspace,
                    onLongPress = { navigateTo(screenFactory = { ResultActionsScreen(it, display) }) },
                    modifier = Modifier.weight(1f),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        ConvertUnitsButton.Label("C", onClick = viewModel::clear),
                        ConvertUnitsButton.Label(fromUnit.shortLabel, scale = fromUnit.labelScale, onClick = ::openFromPicker),
                        ConvertUnitsButton.Icon(
                            LightIcons.REVERSE_ORDER,
                            rotationDegrees = 90f,
                            onClick = viewModel::swapUnits,
                        ),
                        ConvertUnitsButton.Label(toUnit.shortLabel, scale = toUnit.labelScale, onClick = ::openToPicker),
                    ),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        ConvertUnitsButton.Label("7") { viewModel.inputDigit("7") },
                        ConvertUnitsButton.Label("8") { viewModel.inputDigit("8") },
                        ConvertUnitsButton.Label("9") { viewModel.inputDigit("9") },
                        null,
                    ),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        ConvertUnitsButton.Label("4") { viewModel.inputDigit("4") },
                        ConvertUnitsButton.Label("5") { viewModel.inputDigit("5") },
                        ConvertUnitsButton.Label("6") { viewModel.inputDigit("6") },
                        null,
                    ),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        ConvertUnitsButton.Label("1") { viewModel.inputDigit("1") },
                        ConvertUnitsButton.Label("2") { viewModel.inputDigit("2") },
                        ConvertUnitsButton.Label("3") { viewModel.inputDigit("3") },
                        null,
                    ),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        ConvertUnitsButton.Icon(LightIcons.LIST, onClick = { openToolsMenu("convert-units") }),
                        ConvertUnitsButton.Label("0") { viewModel.inputDigit("0") },
                        ConvertUnitsButton.Label(".", onClick = viewModel::inputDecimal),
                        ConvertUnitsButton.Label("=", onClick = viewModel::convert),
                    ),
                )
            }
        }
    }

    private fun openFromPicker() {
        navigateTo(
            screenFactory = { OptionsScreen(it, unitOptions) },
            resultCallback = { key -> viewModel.setFromUnit(LengthUnit.valueOf(key)) },
        )
    }

    private fun openToPicker() {
        navigateTo(
            screenFactory = { OptionsScreen(it, unitOptions) },
            resultCallback = { key -> viewModel.setToUnit(LengthUnit.valueOf(key)) },
        )
    }

}

private sealed interface ConvertUnitsButton {
    val onClick: () -> Unit

    data class Label(val text: String, val scale: Float = 1f, override val onClick: () -> Unit) : ConvertUnitsButton
    data class Icon(
        val icon: LightIconConfiguration,
        val rotationDegrees: Float = 0f,
        override val onClick: () -> Unit,
    ) : ConvertUnitsButton
}

private val ButtonInset = 3.6f
private val RightGutter = 2.3f
private const val GridFontScale = 1.196f

@Composable
private fun gridTextStyle(scale: Float = 1f): TextStyle {
    val base = LightThemeTokens.typography.heading
    val factor = GridFontScale * scale
    return base.copy(
        fontSize = (base.fontSize.value * factor).designVerticalPxToSp(),
        lineHeight = (base.lineHeight.value * factor).designVerticalPxToSp(),
    )
}

@Composable
private fun DisplayRow(
    value: String,
    onBackspace: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = RightGutter.gridUnitsAsDp()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .combinedClickable(onClick = {}, onLongClick = onLongPress),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = value,
                style = gridTextStyle(),
                color = LightThemeTokens.colors.content,
                maxLines = 1,
                overflow = TextOverflow.Clip,
            )
        }
        LightIcon(
            icon = LightIcons.BACK,
            size = 1.9f,
            modifier = Modifier
                .padding(start = 0.5f.gridUnitsAsDp())
                .lightClickable(onClick = onBackspace),
        )
    }
}

@Composable
private fun CalculatorRow(buttons: List<ConvertUnitsButton?>, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth().padding(end = RightGutter.gridUnitsAsDp())) {
        buttons.forEach { button ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                when (button) {
                    null -> Unit
                    is ConvertUnitsButton.Label -> Text(
                        text = button.text,
                        style = gridTextStyle(button.scale),
                        color = LightThemeTokens.colors.content,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier
                            .padding(start = ButtonInset.gridUnitsAsDp())
                            .lightClickable(onClick = button.onClick),
                    )
                    is ConvertUnitsButton.Icon -> LightIcon(
                        icon = button.icon,
                        size = 1.7f,
                        modifier = Modifier
                            .padding(start = ButtonInset.gridUnitsAsDp())
                            .rotate(button.rotationDegrees)
                            .lightClickable(onClick = button.onClick),
                    )
                }
            }
        }
    }
}
