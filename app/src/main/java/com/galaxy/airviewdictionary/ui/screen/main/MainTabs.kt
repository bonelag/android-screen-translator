package com.galaxy.airviewdictionary.ui.screen.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxy.airviewdictionary.R
import com.galaxy.airviewdictionary.data.remote.translation.Language
import com.galaxy.airviewdictionary.data.remote.translation.TranslationKitType
import com.galaxy.airviewdictionary.data.remote.translation.TranslationResponse
import com.galaxy.airviewdictionary.ui.theme.LocalAppDarkTheme
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private data class MainTabPalette(
    val backgroundColor: Color,
    val cardColor: Color,
    val borderColor: Color,
    val dividerColor: Color,
    val titleColor: Color,
    val contentColor: Color,
    val subContentColor: Color,
    val accentColor: Color,
    val accentSoftColor: Color,
    val successColor: Color,
    val dangerColor: Color,
    val inputColor: Color,
)

@Composable
private fun rememberMainTabPalette(): MainTabPalette {
    val isDarkMode = LocalAppDarkTheme.current
    return MainTabPalette(
        backgroundColor = if (isDarkMode) colorResource(R.color.settings_background_dark) else colorResource(R.color.settings_background_light),
        cardColor = if (isDarkMode) colorResource(R.color.settings_card_dark) else colorResource(R.color.settings_card_light),
        borderColor = if (isDarkMode) colorResource(R.color.settings_border_dark) else colorResource(R.color.settings_border_light),
        dividerColor = if (isDarkMode) colorResource(R.color.settings_divider_dark) else colorResource(R.color.settings_divider_light),
        titleColor = if (isDarkMode) colorResource(R.color.settings_text_primary_dark) else colorResource(R.color.settings_text_primary_light),
        contentColor = if (isDarkMode) colorResource(R.color.settings_text_primary_dark) else colorResource(R.color.settings_text_primary_light),
        subContentColor = if (isDarkMode) colorResource(R.color.settings_text_secondary_dark) else colorResource(R.color.settings_text_secondary_light),
        accentColor = colorResource(R.color.settings_accent),
        accentSoftColor = if (isDarkMode) colorResource(R.color.settings_accent_soft) else colorResource(R.color.settings_accent_soft_light),
        successColor = colorResource(R.color.settings_success),
        dangerColor = Color(0xFFE65454),
        inputColor = if (isDarkMode) colorResource(R.color.settings_appbar_dark) else colorResource(R.color.settings_background_light),
    )
}

