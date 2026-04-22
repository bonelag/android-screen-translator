package com.galaxy.airviewdictionary.ui.screen.overlay.translation

import android.content.Context
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxy.airviewdictionary.R
import com.galaxy.airviewdictionary.core.OverlayService
import com.galaxy.airviewdictionary.data.local.screen.ScreenInfoHolder
import com.galaxy.airviewdictionary.data.local.vision.model.Line
import com.galaxy.airviewdictionary.data.local.vision.model.Paragraph
import com.galaxy.airviewdictionary.data.local.vision.model.Transaction as VisionTransaction
import com.galaxy.airviewdictionary.data.remote.translation.Language
import com.galaxy.airviewdictionary.data.remote.translation.Transaction as TranslationTransaction
import com.galaxy.airviewdictionary.extensions.toPx
import com.galaxy.airviewdictionary.ui.screen.overlay.OverlayView
import com.galaxy.airviewdictionary.ui.screen.overlay.targethandle.TargetHandleViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Singleton
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

enum class RealtimeOverlayMode {
    SELECT,
    FIXED_AREA,
}

data class RealtimeTranslationOverlayPayload(
    val mode: RealtimeOverlayMode,
    val selectedArea: Rect,
    val visionTransaction: VisionTransaction,
    val translation: TranslationTransaction,
)

private data class RealtimeDisplaySettings(
    val translationTransparency: Float,
    val transparentBackground: Boolean,
    val smartBackground: Boolean,
    val textColorArgb: Int,
    val textSizeSp: Float,
    val boldText: Boolean,
    val backgroundColorArgb: Int,
    val backgroundOpacity: Float,
)

private data class ParagraphOverlayInstruction(
    val rect: Rect,
    val text: String,
    val textColor: Color,
    val backgroundColor: Color,
    val borderColor: Color,
    val fontSizeSp: Float,
    val maxLines: Int,
    val boldText: Boolean,
)

@Singleton
class RealtimeTranslationOverlayView private constructor() : OverlayView() {

    companion object {
        val INSTANCE: RealtimeTranslationOverlayView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            RealtimeTranslationOverlayView()
        }

        val liveStateFlow = MutableStateFlow(false)

