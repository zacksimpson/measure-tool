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

// entry is always in inches, with an optional leading feet part: a plain integer
// ("12"), a simple fraction ("3/4"), a mixed number ("12,3/4", "," marking the
// whole/numerator boundary, see inputMixedSeparator), any of those prefixed with
// feet ("3'"), or "'" alone. all arithmetic happens in total inches.
private fun parseInchesFraction(text: String): Fraction {
    val commaIndex = text.indexOf(",")
    val whole = if (commaIndex >= 0) text.substring(0, commaIndex).toLongOrNull() ?: 0L else 0L
    val fractionPart = if (commaIndex >= 0) text.substring(commaIndex + 1) else text
    val parts = fractionPart.split("/")
    val numerator = parts.getOrNull(0)?.toLongOrNull() ?: 0L
    val denominator = parts.getOrNull(1)?.toLongOrNull()?.takeIf { it != 0L } ?: 1L
    return Fraction(whole * denominator + numerator, denominator)
}

private fun parseFraction(text: String): Fraction {
    val negative = text.startsWith("-")
    val body = text.removePrefix("-")
    val feetIndex = body.indexOf("'")
    val feet = if (feetIndex >= 0) body.substring(0, feetIndex).toLongOrNull() ?: 0L else 0L
    val inches = parseInchesFraction(if (feetIndex >= 0) body.substring(feetIndex + 1) else body)
    val totalNumerator = feet * 12 * inches.denominator + inches.numerator
    return Fraction(if (negative) -totalNumerator else totalNumerator, inches.denominator)
}

// an empty (or "0") whole part is never meaningful (0 wholes + a fraction is just the
// fraction), so drop it from what's shown while typing: "0,3/4" and "3',3/4" both
// read as clean "3/4" / "3'3/4" instead of a stray leading/trailing "-".
private fun renderEntryDisplay(raw: String): String {
    val sign = if (raw.startsWith("-")) "-" else ""
    val body = raw.removePrefix(sign)
    val commaIndex = body.indexOf(",")
    val withoutEmptyWhole = if (commaIndex < 0) {
        body
    } else {
        val feetIndex = body.indexOf("'")
        val wholeStart = if (feetIndex in 0 until commaIndex) feetIndex + 1 else 0
        val whole = body.substring(wholeStart, commaIndex)
        if (whole.isEmpty() || whole == "0") body.removeRange(wholeStart, commaIndex + 1) else body
    }
    val rendered = sign + withoutEmptyWhole.replace(",", "-")
    return if (rendered.isEmpty() || rendered == "-") "0" else rendered
}

class FractionCalcScreenViewModel(private val historyRepo: CalcHistoryRepository) : LightViewModel<Unit>() {

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

    // "fra": marks the boundary between the whole number and the numerator,
    // e.g. "12" + fra + "3" + "/" + "4" builds "12,3/4" (twelve and three quarters).
    fun inputMixedSeparator() {
        if (startingNewEntry) {
            _display.value = "0,"
            startingNewEntry = false
            return
        }
        if (_display.value.length >= MAX_DISPLAY_LENGTH) return
        if (!_display.value.contains(",") && !_display.value.contains("/")) {
            _display.value += ","
        }
    }

