package com.galaxy.airviewdictionary.ui.screen.main

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.galaxy.airviewdictionary.R
import kotlin.math.abs
import kotlin.math.roundToInt

data class RealtimeTranslationDisplaySettingsState(
    val translationTransparency: Float,
    val transparentBackground: Boolean,
    val smartBackground: Boolean,
    val textColorArgb: Int,
    val textSizeSp: Float,
    val boldText: Boolean,
    val backgroundColorArgb: Int,
    val backgroundOpacity: Float,
)

private enum class RealtimeColorTarget {
    TEXT,
    BACKGROUND,
}

private data class RealtimeSettingsPalette(
    val cardColor: Color,
    val borderColor: Color,
    val dividerColor: Color,
    val titleColor: Color,
    val contentColor: Color,
    val subContentColor: Color,
    val accentColor: Color,
    val accentSoftColor: Color,
    val inputColor: Color,
    val previewBackground: Color,
)

@Composable
private fun rememberRealtimeSettingsPalette(): RealtimeSettingsPalette {
    val isDarkMode = com.galaxy.airviewdictionary.ui.theme.LocalAppDarkTheme.current
    return RealtimeSettingsPalette(
        cardColor = if (isDarkMode) colorResource(R.color.settings_card_dark) else colorResource(R.color.settings_card_light),
        borderColor = if (isDarkMode) colorResource(R.color.settings_border_dark) else colorResource(R.color.settings_border_light),
        dividerColor = if (isDarkMode) colorResource(R.color.settings_divider_dark) else colorResource(R.color.settings_divider_light),
        titleColor = if (isDarkMode) colorResource(R.color.settings_text_primary_dark) else colorResource(R.color.settings_text_primary_light),
        contentColor = if (isDarkMode) colorResource(R.color.settings_text_primary_dark) else colorResource(R.color.settings_text_primary_light),
        subContentColor = if (isDarkMode) colorResource(R.color.settings_text_secondary_dark) else colorResource(R.color.settings_text_secondary_light),
        accentColor = colorResource(R.color.settings_accent),
        accentSoftColor = if (isDarkMode) colorResource(R.color.settings_accent_soft) else colorResource(R.color.settings_accent_soft_light),
        inputColor = if (isDarkMode) colorResource(R.color.settings_appbar_dark) else colorResource(R.color.settings_background_light),
        previewBackground = if (isDarkMode) Color(0xFF111723) else Color(0xFFF7F8FC),
    )
}

