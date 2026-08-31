package com.zacksimpson.measure

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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewModelScope
import com.thelightphone.sdk.InitialScreen
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
import com.zacksimpson.measure.screens.ResultActionsScreen
import com.zacksimpson.measure.screens.openToolsMenu
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// max characters (digits, sign, decimal point) the display will show before
// switching to exponent notation, so a number can never overflow or wrap.
private const val MAX_DISPLAY_LENGTH = 10

enum class Operator {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE;

    fun apply(a: Double, b: Double): Double = when (this) {
        ADD -> a + b
        SUBTRACT -> a - b
        MULTIPLY -> a * b
        DIVIDE -> a / b
    }
}

class MainScreenViewModel(private val historyRepo: CalcHistoryRepository) : LightViewModel<Unit>() {

    private var accumulator: Double? = null
    private var pendingOperator: Operator? = null
    private var startingNewEntry = true

    private val _display = MutableStateFlow("0")
    val display: StateFlow<String> = _display.asStateFlow()

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

    fun toggleSign() {
        val current = _display.value
        if (current == "0") return
        val next = if (current.startsWith("-")) current.removePrefix("-") else "-$current"
        if (next.length > MAX_DISPLAY_LENGTH) return
        _display.value = next
    }

    fun backspace() {
        if (_display.value == "Error") {
            clear()
            return
        }
        val trimmed = _display.value.dropLast(1)
        _display.value = if (trimmed.isEmpty() || trimmed == "-") "0" else trimmed
        startingNewEntry = _display.value == "0"
    }

    fun clear() {
        accumulator = null
        pendingOperator = null
        startingNewEntry = true
        _display.value = "0"
    }

    fun setOperator(operator: Operator) {
        val current = _display.value.toDoubleOrNull() ?: 0.0
        accumulator = if (pendingOperator != null && !startingNewEntry) {
            pendingOperator!!.apply(accumulator ?: 0.0, current)
        } else {
            accumulator ?: current
        }
        pendingOperator = operator
        startingNewEntry = true
        _display.value = formatValue(accumulator ?: current)
    }

    fun equals() {
        val operator = pendingOperator ?: return
        val current = _display.value.toDoubleOrNull() ?: 0.0
        val result = operator.apply(accumulator ?: 0.0, current)
        _display.value = formatValue(result)
        accumulator = null
        pendingOperator = null
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

@InitialScreen
class MainScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, MainScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<MainScreenViewModel>
        get() = MainScreenViewModel::class.java

    override fun createViewModel(): MainScreenViewModel =
        MainScreenViewModel(CalcHistoryRepository(lightContext.dataStore))

    @Composable
    override fun Content() {
        val themeColors by LightThemeController.colors.collectAsState()
        val display by viewModel.display.collectAsState()

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
                        CalculatorButton.Label("C", viewModel::clear),
                        null,
                        CalculatorButton.Label("±", viewModel::toggleSign),
                        CalculatorButton.Label("÷") { viewModel.setOperator(Operator.DIVIDE) },
                    ),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        CalculatorButton.Label("7") { viewModel.inputDigit("7") },
                        CalculatorButton.Label("8") { viewModel.inputDigit("8") },
                        CalculatorButton.Label("9") { viewModel.inputDigit("9") },
                        CalculatorButton.Label("×") { viewModel.setOperator(Operator.MULTIPLY) },
                    ),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        CalculatorButton.Label("4") { viewModel.inputDigit("4") },
                        CalculatorButton.Label("5") { viewModel.inputDigit("5") },
                        CalculatorButton.Label("6") { viewModel.inputDigit("6") },
                        CalculatorButton.Label("-") { viewModel.setOperator(Operator.SUBTRACT) },
                    ),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        CalculatorButton.Label("1") { viewModel.inputDigit("1") },
                        CalculatorButton.Label("2") { viewModel.inputDigit("2") },
                        CalculatorButton.Label("3") { viewModel.inputDigit("3") },
                        CalculatorButton.Label("+") { viewModel.setOperator(Operator.ADD) },
                    ),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        CalculatorButton.Icon(LightIcons.LIST, onClick = { openToolsMenu("standard") }),
                        CalculatorButton.Label("0") { viewModel.inputDigit("0") },
                        CalculatorButton.Label(".", viewModel::inputDecimal),
                        CalculatorButton.Label("=", viewModel::equals),
                    ),
                )
            }
        }
    }

}

private sealed interface CalculatorButton {
    val onClick: () -> Unit

    data class Label(val text: String, override val onClick: () -> Unit) : CalculatorButton
    data class Icon(val icon: LightIconConfiguration, override val onClick: () -> Unit) : CalculatorButton
}

// measured against the stock LightOS calculator screenshot: glyph height there is
// ~1.196x LightTextVariant.Heading, and the column pitch is narrower than an even
// screen-width/4 split, hence the custom style and gutter below instead of SDK presets.
private val ButtonInset = 3.6f
private val RightGutter = 2.3f
private const val GridFontScale = 1.196f

@Composable
private fun gridTextStyle(): TextStyle {
    val base = LightThemeTokens.typography.heading
    return base.copy(
        fontSize = (base.fontSize.value * GridFontScale).designVerticalPxToSp(),
        lineHeight = (base.lineHeight.value * GridFontScale).designVerticalPxToSp(),
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
        // weighted, so the number's own box can never grow into the icon's space,
        // the icon below keeps a fixed size and position no matter what's here.
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
private fun CalculatorRow(buttons: List<CalculatorButton?>, modifier: Modifier = Modifier) {
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
                    is CalculatorButton.Label -> Text(
                        text = button.text,
                        style = gridTextStyle(),
                        color = LightThemeTokens.colors.content,
                        modifier = Modifier
                            .padding(start = ButtonInset.gridUnitsAsDp())
                            .lightClickable(onClick = button.onClick),
                    )
                    is CalculatorButton.Icon -> LightIcon(
                        icon = button.icon,
                        size = 1.7f,
                        modifier = Modifier
                            .padding(start = ButtonInset.gridUnitsAsDp())
                            .lightClickable(onClick = button.onClick),
                    )
                }
            }
        }
    }
}