    // "ft": marks the boundary between feet and inches, must come before any
    // fraction markers, e.g. "3" + ft + "4" + fra + "1" + "/" + "2" builds "3'4,1/2".
    fun inputFeetMarker() {
        if (startingNewEntry) {
            _display.value = "0'"
            startingNewEntry = false
            return
        }
        if (_display.value.length >= MAX_DISPLAY_LENGTH) return
        if (!_display.value.contains("'") && !_display.value.contains(",") && !_display.value.contains("/")) {
            _display.value += "'"
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
        if (_display.value != "Error") {
            viewModelScope.launch { historyRepo.record(_display.value) }
        }
    }

    // fraction is total inches. folds into feet once >= 12 and always ends in a
    // unit mark ("'" for a bare feet result, otherwise the inches mark """)
    // so it's never ambiguous which unit a result is in.
    private fun formatValue(fraction: Fraction): String {
        val reduced = fraction.reduced()
        if (reduced.denominator == 0L) return "Error"

        val sign = if (reduced.numerator < 0) "-" else ""
        val totalWhole = abs(reduced.numerator) / reduced.denominator
        val remainder = abs(reduced.numerator) % reduced.denominator
        val feet = totalWhole / 12
        val inchesWhole = if (feet > 0) totalWhole % 12 else totalWhole
        val feetPrefix = if (feet > 0) "$feet'" else ""

        val result = when {
            remainder == 0L && inchesWhole == 0L && feet > 0 -> "$sign$feetPrefix"
            remainder == 0L -> "$sign$feetPrefix$inchesWhole\""
            inchesWhole == 0L -> "$sign$feetPrefix$remainder/${reduced.denominator}\""
            else -> "$sign$feetPrefix$inchesWhole-$remainder/${reduced.denominator}\""
        }
        return if (result.length <= MAX_DISPLAY_LENGTH) result else "Error"
    }
}

class FractionCalcScreen(sealedActivity: SealedLightActivity) :
    LightScreen<Unit, FractionCalcScreenViewModel>(sealedActivity) {

    override val viewModelClass: Class<FractionCalcScreenViewModel>
        get() = FractionCalcScreenViewModel::class.java

    override fun createViewModel(): FractionCalcScreenViewModel =
        FractionCalcScreenViewModel(CalcHistoryRepository(lightContext.dataStore))

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
                    value = renderEntryDisplay(display),
                    onBackspace = viewModel::backspace,
                    onLongPress = {
                        navigateTo(screenFactory = { ResultActionsScreen(it, renderEntryDisplay(display)) })
                    },
                    modifier = Modifier.weight(1f),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        FractionCalcButton.Label("C", onClick = viewModel::clear),
                        FractionCalcButton.Label("ft", scale = 0.7f, onClick = viewModel::inputFeetMarker),
                        FractionCalcButton.Label("±", onClick = viewModel::toggleSign),
                        FractionCalcButton.Label("÷") { viewModel.setOperator(Operator.DIVIDE) },
                    ),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        FractionCalcButton.Label("7") { viewModel.inputDigit("7") },
                        FractionCalcButton.Label("8") { viewModel.inputDigit("8") },
                        FractionCalcButton.Label("9") { viewModel.inputDigit("9") },
                        FractionCalcButton.Label("×") { viewModel.setOperator(Operator.MULTIPLY) },
                    ),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        FractionCalcButton.Label("4") { viewModel.inputDigit("4") },
                        FractionCalcButton.Label("5") { viewModel.inputDigit("5") },
                        FractionCalcButton.Label("6") { viewModel.inputDigit("6") },
                        FractionCalcButton.Label("-") { viewModel.setOperator(Operator.SUBTRACT) },
                    ),
                )
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        FractionCalcButton.Label("1") { viewModel.inputDigit("1") },
                        FractionCalcButton.Label("2") { viewModel.inputDigit("2") },
                        FractionCalcButton.Label("3") { viewModel.inputDigit("3") },
                        FractionCalcButton.Label("+") { viewModel.setOperator(Operator.ADD) },
                    ),
                )
                // "fra"/"/" share one key: it reads "fra" until the whole/numerator
                // marker has been placed, then relabels to "/" for the numerator/
                // denominator split, then reverts once a new entry starts.
                val fraSlashShowsSlash = display.contains(",")
                CalculatorRow(
                    modifier = Modifier.weight(1f),
                    buttons = listOf(
                        FractionCalcButton.Icon(LightIcons.LIST, onClick = { openToolsMenu("fraction-calc") }),
                        FractionCalcButton.Label("0") { viewModel.inputDigit("0") },
                        FractionCalcButton.Label(
                            text = if (fraSlashShowsSlash) "/" else "fra",
                            scale = if (fraSlashShowsSlash) 0.85f else 0.7f,
                            onClick = if (fraSlashShowsSlash) viewModel::inputSlash else viewModel::inputMixedSeparator,
                        ),
                        FractionCalcButton.Label("=", onClick = viewModel::equals),
                    ),
                )
            }
        }
    }

}

private sealed interface FractionCalcButton {
    val onClick: () -> Unit

    data class Label(val text: String, val scale: Float = 1f, override val onClick: () -> Unit) : FractionCalcButton
    data class Icon(val icon: LightIconConfiguration, override val onClick: () -> Unit) : FractionCalcButton
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
private fun CalculatorRow(buttons: List<FractionCalcButton?>, modifier: Modifier = Modifier) {
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
                    is FractionCalcButton.Label -> Text(
                        text = button.text,
                        style = gridTextStyle(button.scale),
                        color = LightThemeTokens.colors.content,
                        modifier = Modifier
                            .padding(start = ButtonInset.gridUnitsAsDp())
                            .lightClickable(onClick = button.onClick),
                    )
                    is FractionCalcButton.Icon -> LightIcon(
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