@Composable
fun RealtimeTranslationDisplaySettingsPage(
    state: RealtimeTranslationDisplaySettingsState,
    modifier: Modifier = Modifier,
    onTranslationTransparencyChange: (Float) -> Unit,
    onTransparentBackgroundChange: (Boolean) -> Unit,
    onSmartBackgroundChange: (Boolean) -> Unit,
    onTextColorChange: (Int) -> Unit,
    onTextSizeChange: (Float) -> Unit,
    onBoldTextChange: (Boolean) -> Unit,
    onBackgroundColorChange: (Int) -> Unit,
    onBackgroundOpacityChange: (Float) -> Unit,
) {
    val palette = rememberRealtimeSettingsPalette()
    val sectionSpacing = dimensionResource(R.dimen.settings_section_spacing)
    val screenPadding = dimensionResource(R.dimen.settings_screen_horizontal_padding)
    val scrollState = rememberScrollState()
    var colorTarget by rememberSaveable { mutableStateOf<RealtimeColorTarget?>(null) }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = screenPadding, vertical = sectionSpacing),
    ) {
        RealtimeSettingsSectionCard(
            title = stringResource(R.string.settings_realtime_preview_title),
            modifier = Modifier.padding(bottom = sectionSpacing)
        ) {
            RealtimeTranslationPreviewCard(
                state = state,
                palette = palette,
            )
        }

        RealtimeSettingsSectionCard(
            title = stringResource(R.string.settings_realtime_section_display),
            modifier = Modifier.padding(bottom = sectionSpacing)
        ) {
            val transparencyAmount = (1f - state.translationTransparency).coerceIn(0f, 0.5f)
            RealtimeSettingsSliderRow(
                title = stringResource(R.string.settings_menu_translation_transparency),
                supportingText = stringResource(R.string.settings_realtime_translation_transparency_supporting),
                value = transparencyAmount,
                valueRange = 0f..0.5f,
                steps = 9,
                valueLabel = "${(transparencyAmount * 100).roundToInt()}%",
                onValueChange = { onTranslationTransparencyChange(1f - snapRealtimeStep(it, 0.05f, 0f, 0.5f)) },
                palette = palette,
            )
            RealtimeSettingsSwitchRow(
                title = stringResource(R.string.settings_realtime_transparent_background),
                supportingText = stringResource(R.string.settings_realtime_transparent_background_supporting),
                checked = state.transparentBackground,
                onCheckedChange = onTransparentBackgroundChange,
                palette = palette,
            )
            RealtimeSettingsSwitchRow(
                title = stringResource(R.string.settings_realtime_smart_background),
                supportingText = stringResource(R.string.settings_realtime_smart_background_supporting),
                checked = state.smartBackground,
                onCheckedChange = onSmartBackgroundChange,
                palette = palette,
                showDivider = false,
            )
        }

        RealtimeSettingsSectionCard(
            title = stringResource(R.string.settings_realtime_section_text),
            modifier = Modifier.padding(bottom = sectionSpacing)
        ) {
            RealtimeSettingsColorRow(
                title = stringResource(R.string.settings_realtime_text_color),
                supportingText = if (state.smartBackground) {
                    stringResource(R.string.settings_realtime_disabled_by_smart_background)
                } else {
                    stringResource(R.string.settings_realtime_text_color_supporting)
                },
                color = Color(state.textColorArgb),
                valueLabel = formatHexColor(state.textColorArgb),
                enabled = !state.smartBackground,
                onClick = { colorTarget = RealtimeColorTarget.TEXT },
                palette = palette,
            )
            RealtimeSettingsSliderRow(
                title = stringResource(R.string.settings_realtime_text_size),
                supportingText = stringResource(R.string.settings_realtime_text_size_supporting),
                value = state.textSizeSp.coerceIn(10f, 28f),
                valueRange = 10f..28f,
                steps = 17,
                valueLabel = "${state.textSizeSp.roundToInt()}sp",
                onValueChange = { onTextSizeChange(snapRealtimeStep(it, 1f, 10f, 28f)) },
                palette = palette,
            )
            RealtimeSettingsSwitchRow(
                title = stringResource(R.string.settings_realtime_bold_text),
                supportingText = stringResource(R.string.settings_realtime_bold_text_supporting),
                checked = state.boldText,
                onCheckedChange = onBoldTextChange,
                palette = palette,
                showDivider = false,
            )
        }

        RealtimeSettingsSectionCard(
            title = stringResource(R.string.settings_realtime_section_background),
        ) {
            val backgroundColorEnabled = !state.transparentBackground && !state.smartBackground
            RealtimeSettingsColorRow(
                title = stringResource(R.string.settings_realtime_background_color),
                supportingText = when {
                    state.transparentBackground -> stringResource(R.string.settings_realtime_disabled_by_transparent_background)
                    state.smartBackground -> stringResource(R.string.settings_realtime_disabled_by_smart_background)
                    else -> stringResource(R.string.settings_realtime_background_color_supporting)
                },
                color = Color(state.backgroundColorArgb),
                valueLabel = formatHexColor(state.backgroundColorArgb),
                enabled = backgroundColorEnabled,
                onClick = { colorTarget = RealtimeColorTarget.BACKGROUND },
                palette = palette,
            )
            RealtimeSettingsSliderRow(
                title = stringResource(R.string.settings_realtime_background_opacity),
                supportingText = if (state.transparentBackground) {
                    stringResource(R.string.settings_realtime_disabled_by_transparent_background)
                } else {
                    stringResource(R.string.settings_realtime_background_opacity_supporting)
                },
                value = state.backgroundOpacity.coerceIn(0f, 1f),
                valueRange = 0f..1f,
                steps = 19,
                valueLabel = formatOpacityValue(state.backgroundOpacity),
                onValueChange = { onBackgroundOpacityChange(snapRealtimeStep(it, 0.05f, 0f, 1f)) },
                enabled = !state.transparentBackground,
                showDivider = false,
                palette = palette,
            )
        }
    }

    when (colorTarget) {
        RealtimeColorTarget.TEXT -> {
            RealtimeColorPickerDialog(
                title = stringResource(R.string.settings_realtime_color_picker_text),
                initialColorArgb = state.textColorArgb,
                defaultColorArgb = AndroidColor.WHITE,
                onDismiss = { colorTarget = null },
                onApply = {
                    onTextColorChange(it)
                    colorTarget = null
                },
            )
        }

        RealtimeColorTarget.BACKGROUND -> {
            RealtimeColorPickerDialog(
                title = stringResource(R.string.settings_realtime_color_picker_background),
                initialColorArgb = state.backgroundColorArgb,
                defaultColorArgb = AndroidColor.BLACK,
                onDismiss = { colorTarget = null },
                onApply = {
                    onBackgroundColorChange(it)
                    colorTarget = null
                },
            )
        }

        null -> Unit
    }
}

