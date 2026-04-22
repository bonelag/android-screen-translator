package com.galaxy.airviewdictionary.ui.screen.overlay.translation

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Reply
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.galaxy.airviewdictionary.R
import com.galaxy.airviewdictionary.core.OverlayService
import com.galaxy.airviewdictionary.data.local.screen.ScreenInfoHolder
import com.galaxy.airviewdictionary.data.remote.translation.Transaction as TranslationTransaction
import com.galaxy.airviewdictionary.extensions.toPx
import com.galaxy.airviewdictionary.ui.screen.overlay.OverlayView
import com.galaxy.airviewdictionary.ui.screen.overlay.targethandle.TargetHandleViewModel
import com.galaxy.airviewdictionary.ui.screen.reply.ReplyActivity
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Singleton
import kotlin.math.roundToInt
import androidx.compose.foundation.gestures.detectDragGestures

@Singleton
class RealtimeSelectionActionView private constructor() : OverlayView() {

    companion object {
        val INSTANCE: RealtimeSelectionActionView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            RealtimeSelectionActionView()
        }

        private const val ACTION_STRIP_WIDTH_DP = 184
        private const val ACTION_STRIP_HEIGHT_DP = 64
        private const val ACTION_STRIP_MARGIN_DP = 12
    }

    private lateinit var targetHandleViewModel: TargetHandleViewModel

    private val translationFlow = MutableStateFlow<TranslationTransaction?>(null)
    private val anchorRectFlow = MutableStateFlow<Rect?>(null)

    override var layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        0,
        0,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    override val composable: @Composable () -> Unit = @Composable {
        val translation by translationFlow.collectAsStateWithLifecycle()
        val context = LocalContext.current
        val isDarkMode by if (this@RealtimeSelectionActionView::targetHandleViewModel.isInitialized) {
            targetHandleViewModel.preferenceRepository.uiDarkThemeFlow.collectAsState(initial = true)
        } else {
            androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(true) }
        }

        translation?.let { currentTranslation ->

            val containerColor = if (isDarkMode) {
                colorResource(R.color.settings_card_dark).copy(alpha = 0.96f)
            } else {
                colorResource(R.color.settings_card_light).copy(alpha = 0.98f)
            }
            val borderColor = if (isDarkMode) {
                colorResource(R.color.settings_border_dark)
            } else {
                colorResource(R.color.settings_border_light)
            }
            val iconTint = if (isDarkMode) {
                colorResource(R.color.settings_text_primary_dark)
            } else {
                colorResource(R.color.settings_text_primary_light)
            }

            Surface(
                modifier = Modifier.pointerInput(currentTranslation.sourceText) {
                    detectDragGestures(
                        onDragStart = {
                            targetHandleViewModel.pauseDismissRunning()
                        },
                        onDragEnd = {
                            targetHandleViewModel.resumeDismissRunning()
                        },
                        onDragCancel = {
                            targetHandleViewModel.resumeDismissRunning()
                        },
                    ) { _, dragAmount ->
                        val screenInfo = ScreenInfoHolder.get()
                        val fallbackWidth = ACTION_STRIP_WIDTH_DP.dp.toPx(context)
                        val fallbackHeight = ACTION_STRIP_HEIGHT_DP.dp.toPx(context)
                        val currentWidth = view?.width ?: fallbackWidth
                        val currentHeight = view?.height ?: fallbackHeight
                        val nextX = (layoutParams.x + dragAmount.x.roundToInt())
                            .coerceIn(0, (screenInfo.width - currentWidth).coerceAtLeast(0))
                        val nextY = (layoutParams.y + dragAmount.y.roundToInt())
                            .coerceIn(0, (screenInfo.height - currentHeight).coerceAtLeast(0))
                        updateLayout(context, nextX, nextY)
                    }
                },
                color = containerColor,
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor.copy(alpha = 0.82f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ActionIcon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy translation",
                        tint = iconTint,
                    ) {
                        targetHandleViewModel.rerunDismissRunning()
                        copyTranslationToClipboard(context, currentTranslation)
                    }
                    ActionIcon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Read translation aloud",
                        tint = iconTint,
                    ) {
                        targetHandleViewModel.rerunDismissRunning()
                        targetHandleViewModel.playTTS(
                            currentTranslation.resultText.orEmpty(),
                            currentTranslation.targetLanguageCode ?: "en",
                        )
                    }
                    ActionIcon(
                        imageVector = Icons.Default.Reply,
                        contentDescription = "Reply",
                        tint = iconTint,
                    ) {
                        targetHandleViewModel.rerunDismissRunning()
                        ReplyActivity.start(
                            context = context,
                            translationResultText = currentTranslation.resultText.orEmpty(),
                            detectedLanguageCode = currentTranslation.detectedLanguageCode,
                            targetLanguageCode = currentTranslation.targetLanguageCode,
                        )
                        RealtimeTranslationOverlayView.INSTANCE.clear()
                        clear()
                    }
                }
            }
        }
    }

    override fun onServiceConnected(overlayService: OverlayService) {
        targetHandleViewModel = overlayService.getTargetHandleViewModel()
        super.onServiceConnected(overlayService)
    }

    suspend fun cast(
        applicationContext: Context,
        translation: TranslationTransaction,
        anchorRect: Rect,
    ) {
        translationFlow.value = translation
        anchorRectFlow.value = Rect(anchorRect)
        layoutParams = buildLayoutParams(applicationContext, anchorRect)
        if (isAttachedToWindow()) {
            updateLayout(applicationContext)
        } else {
            super.cast(applicationContext)
        }
    }

    override fun clear() {
        translationFlow.value = null
        anchorRectFlow.value = null
        super.clear()
    }

    private fun buildLayoutParams(
        context: Context,
        anchorRect: Rect,
    ): WindowManager.LayoutParams {
        val screenInfo = ScreenInfoHolder.get()
        val actionWidth = ACTION_STRIP_WIDTH_DP.dp.toPx(context)
        val actionHeight = ACTION_STRIP_HEIGHT_DP.dp.toPx(context)
        val margin = ACTION_STRIP_MARGIN_DP.dp.toPx(context)

        val candidateRects = listOf(
            Rect(anchorRect.right + margin, anchorRect.top, anchorRect.right + margin + actionWidth, anchorRect.top + actionHeight),
            Rect(anchorRect.left, anchorRect.bottom + margin, anchorRect.left + actionWidth, anchorRect.bottom + margin + actionHeight),
            Rect(anchorRect.left, anchorRect.top - margin - actionHeight, anchorRect.left + actionWidth, anchorRect.top - margin),
            Rect(anchorRect.left - margin - actionWidth, anchorRect.top, anchorRect.left - margin, anchorRect.top + actionHeight),
        )

        val fittingRect = candidateRects.firstOrNull { rect ->
            rect.left >= 0 &&
                rect.top >= 0 &&
                rect.right <= screenInfo.width &&
                rect.bottom <= screenInfo.height &&
                !Rect.intersects(rect, anchorRect)
        } ?: Rect(
            (anchorRect.right + margin).coerceAtMost(screenInfo.width - actionWidth),
            (anchorRect.bottom + margin).coerceAtMost(screenInfo.height - actionHeight),
            0,
            0,
        ).apply {
            right = left + actionWidth
            bottom = top + actionHeight
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            fittingRect.left.coerceAtLeast(0),
            fittingRect.top.coerceAtLeast(0),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    @Composable
    private fun ActionIcon(
        imageVector: androidx.compose.ui.graphics.vector.ImageVector,
        contentDescription: String,
        tint: Color,
        onClick: () -> Unit,
    ) {
        IconButton(onClick = onClick) {
            Icon(
                imageVector = imageVector,
                contentDescription = contentDescription,
                tint = tint,
            )
        }
    }

    private fun copyTranslationToClipboard(
        context: Context,
        translation: TranslationTransaction,
    ) {
        runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val sourceText = translation.correctedText ?: translation.sourceText.orEmpty()
            val resultText = translation.resultText.orEmpty()
            clipboard.setPrimaryClip(ClipData.newPlainText("Translated Text", "$sourceText  $resultText".trim()))
        }.onFailure {
            Timber.tag(TAG).e(it, "Failed to copy translation")
        }
    }
}
