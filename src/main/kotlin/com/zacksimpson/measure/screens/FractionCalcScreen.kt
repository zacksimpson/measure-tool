package com.zacksimpson.measure.screens

import androidx.compose.foundation.background
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
import com.zacksimpson.measure.MainScreen
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// same grid, spacing, and type scale as MainScreen (copied rather than shared, see
// measure-tool's own notes on that choice), with "/" swapping in for "." and the
// arithmetic operating on fractions instead of doubles.
private const val MAX_DISPLAY_LENGTH = 10

data class Fraction(val numerator: Long, val denominator: Long) {
    fun reduced(): Fraction {
        if (numerator == 0L) return Fraction(0, 1)
        val g = gcd(abs(numerator), abs(denominator))
        val sign = if (denominator < 0) -1 else 1
        return Fraction(sign * numerator / g, sign * denominator / g)
    }
}

private fun gcd(a: Long, b: Long): Long = if (b == 0L) a else gcd(b, a % b)

enum class Operator {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE;

    fun apply(a: Fraction, b: Fraction): Fraction = when (this) {
        ADD -> Fraction(a.numerator * b.denominator + b.numerator * a.denominator, a.denominator * b.denominator)
        SUBTRACT -> Fraction(a.numerator * b.denominator - b.numerator * a.denominator, a.denominator * b.denominator)
        MULTIPLY -> Fraction(a.numerator * b.numerator, a.denominator * b.denominator)
        DIVIDE -> Fraction(a.numerator * b.denominator, a.denominator * b.numerator)
    }.reduced()
}

private fun parseFraction(text: String): Fraction {
    val negative = text.startsWith("-")
    val parts = text.removePrefix("-").split("/")
    val numerator = parts.getOrNull(0)?.toLongOrNull() ?: 0L
    val denominator = parts.getOrNull(1)?.toLongOrNull()?.takeIf { it != 0L } ?: 1L
    return Fraction(if (negative) -numerator else numerator, denominator)
}

class FractionCalcScreenViewModel : LightViewModel<Unit>() {

    private var accumulator: Fraction? = null
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

    fun inputSlash() {
        if (startingNewEntry) {
            _display.value = "0/"
            startingNewEntry = false
            return
        }
        if (_display.value.length >= MAX_DISPLAY_LENGTH) return
        if (!_display.value.contains("/")) {
            _display.value += "/"
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
        val current = parseFraction(_display.value)
        accumulator = if (pendingOperator != null && !startingNewEntry) {
            pendingOperator!!.apply(accumulator ?: Fraction(0, 1), current)
        } else {
            accumulator ?: current
        }
        pendingOperator = operator
        startingNewEntry = true
        _display.value = formatValue(accumulator ?: current)
    }

    fun equals() {
        val operator = pendingOperator ?: return
        val current = parseFraction(_display.value)
        val result = operator.apply(accumulator ?: Fraction(0, 1), current)
        _display.value = formatValue(result)
        accumulator = null
        pendingOperator = null
        startingNewEntry = true
    }

    private fun formatValue(fraction: Fraction): String {
        val reduced = fraction.reduced()
        if (reduced.denominator == 0L) return "Error"

        val sign = if (reduced.numerator < 0) "-" else ""
        val whole = abs(reduced.numerator) / reduced.denominator
        val remainder = abs(reduced.numerator) % reduced.denominator
        val result = when {
            remainder == 0L -> "$sign$whole"
            whole == 0L -> "$sign$remainder/${reduced.denominator}"
            else -> "$sign$whole-$remainder/${reduced.denominator}"
        }
        return if (result.length <= MAX_DISPLAY_LENGTH) result else "Error"
    }
}

class FractionCalcScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, FractionCalcScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<FractionCalcScreenViewModel>
        get() = FractionCalcScreenViewModel::class.java

    override fun createViewModel(): FractionCalcScreenViewModel = FractionCalcScreenViewModel()

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
                        CalculatorButton.Icon(LightIcons.LIST, onClick = ::openToolsMenu),
                        CalculatorButton.Label("0") { viewModel.inputDigit("0") },
                        CalculatorButton.Label("/", viewModel::inputSlash),
                        CalculatorButton.Label("=", viewModel::equals),
                    ),
                )
            }
        }
    }

    private val toolOptions = listOf(
        ViewOption("standard", "Standard"),
        ViewOption("convert-units", "Convert Units"),
        ViewOption("fraction-calc", "Fraction Calc"),
        ViewOption("angle-find", "Angle Find"),
    )

    private fun openToolsMenu() {
        navigateTo(
            screenFactory = { OptionsScreen(it, toolOptions) },
            resultCallback = { key ->
                when (key) {
                    "fraction-calc" -> Unit
                    "standard" -> navigateTo(screenFactory = { MainScreen(it) })
                    else -> {
                        val label = toolOptions.first { it.key == key }.label
                        navigateTo(screenFactory = { UnimplementedScreen(it, "$label: not built yet.") })
                    }
                }
            },
        )
    }
}

private sealed interface CalculatorButton {
    val onClick: () -> Unit

    data class Label(val text: String, override val onClick: () -> Unit) : CalculatorButton
    data class Icon(val icon: LightIconConfiguration, override val onClick: () -> Unit) : CalculatorButton
}

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
private fun DisplayRow(value: String, onBackspace: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(end = RightGutter.gridUnitsAsDp()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.weight(1f),
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