@Composable
private fun RealtimeSettingsSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = rememberRealtimeSettingsPalette()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.cardColor,
        shape = RoundedCornerShape(dimensionResource(R.dimen.settings_card_radius)),
        border = androidx.compose.foundation.BorderStroke(1.dp, palette.borderColor)
    ) {
        Column {
            Text(
                text = title,
                color = palette.titleColor,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
            )
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = palette.dividerColor
            )
            content()
        }
    }
}

@Composable
private fun RealtimeSettingsSliderRow(
    title: String,
    supportingText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    valueLabel: String,
    onValueChange: (Float) -> Unit,
    palette: RealtimeSettingsPalette,
    enabled: Boolean = true,
    showDivider: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = if (enabled) palette.contentColor else palette.subContentColor.copy(alpha = 0.74f),
                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = valueLabel,
                color = if (enabled) palette.accentColor else palette.subContentColor.copy(alpha = 0.8f),
                style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        Text(
            text = supportingText,
            color = palette.subContentColor.copy(alpha = if (enabled) 1f else 0.75f),
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = palette.accentColor,
                activeTrackColor = palette.accentColor,
                inactiveTrackColor = palette.dividerColor,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent,
                disabledThumbColor = palette.subContentColor.copy(alpha = 0.48f),
                disabledActiveTrackColor = palette.dividerColor,
                disabledInactiveTrackColor = palette.dividerColor.copy(alpha = 0.6f),
            ),
            modifier = Modifier.padding(top = 10.dp)
        )
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 6.dp),
                thickness = 1.dp,
                color = palette.dividerColor
            )
        }
    }
}

@Composable
private fun RealtimeSettingsSwitchRow(
    title: String,
    supportingText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    palette: RealtimeSettingsPalette,
    enabled: Boolean = true,
    showDivider: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) palette.contentColor else palette.subContentColor.copy(alpha = 0.74f),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = supportingText,
                    color = palette.subContentColor.copy(alpha = if (enabled) 1f else 0.75f),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = colorResource(R.color.settings_switch_thumb),
                    uncheckedThumbColor = colorResource(R.color.settings_switch_thumb),
                    checkedTrackColor = colorResource(R.color.settings_switch_checked_track_dark),
                    uncheckedTrackColor = colorResource(R.color.settings_switch_unchecked_track_dark),
                    checkedBorderColor = Color.Transparent,
                    uncheckedBorderColor = Color.Transparent,
                    disabledUncheckedTrackColor = palette.dividerColor,
                    disabledCheckedTrackColor = palette.dividerColor,
                ),
            )
        }
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 1.dp,
                color = palette.dividerColor
            )
        }
    }
}

@Composable
private fun RealtimeSettingsColorRow(
    title: String,
    supportingText: String,
    color: Color,
    valueLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
    palette: RealtimeSettingsPalette,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) palette.contentColor else palette.subContentColor.copy(alpha = 0.74f),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                )
                Text(
                    text = supportingText,
                    color = palette.subContentColor.copy(alpha = if (enabled) 1f else 0.75f),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(color, CircleShape)
                        .border(1.dp, palette.borderColor, CircleShape)
                )
                Text(
                    text = valueLabel,
                    color = if (enabled) palette.accentColor else palette.subContentColor.copy(alpha = 0.8f),
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 10.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (enabled) palette.subContentColor else palette.subContentColor.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(18.dp)
                )
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(top = 12.dp),
            thickness = 1.dp,
            color = palette.dividerColor
        )
    }
}