@Composable
fun HomeTab(
    viewModel: SettingsViewModel,
    paddingValues: PaddingValues,
    screenTranslatorRunningFlow: StateFlow<Boolean>,
    onStartStopClick: () -> Unit
) {
    val palette = rememberMainTabPalette()
    val sectionSpacing = dimensionResource(R.dimen.settings_section_spacing)
    val screenTranslatorRunning by screenTranslatorRunningFlow.collectAsStateWithLifecycle(initialValue = false)

    val sourceLanguageCode by viewModel.preferenceRepository.sourceLanguageCodeFlow.collectAsStateWithLifecycle(initialValue = "auto")
    val targetLanguageCode by viewModel.preferenceRepository.targetLanguageCodeFlow.collectAsStateWithLifecycle(initialValue = "en")
    val kitType by viewModel.preferenceRepository.translationKitTypeFlow.collectAsStateWithLifecycle(initialValue = TranslationKitType.GOOGLE)

    var showSourceLangDialog by rememberSaveable { mutableStateOf(false) }
    var showTargetLangDialog by rememberSaveable { mutableStateOf(false) }
    var showKitDialog by rememberSaveable { mutableStateOf(false) }

    val sourceLanguage = viewModel.translationRepository.getSupportedSourceLanguage(sourceLanguageCode)
    val targetLanguage = viewModel.translationRepository.getSupportedTargetLanguage(targetLanguageCode)

    val compatibleTranslationKits = remember {
        TranslationKitType.entries.toList()
    }

    val supportedSourceLanguages = remember(kitType, targetLanguageCode) {
        viewModel.translationRepository.supportedLanguagesAsSource.filter { language ->
            viewModel.translationRepository.isSupportedAsSource(
                kitType = kitType,
                code = language.code,
                targetLanguageCode = targetLanguageCode,
            )
        }.ifEmpty { viewModel.translationRepository.supportedLanguagesAsSource }
    }

    val supportedTargetLanguages = remember(kitType, sourceLanguageCode) {
        viewModel.translationRepository.supportedLanguagesAsTarget.filter { language ->
            viewModel.translationRepository.isSupportedAsTarget(
                kitType = kitType,
                code = language.code,
                sourceLanguageCode = sourceLanguageCode,
            )
        }.ifEmpty { viewModel.translationRepository.supportedLanguagesAsTarget }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = dimensionResource(R.dimen.settings_screen_horizontal_padding), vertical = dimensionResource(R.dimen.settings_screen_top_spacing))
            .padding(bottom = paddingValues.calculateBottomPadding())
            .verticalScroll(rememberScrollState()),
    ) {
        MainTabSectionCard(
            icon = Icons.Default.PowerSettingsNew,
            title = "Trình dịch màn hình",
            modifier = Modifier.padding(bottom = sectionSpacing)
        ) {
            MainTabContentBlock {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (screenTranslatorRunning) "Popup nổi đang hoạt động" else "Chưa bật dịch màn hình",
                            color = palette.contentColor,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Text(
                            text = if (screenTranslatorRunning) {
                                "Bạn có thể tiếp tục đổi nhanh ngôn ngữ và công cụ ngay bên dưới."
                            } else {
                                "Chỉ bật khi bạn cần chia sẻ màn hình và dùng popup nổi."
                            },
                            color = palette.subContentColor,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    MainTabStatusChip(
                        text = if (screenTranslatorRunning) "Đang bật" else "Đã tắt",
                        backgroundColor = if (screenTranslatorRunning) palette.successColor.copy(alpha = 0.18f) else palette.dividerColor,
                        contentColor = if (screenTranslatorRunning) palette.successColor else palette.subContentColor
                    )
                }
                MainTabPrimaryButton(
                    text = if (screenTranslatorRunning) "Dừng dịch màn hình" else "Bắt đầu dịch màn hình",
                    containerColor = if (screenTranslatorRunning) palette.dangerColor else palette.accentColor,
                    onClick = onStartStopClick,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        MainTabSectionCard(
            icon = Icons.Default.Home,
            title = "Ngôn ngữ & công cụ"
        ) {
            MainTabContentBlock {
                MainTabSelectorCluster(
                    sourceTitle = "Nguồn",
                    sourceValue = sourceLanguage.displayName,
                    targetTitle = "Đích",
                    targetValue = targetLanguage.displayName,
                    canSwap = viewModel.isLanguageSwappable(sourceLanguageCode, targetLanguageCode, kitType),
                    onSelectSource = { showSourceLangDialog = true },
                    onSelectTarget = { showTargetLangDialog = true },
                    onSwap = {
                        if (viewModel.isLanguageSwappable(sourceLanguageCode, targetLanguageCode, kitType)) {
                            viewModel.updateSourceLanguageCode(targetLanguage.code)
                            viewModel.updateTargetLanguageCode(sourceLanguage.code)
                        }
                    }
                )
                Spacer(modifier = Modifier.size(16.dp))
                MainTabToolTile(
                    title = "Công cụ dịch",
                    value = kitType.text,
                    onClick = { showKitDialog = true }
                )
                Text(
                    text = "Trang chủ dùng chung cấu hình với menu popup. Nếu muốn dịch tay riêng, dùng tab Dịch.",
                    color = palette.subContentColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }

    if (showSourceLangDialog) {
        LanguageSelectionDialog(
            title = "Chọn ngôn ngữ nguồn",
            languages = supportedSourceLanguages,
            selectedCode = sourceLanguageCode,
            onSelect = {
                viewModel.updateLanguage(isSourceLanguage = true, language = it, oppositeLanguage = targetLanguage)
                showSourceLangDialog = false
            },
            onDismiss = { showSourceLangDialog = false }
        )
    }

    if (showTargetLangDialog) {
        LanguageSelectionDialog(
            title = "Chọn ngôn ngữ đích",
            languages = supportedTargetLanguages,
            selectedCode = targetLanguageCode,
            onSelect = {
                viewModel.updateLanguage(isSourceLanguage = false, language = it, oppositeLanguage = sourceLanguage)
                showTargetLangDialog = false
            },
            onDismiss = { showTargetLangDialog = false }
        )
    }

    if (showKitDialog) {
        KitSelectionDialog(
            currentKit = kitType,
            kits = compatibleTranslationKits,
            onSelect = {
                viewModel.updateTranslationKitType(it)
                showKitDialog = false
            },
            onDismiss = { showKitDialog = false }
        )
    }
}

@Composable
fun TranslateTab(
    viewModel: SettingsViewModel,
    paddingValues: PaddingValues
) {
    val palette = rememberMainTabPalette()
    val sectionSpacing = dimensionResource(R.dimen.settings_section_spacing)
    val contentPadding = dimensionResource(R.dimen.settings_screen_horizontal_padding)
    val sourceLanguageCode by viewModel.preferenceRepository.sourceLanguageCodeFlow.collectAsStateWithLifecycle(initialValue = "auto")
    val targetLanguageCode by viewModel.preferenceRepository.targetLanguageCodeFlow.collectAsStateWithLifecycle(initialValue = "en")
    val kitType by viewModel.preferenceRepository.translationKitTypeFlow.collectAsStateWithLifecycle(initialValue = TranslationKitType.GOOGLE)

    val sourceLanguage = viewModel.translationRepository.getSupportedSourceLanguage(sourceLanguageCode)
    val targetLanguage = viewModel.translationRepository.getSupportedTargetLanguage(targetLanguageCode)
    val coroutineScope = rememberCoroutineScope()

    var inputText by rememberSaveable { mutableStateOf("") }
    var translatedText by rememberSaveable { mutableStateOf("") }
    var isLoading by rememberSaveable { mutableStateOf(false) }

    var showSourceLangDialog by rememberSaveable { mutableStateOf(false) }
    var showTargetLangDialog by rememberSaveable { mutableStateOf(false) }
    var showKitDialog by rememberSaveable { mutableStateOf(false) }

    val compatibleTranslationKits = remember {
        TranslationKitType.entries.toList()
    }

    val supportedSourceLanguages = remember(kitType, targetLanguageCode) {
        viewModel.translationRepository.supportedLanguagesAsSource.filter { language ->
            viewModel.translationRepository.isSupportedAsSource(
                kitType = kitType,
                code = language.code,
                targetLanguageCode = targetLanguageCode,
            )
        }.ifEmpty { viewModel.translationRepository.supportedLanguagesAsSource }
    }

    val supportedTargetLanguages = remember(kitType, sourceLanguageCode) {
        viewModel.translationRepository.supportedLanguagesAsTarget.filter { language ->
            viewModel.translationRepository.isSupportedAsTarget(
                kitType = kitType,
                code = language.code,
                sourceLanguageCode = sourceLanguageCode,
            )
        }.ifEmpty { viewModel.translationRepository.supportedLanguagesAsTarget }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = contentPadding, vertical = dimensionResource(R.dimen.settings_screen_top_spacing))
            .padding(bottom = paddingValues.calculateBottomPadding())
            .verticalScroll(rememberScrollState())
    ) {
        MainTabSectionCard(
            icon = Icons.Default.Translate,
            title = "Ngôn ngữ & công cụ",
            modifier = Modifier.padding(bottom = sectionSpacing)
        ) {
            MainTabContentBlock {
                MainTabSelectorCluster(
                    sourceTitle = "Nguồn",
                    sourceValue = sourceLanguage.displayName,
                    targetTitle = "Đích",
                    targetValue = targetLanguage.displayName,
                    canSwap = viewModel.isLanguageSwappable(sourceLanguageCode, targetLanguageCode, kitType),
                    onSelectSource = { showSourceLangDialog = true },
                    onSelectTarget = { showTargetLangDialog = true },
                    onSwap = {
                        if (viewModel.isLanguageSwappable(sourceLanguageCode, targetLanguageCode, kitType)) {
                            viewModel.updateSourceLanguageCode(targetLanguage.code)
                            viewModel.updateTargetLanguageCode(sourceLanguage.code)
                        }
                    }
                )
                Spacer(modifier = Modifier.size(16.dp))
                MainTabToolTile(
                    title = "Công cụ dịch",
                    value = kitType.text,
                    onClick = { showKitDialog = true }
                )
            }
        }

        MainTabSectionCard(
            icon = Icons.Default.Translate,
            title = "Nhập văn bản",
            modifier = Modifier.padding(bottom = sectionSpacing)
        ) {
            MainTabContentBlock {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 160.dp),
                    minLines = 5,
                    maxLines = 8,
                    placeholder = {
                        Text(
                            text = "Nhập văn bản cần dịch",
                            color = palette.subContentColor
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accentColor,
                        unfocusedBorderColor = palette.borderColor,
                        focusedTextColor = palette.contentColor,
                        unfocusedTextColor = palette.contentColor,
                        focusedContainerColor = palette.inputColor,
                        unfocusedContainerColor = palette.inputColor,
                        cursorColor = palette.accentColor,
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                MainTabPrimaryButton(
                    text = "Dịch ngay",
                    containerColor = palette.accentColor,
                    enabled = inputText.isNotBlank() && !isLoading,
                    loading = isLoading,
                    onClick = {
                        if (inputText.isBlank()) {
                            return@MainTabPrimaryButton
                        }
                        coroutineScope.launch {
                            isLoading = true
                            translatedText = ""
                            val response = viewModel.translationRepository.request(
                                translationKitType = kitType,
                                sourceLanguageCode = sourceLanguage.code,
                                targetLanguageCode = targetLanguage.code,
                                sourceText = inputText
                            )
                            translatedText = when (response) {
                                is TranslationResponse.Success -> response.result.resultText.orEmpty()
                                is TranslationResponse.Error -> "Lỗi dịch thuật. Vui lòng thử lại."
                            }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }

        MainTabSectionCard(
            icon = Icons.Default.Translate,
            title = "Kết quả"
        ) {
            MainTabContentBlock {
                MainTabStatusChip(
                    text = when {
                        isLoading -> "Đang dịch"
                        translatedText.startsWith("Lỗi") -> "Có lỗi"
                        translatedText.isNotBlank() -> "Hoàn tất"
                        else -> "Chờ nhập"
                    },
                    backgroundColor = when {
                        isLoading -> palette.accentSoftColor
                        translatedText.startsWith("Lỗi") -> palette.dangerColor.copy(alpha = 0.16f)
                        translatedText.isNotBlank() -> palette.successColor.copy(alpha = 0.16f)
                        else -> palette.dividerColor
                    },
                    contentColor = when {
                        isLoading -> palette.accentColor
                        translatedText.startsWith("Lỗi") -> palette.dangerColor
                        translatedText.isNotBlank() -> palette.successColor
                        else -> palette.subContentColor
                    }
                )
                Text(
                    text = when {
                        isLoading -> "Đang dịch..."
                        translatedText.isNotBlank() -> translatedText
                        else -> "Kết quả dịch sẽ hiển thị ở đây."
                    },
                    color = if (translatedText.startsWith("Lỗi")) palette.dangerColor else palette.contentColor,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = if (translatedText.isNotBlank()) FontWeight.Medium else FontWeight.Normal
                    ),
                    modifier = Modifier.padding(top = 12.dp)
                )
                Text(
                    text = "Dùng tab này để dịch tay nhanh mà không cần bật popup nổi.",
                    color = palette.subContentColor,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
    }

    if (showSourceLangDialog) {
        LanguageSelectionDialog(
            title = "Chọn ngôn ngữ nguồn",
            languages = supportedSourceLanguages,
            selectedCode = sourceLanguageCode,
            onSelect = {
                viewModel.updateLanguage(isSourceLanguage = true, language = it, oppositeLanguage = targetLanguage)
                showSourceLangDialog = false
            },
            onDismiss = { showSourceLangDialog = false }
        )
    }

    if (showTargetLangDialog) {
        LanguageSelectionDialog(
            title = "Chọn ngôn ngữ đích",
            languages = supportedTargetLanguages,
            selectedCode = targetLanguageCode,
            onSelect = {
                viewModel.updateLanguage(isSourceLanguage = false, language = it, oppositeLanguage = sourceLanguage)
                showTargetLangDialog = false
            },
            onDismiss = { showTargetLangDialog = false }
        )
    }

    if (showKitDialog) {
        KitSelectionDialog(
            currentKit = kitType,
            kits = compatibleTranslationKits,
            onSelect = {
                viewModel.updateTranslationKitType(it)
                showKitDialog = false
            },
            onDismiss = { showKitDialog = false }
        )
    }
}

private enum class MainTabRowPosition {
    Single,
    Top,
    Middle,
    Bottom,
}

@Composable
private fun MainTabStatusChip(
    text: String,
    backgroundColor: Color,
    contentColor: Color,
) {
    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
        )
    }
}

@Composable
private fun MainTabSelectorCluster(
    sourceTitle: String,
    sourceValue: String,
    targetTitle: String,
    targetValue: String,
    canSwap: Boolean,
    onSelectSource: () -> Unit,
    onSelectTarget: () -> Unit,
    onSwap: () -> Unit,
) {
    val palette = rememberMainTabPalette()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MainTabSelectorTile(
            modifier = Modifier.weight(1f),
            title = sourceTitle,
            value = sourceValue,
            onClick = onSelectSource
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = if (canSwap) palette.accentSoftColor else palette.dividerColor,
                    shape = CircleShape
                )
                .clickable(
                    enabled = canSwap,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onSwap() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SyncAlt,
                contentDescription = "Swap languages",
                tint = if (canSwap) palette.accentColor else palette.subContentColor,
                modifier = Modifier.size(20.dp)
            )
        }
        MainTabSelectorTile(
            modifier = Modifier.weight(1f),
            title = targetTitle,
            value = targetValue,
            onClick = onSelectTarget
        )
    }
}

@Composable
private fun MainTabSelectorTile(
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = rememberMainTabPalette()
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        color = palette.inputColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, palette.borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Text(
                text = title,
                color = palette.subContentColor,
                style = MaterialTheme.typography.bodySmall
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = value,
                    color = palette.contentColor,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = palette.subContentColor,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun MainTabToolTile(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    val palette = rememberMainTabPalette()
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = palette.inputColor,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, palette.borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = palette.subContentColor,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = value,
                    color = palette.contentColor,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.subContentColor,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun MainTabSectionCard(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = rememberMainTabPalette()
    val cardRadius = dimensionResource(R.dimen.settings_card_radius)
    val headerChipSize = dimensionResource(R.dimen.settings_header_chip_size)

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = palette.cardColor,
        shape = RoundedCornerShape(cardRadius),
        border = BorderStroke(1.dp, palette.borderColor)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(headerChipSize)
                        .background(palette.accentSoftColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        modifier = Modifier.size(20.dp),
                        tint = palette.accentColor
                    )
                }
                Text(
                    text = title,
                    color = palette.titleColor,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
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
private fun MainTabValueRow(
    position: MainTabRowPosition,
    title: String,
    value: String,
    onClick: (() -> Unit)?,
    valueColor: Color? = null,
) {
    MainTabRowContainer(position = position, onClick = onClick) {
        val palette = rememberMainTabPalette()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.settings_row_min_height)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = palette.contentColor,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    color = valueColor ?: palette.accentColor,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = 4.dp)
                )
                if (onClick != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = palette.subContentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun MainTabActionRow(
    position: MainTabRowPosition,
    title: String,
    actionLabel: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    MainTabRowContainer(position = position, onClick = if (enabled) onClick else null) {
        val palette = rememberMainTabPalette()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = dimensionResource(R.dimen.settings_row_min_height)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = palette.contentColor,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .background(
                            color = if (enabled) palette.accentSoftColor else palette.dividerColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = actionLabel,
                            color = if (enabled) palette.accentColor else palette.subContentColor,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                        )
                        Icon(
                            imageVector = Icons.Default.SyncAlt,
                            contentDescription = null,
                            tint = if (enabled) palette.accentColor else palette.subContentColor,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .size(15.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(18.dp),
                    tint = palette.subContentColor
                )
            }
        }
    }
}

@Composable
private fun MainTabRowContainer(
    position: MainTabRowPosition,
    onClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val palette = rememberMainTabPalette()
    val interactionSource = remember { MutableInteractionSource() }
    Column(modifier = Modifier.fillMaxWidth()) {
        if (position == MainTabRowPosition.Middle || position == MainTabRowPosition.Bottom) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 1.dp,
                color = palette.dividerColor
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = onClick != null,
                    interactionSource = interactionSource,
                    indication = null
                ) {
                    onClick?.invoke()
                }
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            content()
        }
    }
}

@Composable
private fun MainTabContentBlock(
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        content = content
    )
}

@Composable
private fun MainTabPrimaryButton(
    text: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = Color.White,
            disabledContainerColor = containerColor.copy(alpha = 0.35f),
            disabledContentColor = Color.White.copy(alpha = 0.65f)
        ),
        shape = RoundedCornerShape(18.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = Color.White
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}

@Composable
fun LanguageSelectionDialog(
    title: String,
    languages: List<Language>,
    selectedCode: String,
    onSelect: (Language) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = rememberMainTabPalette()
    var searchQuery by remember(title, languages) { mutableStateOf("") }
    val filteredLanguages = remember(languages, searchQuery) {
        if (searchQuery.isBlank()) {
            languages
        } else {
            languages.filter { language ->
                language.displayName.contains(searchQuery, ignoreCase = true) ||
                    language.code.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(dimensionResource(R.dimen.settings_dialog_radius)),
        containerColor = palette.cardColor,
        title = {
            Text(
                text = title,
                color = palette.titleColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    singleLine = true,
                    label = { Text(text = "Tìm kiếm") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.accentColor,
                        unfocusedBorderColor = palette.dividerColor,
                        focusedLabelColor = palette.accentColor,
                        unfocusedLabelColor = palette.subContentColor,
                        focusedTextColor = palette.titleColor,
                        unfocusedTextColor = palette.titleColor,
                        cursorColor = palette.accentColor,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )

                LazyColumn {
                    items(filteredLanguages, key = { language -> language.code }) { language ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = if (language.code.equals(selectedCode, ignoreCase = true)) palette.accentSoftColor else Color.Transparent,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { onSelect(language) }
                                .padding(horizontal = 6.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = language.code.equals(selectedCode, ignoreCase = true),
                                onClick = { onSelect(language) }
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 8.dp)
                            ) {
                                Text(
                                    text = language.displayName,
                                    color = palette.titleColor,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                )
                                Text(
                                    text = language.code.uppercase(),
                                    color = palette.subContentColor,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            thickness = 1.dp,
                            color = palette.dividerColor.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Đóng", color = palette.accentColor)
            }
        }
    )
}

@Composable
fun KitSelectionDialog(
    currentKit: TranslationKitType,
    kits: List<TranslationKitType> = TranslationKitType.entries,
    onSelect: (TranslationKitType) -> Unit,
    onDismiss: () -> Unit
) {
    val palette = rememberMainTabPalette()

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(dimensionResource(R.dimen.settings_dialog_radius)),
        containerColor = palette.cardColor,
        title = {
            Text(
                text = "Chọn công cụ dịch",
                color = palette.titleColor,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 360.dp)
            ) {
                items(kits, key = { kit -> kit.name }) { kit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = if (kit == currentKit) palette.accentSoftColor else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelect(kit) }
                            .padding(horizontal = 6.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = kit == currentKit,
                            onClick = { onSelect(kit) }
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        ) {
                            Text(
                                text = kit.text,
                                color = palette.titleColor,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                            )
                            Text(
                                text = kit.name,
                                color = palette.subContentColor,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 12.dp),
                        thickness = 1.dp,
                        color = palette.dividerColor.copy(alpha = 0.55f)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Đóng", color = palette.accentColor)
            }
        }
    )
}