        private const val MIN_TEXT_SIZE_SP = 8f
        private const val TEXT_BOX_RADIUS_DP = 8
        private const val TEXT_BOX_PADDING_HORIZONTAL_DP = 4
        private const val TEXT_BOX_PADDING_VERTICAL_DP = 2
    }

    private val payloadFlow = MutableStateFlow<RealtimeTranslationOverlayPayload?>(null)

    private lateinit var targetHandleViewModel: TargetHandleViewModel

    override lateinit var layoutParams: WindowManager.LayoutParams

    override val composable: @Composable () -> Unit = @Composable {
        val payload by payloadFlow.collectAsStateWithLifecycle()
        val translationState by if (this::targetHandleViewModel.isInitialized) {
            targetHandleViewModel.translationFlow.collectAsStateWithLifecycle(initialValue = null)
        } else {
            remember { mutableStateOf(null) }
        }

        val translationTransparency by if (this::targetHandleViewModel.isInitialized) {
            targetHandleViewModel.preferenceRepository.translationTransparencyFlow.collectAsStateWithLifecycle(
                initialValue = 0.905f
            )
        } else {
            remember { mutableStateOf(0.905f) }
        }
        val transparentBackground by if (this::targetHandleViewModel.isInitialized) {
            targetHandleViewModel.preferenceRepository.realtimeTranslationTransparentBackgroundFlow.collectAsStateWithLifecycle(
                initialValue = false
            )
        } else {
            remember { mutableStateOf(false) }
        }
        val smartBackground by if (this::targetHandleViewModel.isInitialized) {
            targetHandleViewModel.preferenceRepository.realtimeTranslationSmartBackgroundFlow.collectAsStateWithLifecycle(
                initialValue = true
            )
        } else {
            remember { mutableStateOf(true) }
        }
        val textColorArgb by if (this::targetHandleViewModel.isInitialized) {
            targetHandleViewModel.preferenceRepository.realtimeTranslationTextColorFlow.collectAsStateWithLifecycle(
                initialValue = android.graphics.Color.WHITE
            )
        } else {
            remember { mutableStateOf(android.graphics.Color.WHITE) }
        }
        val textSizeSp by if (this::targetHandleViewModel.isInitialized) {
            targetHandleViewModel.preferenceRepository.realtimeTranslationTextSizeSpFlow.collectAsStateWithLifecycle(
                initialValue = 14f
            )
        } else {
            remember { mutableStateOf(14f) }
        }
        val boldText by if (this::targetHandleViewModel.isInitialized) {
            targetHandleViewModel.preferenceRepository.realtimeTranslationBoldTextFlow.collectAsStateWithLifecycle(
                initialValue = false
            )
        } else {
            remember { mutableStateOf(false) }
        }
        val backgroundColorArgb by if (this::targetHandleViewModel.isInitialized) {
            targetHandleViewModel.preferenceRepository.realtimeTranslationBackgroundColorFlow.collectAsStateWithLifecycle(
                initialValue = android.graphics.Color.BLACK
            )
        } else {
            remember { mutableStateOf(android.graphics.Color.BLACK) }
        }
        val backgroundOpacity by if (this::targetHandleViewModel.isInitialized) {
            targetHandleViewModel.preferenceRepository.realtimeTranslationBackgroundOpacityFlow.collectAsStateWithLifecycle(
                initialValue = 0.8f
            )
        } else {
            remember { mutableStateOf(0.8f) }
        }

        val context = LocalContext.current
        val settings = remember(
            translationTransparency,
            transparentBackground,
            smartBackground,
            textColorArgb,
            textSizeSp,
            boldText,
            backgroundColorArgb,
            backgroundOpacity,
        ) {
            RealtimeDisplaySettings(
                translationTransparency = translationTransparency,
                transparentBackground = transparentBackground,
                smartBackground = smartBackground,
                textColorArgb = textColorArgb,
                textSizeSp = textSizeSp,
                boldText = boldText,
                backgroundColorArgb = backgroundColorArgb,
                backgroundOpacity = backgroundOpacity,
            )
        }

        payload?.let { currentPayload ->
            var selectDismissMonitorArmed by remember(currentPayload.translation.sourceText) {
                mutableStateOf(false)
            }

            LaunchedEffect(currentPayload.mode, translationState) {
                if (currentPayload.mode != RealtimeOverlayMode.SELECT) {
                    return@LaunchedEffect
                }
                if (translationState != null) {
                    selectDismissMonitorArmed = true
                } else if (selectDismissMonitorArmed) {
                    RealtimeSelectionActionView.INSTANCE.clear()
                    view?.post { clear() }
                }
            }

            val instructions = remember(currentPayload, settings) {
                buildParagraphOverlayInstructions(
                    context = context,
                    payload = currentPayload,
                    settings = settings,
                )
            }

            RealtimeTranslationOverlayContent(
                payload = currentPayload,
                instructions = instructions,
                settings = settings,
                onPauseDismissRunning = {
                    if (currentPayload.mode == RealtimeOverlayMode.SELECT) {
                        targetHandleViewModel.pauseDismissRunning()
                    }
                },
                onResumeDismissRunning = {
                    if (currentPayload.mode == RealtimeOverlayMode.SELECT) {
                        targetHandleViewModel.resumeDismissRunning()
                    }
                },
            )
        }
    }

    override fun onServiceConnected(overlayService: OverlayService) {
        targetHandleViewModel = overlayService.getTargetHandleViewModel()
        super.onServiceConnected(overlayService)
    }

    suspend fun cast(
        applicationContext: Context,
        payload: RealtimeTranslationOverlayPayload,
    ) {
        payloadFlow.value = payload
        val contentBounds = computeSelectedContentBounds(payload.visionTransaction, payload.selectedArea)
            ?: Rect(payload.selectedArea)
        layoutParams = buildLayoutParams(applicationContext, contentBounds)
        if (isAttachedToWindow()) {
            updateLayout(applicationContext)
        } else {
            super.cast(applicationContext)
        }
        if (payload.mode == RealtimeOverlayMode.SELECT) {
            RealtimeSelectionActionView.INSTANCE.cast(
                applicationContext = applicationContext,
                translation = payload.translation,
                anchorRect = contentBounds,
            )
        } else {
            RealtimeSelectionActionView.INSTANCE.clear()
        }
        liveStateFlow.value = true
    }

    override fun clear() {
        payloadFlow.value = null
        liveStateFlow.value = false
        RealtimeSelectionActionView.INSTANCE.clear()
        super.clear()
    }

    private fun buildLayoutParams(
        applicationContext: Context,
        selectedArea: Rect,
    ): WindowManager.LayoutParams {
        val screenInfo = ScreenInfoHolder.get()
        val marginPx = applicationContext.resources.getDimensionPixelSize(R.dimen.translation_view_screen_min_margin)
        val left = (selectedArea.left - marginPx).coerceIn(0, screenInfo.width)
        val top = (selectedArea.top - marginPx).coerceIn(0, screenInfo.height)
        val right = (selectedArea.right + marginPx).coerceIn(left + 1, screenInfo.width)
        val bottom = (selectedArea.bottom + marginPx).coerceIn(top + 1, screenInfo.height)

        return WindowManager.LayoutParams(
            right - left,
            bottom - top,
            left,
            top,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    @Composable
    private fun RealtimeTranslationOverlayContent(
        payload: RealtimeTranslationOverlayPayload,
        instructions: List<ParagraphOverlayInstruction>,
        settings: RealtimeDisplaySettings,
        onPauseDismissRunning: () -> Unit,
        onResumeDismissRunning: () -> Unit,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .pointerInput(payload.mode, payload.translation.sourceText) {
                    if (payload.mode == RealtimeOverlayMode.SELECT) {
                        detectTapGestures(
                            onPress = {
                                onPauseDismissRunning()
                                tryAwaitRelease()
                                onResumeDismissRunning()
                            }
                        )
                    }
                }
                .zIndex(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Transparent)
                ) {
                    instructions.forEach { instruction ->
                        ParagraphOverlayBox(
                            instruction = instruction,
                            windowLeft = layoutParams.x,
                            windowTop = layoutParams.y,
                            translationTransparency = settings.translationTransparency,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ParagraphOverlayBox(
        instruction: ParagraphOverlayInstruction,
        windowLeft: Int,
        windowTop: Int,
        translationTransparency: Float,
    ) {
        val density = LocalDensity.current
        val x = instruction.rect.left - windowLeft
        val y = instruction.rect.top - windowTop
        val widthDp = with(density) { instruction.rect.width().toDp() }

        Surface(
            color = instruction.backgroundColor.copy(alpha = instruction.backgroundColor.alpha * translationTransparency),
            shape = RoundedCornerShape(TEXT_BOX_RADIUS_DP.dp),
            border = if (instruction.backgroundColor.alpha > 0f) {
                androidx.compose.foundation.BorderStroke(0.5.dp, instruction.borderColor.copy(alpha = 0.42f * translationTransparency))
            } else {
                null
            },
            modifier = Modifier
                .offset { IntOffset(x, y) }
                .width(widthDp)
                .wrapContentHeight()
                .alpha(translationTransparency)
        ) {
            Text(
                text = instruction.text,
                color = instruction.textColor,
                maxLines = instruction.maxLines,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    fontSize = instruction.fontSizeSp.sp,
                    lineHeight = (instruction.fontSizeSp * 1.08f).sp,
                    fontWeight = if (instruction.boldText) FontWeight.Bold else FontWeight.Medium,
                    shadow = Shadow(
                        color = if (instruction.backgroundColor.alpha == 0f) {
                            Color.Black.copy(alpha = 0.34f)
                        } else {
                            Color.Black.copy(alpha = 0.18f)
                        },
                        offset = Offset(0f, 1.2f),
                        blurRadius = 3.5f
                    )
                ),
                modifier = Modifier.padding(
                    horizontal = TEXT_BOX_PADDING_HORIZONTAL_DP.dp,
                    vertical = TEXT_BOX_PADDING_VERTICAL_DP.dp
                )
            )
        }
    }

    private fun buildParagraphOverlayInstructions(
        context: Context,
        payload: RealtimeTranslationOverlayPayload,
        settings: RealtimeDisplaySettings,
    ): List<ParagraphOverlayInstruction> {
        val translatedText = payload.translation.resultText?.trim().orEmpty()
        if (translatedText.isBlank()) {
            return emptyList()
        }

        val paragraphs = payload.visionTransaction.paragraphs
            .mapNotNull { paragraph ->
                val selectedLines = paragraph.lines.filter { Rect.intersects(it.boundingBox, payload.selectedArea) }
                val boundedRect = when {
                    payload.mode == RealtimeOverlayMode.SELECT && selectedLines.isNotEmpty() -> {
                        unionRects(selectedLines.map { it.boundingBox })
                    }

                    else -> intersectRect(paragraph.boundingBox, payload.selectedArea)
                }

                boundedRect?.takeIf { it.width() > 6 && it.height() > 6 }?.let {
                    Triple(paragraph, selectedLines.ifEmpty { paragraph.lines }, it)
                }
            }

        if (paragraphs.isEmpty()) {
            return emptyList()
        }

        val targetLanguageCode = payload.translation.targetLanguageCode
            ?: payload.translation.detectedLanguageCode
            ?: "en"
        val preserveWordBoundaries = !Language.isNonSpacingLanguage(targetLanguageCode)
        val paragraphTexts = splitTextByRatios(
            text = translatedText,
            weights = paragraphs.map { (_, _, rect) -> rect.width() * max(1, rect.height()) },
            preserveWordBoundaries = preserveWordBoundaries,
        )

        return paragraphs.mapIndexedNotNull { index, (paragraph, selectedLines, paragraphRect) ->
            val paragraphText = paragraphTexts.getOrElse(index) { "" }.trim()
            if (paragraphText.isBlank()) {
                return@mapIndexedNotNull null
            }

            val lineTexts = buildParagraphDisplayLines(
                sourceLines = selectedLines,
                paragraphText = paragraphText,
                preserveWordBoundaries = preserveWordBoundaries,
            )
            val dominantLine = selectedLines.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                ?: selectedLines.firstOrNull()
                ?: return@mapIndexedNotNull null

            val (resolvedTextColor, resolvedBackgroundColor) = resolveColors(
                dominantLine = dominantLine,
                settings = settings,
            )

            val innerWidthPx = max(1, paragraphRect.width() - (TEXT_BOX_PADDING_HORIZONTAL_DP.dp.toPx(context) * 2))
            val innerHeightPx = max(1, paragraphRect.height() - (TEXT_BOX_PADDING_VERTICAL_DP.dp.toPx(context) * 2))
            val displayText = lineTexts.joinToString(separator = "\n")
            val fittedText = fitTextToBounds(
                context = context,
                text = displayText,
                maxWidthPx = innerWidthPx,
                maxHeightPx = innerHeightPx,
                preferredTextSizeSp = settings.textSizeSp,
                boldText = settings.boldText,
            )

            ParagraphOverlayInstruction(
                rect = paragraphRect,
                text = displayText,
                textColor = resolvedTextColor,
                backgroundColor = resolvedBackgroundColor,
                borderColor = resolvedTextColor.copy(alpha = 0.38f),
                fontSizeSp = fittedText.first,
                maxLines = fittedText.second,
                boldText = settings.boldText,
            )
        }
    }

    private fun buildParagraphDisplayLines(
        sourceLines: List<Line>,
        paragraphText: String,
        preserveWordBoundaries: Boolean,
    ): List<String> {
        val explicitLines = paragraphText
            .lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }
        if (explicitLines.size == sourceLines.size) {
            return explicitLines
        }

        return splitTextByRatios(
            text = paragraphText,
            weights = sourceLines.map { line ->
                max(1, line.representation.trim().length)
            },
            preserveWordBoundaries = preserveWordBoundaries,
        )
    }

    private fun resolveColors(
        dominantLine: Line,
        settings: RealtimeDisplaySettings,
    ): Pair<Color, Color> {
        val rawTextColor = if (settings.smartBackground) {
            Color(dominantLine.fontColor)
        } else {
            Color(settings.textColorArgb)
        }
        val rawBackgroundColor = if (settings.smartBackground) {
            Color(dominantLine.backgroundColor)
        } else {
            Color(settings.backgroundColorArgb)
        }

        val adjustedTextColor = ensureReadableTextColor(rawTextColor, rawBackgroundColor)
        val adjustedBackgroundColor = if (settings.transparentBackground) {
            Color.Transparent
        } else {
            rawBackgroundColor.copy(alpha = settings.backgroundOpacity.coerceIn(0f, 1f))
        }
        return adjustedTextColor to adjustedBackgroundColor
    }

    private fun ensureReadableTextColor(
        textColor: Color,
        backgroundColor: Color,
    ): Color {
        if (backgroundColor == Color.Transparent) {
            return textColor
        }
        val luminanceDelta = abs(textColor.luminance() - backgroundColor.luminance())
        if (luminanceDelta >= 0.28f) {
            return textColor
        }
        return if (backgroundColor.luminance() > 0.48f) Color.Black else Color.White
    }

    private fun fitTextToBounds(
        context: Context,
        text: String,
        maxWidthPx: Int,
        maxHeightPx: Int,
        preferredTextSizeSp: Float,
        boldText: Boolean,
    ): Pair<Float, Int> {
        val displayMetrics = context.resources.displayMetrics
        var sizeSp = preferredTextSizeSp.coerceAtLeast(MIN_TEXT_SIZE_SP)

        while (sizeSp >= MIN_TEXT_SIZE_SP) {
            val layout = buildStaticLayout(
                text = text,
                maxWidthPx = maxWidthPx,
                textSizeSp = sizeSp,
                displayMetrics = displayMetrics,
                boldText = boldText,
            )
            if (layout.height <= maxHeightPx) {
                return sizeSp to max(1, layout.lineCount)
            }
            sizeSp -= 0.5f
        }

        val minimumLayout = buildStaticLayout(
            text = text,
            maxWidthPx = maxWidthPx,
            textSizeSp = MIN_TEXT_SIZE_SP,
            displayMetrics = context.resources.displayMetrics,
            boldText = boldText,
        )
        val lineHeight = max(1, minimumLayout.height / max(1, minimumLayout.lineCount))
        val maxLines = max(1, maxHeightPx / lineHeight)
        return MIN_TEXT_SIZE_SP to maxLines
    }

    private fun buildStaticLayout(
        text: String,
        maxWidthPx: Int,
        textSizeSp: Float,
        displayMetrics: android.util.DisplayMetrics,
        boldText: Boolean,
    ): StaticLayout {
        val textPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                textSizeSp,
                displayMetrics
            )
            typeface = if (boldText) android.graphics.Typeface.DEFAULT_BOLD else android.graphics.Typeface.DEFAULT
        }
        return StaticLayout.Builder
            .obtain(text, 0, text.length, textPaint, max(1, maxWidthPx))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.0f)
            .setIncludePad(false)
            .build()
    }

    private fun splitTextByRatios(
        text: String,
        weights: List<Int>,
        preserveWordBoundaries: Boolean,
    ): List<String> {
        if (weights.isEmpty()) {
            return emptyList()
        }
        val normalizedText = text.replace("\r", "").trim()
        if (normalizedText.isBlank()) {
            return List(weights.size) { "" }
        }
        if (weights.size == 1) {
            return listOf(normalizedText)
        }

        val totalWeight = weights.sum().coerceAtLeast(1)
        val ranges = mutableListOf<String>()
        var startIndex = 0
        var consumedWeight = 0

        weights.dropLast(1).forEach { weight ->
            consumedWeight += weight
            val rawTargetIndex = ((normalizedText.length * (consumedWeight.toDouble() / totalWeight.toDouble()))).roundToInt()
            val targetIndex = rawTargetIndex.coerceIn(startIndex + 1, normalizedText.length - 1)
            val splitIndex = if (preserveWordBoundaries) {
                findNearestWordBoundary(normalizedText, targetIndex, startIndex)
            } else {
                targetIndex
            }
            ranges += normalizedText.substring(startIndex, splitIndex).trim()
            startIndex = splitIndex
        }
        ranges += normalizedText.substring(startIndex, normalizedText.length).trim()

        return ranges.mapIndexed { index, chunk ->
            chunk.ifBlank {
                if (index == ranges.lastIndex) normalizedText.substring(startIndex).trim() else ""
            }
        }
    }

    private fun findNearestWordBoundary(
        text: String,
        targetIndex: Int,
        minimumIndex: Int,
    ): Int {
        val maxOffset = 14
        for (offset in 0..maxOffset) {
            val forward = targetIndex + offset
            if (forward in (minimumIndex + 1) until text.length && text[forward].isWhitespace()) {
                return forward
            }
            val backward = targetIndex - offset
            if (backward in (minimumIndex + 1) until text.length && text[backward].isWhitespace()) {
                return backward
            }
        }
        return targetIndex
    }

    private fun intersectRect(source: Rect, target: Rect): Rect? {
        val result = Rect(source)
        return if (result.intersect(target)) result else null
    }

    private fun unionRects(rects: List<Rect>): Rect? {
        return rects.reduceOrNull { acc, rect ->
            Rect(
                minOf(acc.left, rect.left),
                minOf(acc.top, rect.top),
                maxOf(acc.right, rect.right),
                maxOf(acc.bottom, rect.bottom),
            )
        }
    }
}