@Composable
private fun RealtimeTranslationPreviewCard(
    state: RealtimeTranslationDisplaySettingsState,
    palette: RealtimeSettingsPalette,
) {
    val smartTextColor = Color(0xFFF6F7FA)
    val smartBackgroundColor = Color(0xD94A4D57)
    val manualTextColor = Color(state.textColorArgb)
    val manualBackgroundColor = Color(state.backgroundColorArgb).copy(alpha = state.backgroundOpacity.coerceIn(0f, 1f))
    val rawBackgroundColor = when {
        state.transparentBackground -> Color.Transparent
        state.smartBackground -> smartBackgroundColor
        else -> manualBackgroundColor
    }
    val rawTextColor = if (state.smartBackground) smartTextColor else manualTextColor
    val textColor = ensurePreviewReadableTextColor(rawTextColor, rawBackgroundColor)
    val backgroundColor = rawBackgroundColor
    val overlayAlpha = state.translationTransparency.coerceIn(0.5f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(palette.previewBackground, RoundedCornerShape(22.dp))
            .border(1.dp, palette.borderColor, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 20.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_realtime_preview_source),
            color = palette.subContentColor.copy(alpha = 0.35f),
            style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp),
            modifier = Modifier.align(Alignment.TopStart)
        )
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(top = 22.dp)
                .background(backgroundColor.copy(alpha = backgroundColor.alpha * overlayAlpha), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.settings_realtime_preview_translation),
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = state.textSizeSp.coerceIn(10f, 28f).sp,
                    lineHeight = (state.textSizeSp.coerceIn(10f, 28f) * 1.08f).sp,
                    fontWeight = if (state.boldText) FontWeight.Bold else FontWeight.Medium,
                    shadow = Shadow(
                        color = if (backgroundColor == Color.Transparent) Color.Black.copy(alpha = 0.36f) else Color.Black.copy(alpha = 0.18f),
                        offset = Offset(0f, 1f),
                        blurRadius = 3f,
                    ),
                ),
                modifier = Modifier.width(240.dp),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(top = 8.dp)
                .size(34.dp)
                .background(palette.accentSoftColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ColorLens,
                contentDescription = null,
                tint = palette.accentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RealtimeColorPickerDialog(
    title: String,
    initialColorArgb: Int,
    defaultColorArgb: Int,
    onDismiss: () -> Unit,
    onApply: (Int) -> Unit,
) {
    val palette = rememberRealtimeSettingsPalette()
    var hue by remember(title, initialColorArgb) { mutableStateOf(extractHsv(initialColorArgb)[0]) }
    var saturation by remember(title, initialColorArgb) { mutableStateOf(extractHsv(initialColorArgb)[1]) }
    var brightness by remember(title, initialColorArgb) { mutableStateOf(extractHsv(initialColorArgb)[2]) }
    var hexInput by remember(title, initialColorArgb) { mutableStateOf(formatHexColor(initialColorArgb).removePrefix("#")) }
    var squareSize by remember { mutableStateOf(IntSize.Zero) }
    var hueSliderSize by remember { mutableStateOf(IntSize.Zero) }

    fun updateFromArgb(argb: Int) {
        val hsv = extractHsv(argb)
        hue = hsv[0]
        saturation = hsv[1]
        brightness = hsv[2]
        hexInput = formatHexColor(argb).removePrefix("#")
    }

    val selectedColor = remember(hue, saturation, brightness) {
        Color(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness)))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.cardColor,
        shape = RoundedCornerShape(28.dp),
        title = {
            Text(
                text = title,
                color = palette.titleColor,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_realtime_color_picker_hex),
                    color = palette.subContentColor,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { updated ->
                        val sanitized = updated
                            .uppercase()
                            .replace("#", "")
                            .take(6)
                            .filter { it in '0'..'9' || it in 'A'..'F' }
                        hexInput = sanitized
                        parseHexColor(sanitized)?.let(::updateFromArgb)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    singleLine = true,
                    prefix = { Text("#", color = palette.subContentColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accentColor,
                        unfocusedBorderColor = palette.borderColor,
                        focusedContainerColor = palette.inputColor,
                        unfocusedContainerColor = palette.inputColor,
                        focusedTextColor = palette.contentColor,
                        unfocusedTextColor = palette.contentColor,
                        cursorColor = palette.accentColor,
                    ),
                    shape = RoundedCornerShape(18.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                        .background(palette.inputColor, RoundedCornerShape(18.dp))
                        .border(1.dp, palette.borderColor, RoundedCornerShape(18.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        val hueColor = remember(hue) {
                            Color(AndroidColor.HSVToColor(floatArrayOf(hue, 1f, 1f)))
                        }
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .background(Color.Transparent, RoundedCornerShape(16.dp))
                                .onSizeChanged { squareSize = it }
                                .pointerInput(squareSize, hue) {
                                    fun updateSquareColor(offset: Offset) {
                                        if (squareSize.width == 0 || squareSize.height == 0) return
                                        saturation = (offset.x / squareSize.width.toFloat()).coerceIn(0f, 1f)
                                        brightness = (1f - (offset.y / squareSize.height.toFloat())).coerceIn(0f, 1f)
                                        hexInput = formatHexColor(
                                            AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
                                        ).removePrefix("#")
                                    }
                                    detectTapGestures { updateSquareColor(it) }
                                }
                                .pointerInput(squareSize, hue) {
                                    fun updateSquareColor(offset: Offset) {
                                        if (squareSize.width == 0 || squareSize.height == 0) return
                                        saturation = (offset.x / squareSize.width.toFloat()).coerceIn(0f, 1f)
                                        brightness = (1f - (offset.y / squareSize.height.toFloat())).coerceIn(0f, 1f)
                                        hexInput = formatHexColor(
                                            AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
                                        ).removePrefix("#")
                                    }
                                    detectDragGestures { change, _ ->
                                        updateSquareColor(change.position)
                                    }
                                }
                        ) {
                            drawRect(brush = Brush.horizontalGradient(listOf(Color.White, hueColor)))
                            drawRect(brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                            val x = saturation * size.width
                            val y = (1f - brightness) * size.height
                            drawCircle(
                                color = Color.White,
                                radius = 11.dp.toPx(),
                                center = Offset(x, y)
                            )
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.42f),
                                radius = 13.dp.toPx(),
                                center = Offset(x, y),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .height(26.dp)
                                .background(Color.Transparent, RoundedCornerShape(14.dp))
                                .onSizeChanged { hueSliderSize = it }
                                .pointerInput(hueSliderSize) {
                                    fun updateHue(offset: Offset) {
                                        if (hueSliderSize.width == 0) return
                                        hue = ((offset.x / hueSliderSize.width.toFloat()) * 360f).coerceIn(0f, 360f)
                                        hexInput = formatHexColor(
                                            AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
                                        ).removePrefix("#")
                                    }
                                    detectTapGestures { updateHue(it) }
                                }
                                .pointerInput(hueSliderSize) {
                                    fun updateHue(offset: Offset) {
                                        if (hueSliderSize.width == 0) return
                                        hue = ((offset.x / hueSliderSize.width.toFloat()) * 360f).coerceIn(0f, 360f)
                                        hexInput = formatHexColor(
                                            AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))
                                        ).removePrefix("#")
                                    }
                                    detectDragGestures { change, _ ->
                                        updateHue(change.position)
                                    }
                                }
                        ) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color.Red,
                                        Color.Yellow,
                                        Color.Green,
                                        Color.Cyan,
                                        Color.Blue,
                                        Color.Magenta,
                                        Color.Red,
                                    )
                                ),
                                size = size,
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(999f, 999f)
                            )
                            val x = (hue / 360f) * size.width
                            drawCircle(
                                color = Color.White,
                                radius = 10.dp.toPx(),
                                center = Offset(x, size.height / 2f)
                            )
                            drawCircle(
                                color = Color.Black.copy(alpha = 0.38f),
                                radius = 12.dp.toPx(),
                                center = Offset(x, size.height / 2f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(selectedColor, CircleShape)
                                    .border(1.dp, palette.borderColor, CircleShape)
                            )
                            Text(
                                text = formatHexColor(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness))),
                                color = palette.contentColor,
                                style = androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(
                    onClick = {
                        updateFromArgb(defaultColorArgb)
                    }
                ) {
                    Text(
                        text = stringResource(R.string.settings_realtime_color_picker_reset),
                        color = palette.accentColor
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(android.R.string.cancel),
                        color = palette.subContentColor
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApply(AndroidColor.HSVToColor(floatArrayOf(hue, saturation, brightness)))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.accentColor,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(text = stringResource(R.string.settings_realtime_color_picker_apply))
            }
        }
    )
}

private fun snapRealtimeStep(
    value: Float,
    step: Float,
    min: Float,
    max: Float,
): Float {
    val snapped = (((value - min) / step).roundToInt() * step) + min
    return snapped.coerceIn(min, max)
}

private fun extractHsv(argb: Int): FloatArray {
    return FloatArray(3).also { AndroidColor.colorToHSV(argb, it) }
}

private fun parseHexColor(hex: String): Int? {
    val sanitized = hex.replace("#", "").trim()
    if (sanitized.length != 6) {
        return null
    }
    return runCatching { AndroidColor.parseColor("#$sanitized") }.getOrNull()
}

private fun formatHexColor(argb: Int): String {
    return "#%06X".format(0xFFFFFF and argb)
}

private fun formatOpacityValue(opacity: Float): String {
    val rounded = ((opacity * 100f).roundToInt() / 100f)
    return if (rounded % 1f == 0f) {
        rounded.roundToInt().toString()
    } else {
        rounded.toString()
    }
}

private fun ensurePreviewReadableTextColor(
    textColor: Color,
    backgroundColor: Color,
): Color {
    if (backgroundColor == Color.Transparent) {
        return textColor
    }
    return if (abs(textColor.luminance() - backgroundColor.luminance()) >= 0.24f) {
        textColor
    } else if (backgroundColor.luminance() > 0.48f) {
        Color.Black
    } else {
        Color.White
    }
}
