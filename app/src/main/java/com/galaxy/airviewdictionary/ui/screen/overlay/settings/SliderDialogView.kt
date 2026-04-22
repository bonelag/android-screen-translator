package com.galaxy.airviewdictionary.ui.screen.overlay.settings


import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.galaxy.airviewdictionary.R
import com.galaxy.airviewdictionary.core.OverlayService
import com.galaxy.airviewdictionary.ui.screen.overlay.OverlayView
import kotlinx.coroutines.flow.MutableStateFlow
import timber.log.Timber
import javax.inject.Singleton


/**
 * Settings Slider 뷰
 */
@Singleton
class SliderDialogView private constructor() : OverlayView() {

    companion object {
        val INSTANCE: SliderDialogView by lazy(LazyThreadSafetyMode.SYNCHRONIZED) { SliderDialogView() }

        val liveStateFlow = MutableStateFlow(false)
    }

    private lateinit var viewModel: SliderDialogViewModel

    private val initialValue = mutableStateOf<Float?>(null)
    private val valueRange = mutableStateOf<ClosedFloatingPointRange<Float>?>(null)
    private val steps = mutableIntStateOf(0)
    private val onValueChange = mutableStateOf<((Float) -> Unit)?>(null)
    private val onDismissRequest = mutableStateOf<(() -> Unit)?>(null)
    private val speechRateText = mutableStateOf<String?>(null)
    private val onSamplePlayback = mutableStateOf<(() -> Unit)?>(null)
    private val playDefaultSample = mutableStateOf(false)

    override var layoutParams: WindowManager.LayoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        },
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.CENTER
    }

    override val composable: @Composable () -> Unit = @Composable {
        val isDarkMode by if (this::viewModel.isInitialized) {
            viewModel.preferenceRepository.uiDarkThemeFlow.collectAsState(initial = true)
        } else {
            remember { mutableStateOf(true) }
        }
        if (isAttachedToWindow() && initialValue.value != null && valueRange.value != null && onValueChange.value != null && onDismissRequest.value != null) {
            SliderDialog(
                isDarkMode = isDarkMode,
                initialValue = initialValue.value!!,
                valueRange = valueRange.value!!,
                steps = steps.intValue,
                onValueChange = onValueChange.value!!,
                onDismissRequest = onDismissRequest.value!!,
            )
        }
    }

    override fun onServiceConnected(overlayService: OverlayService) {
        viewModel = overlayService.getSliderDialogViewModel()
        super.onServiceConnected(overlayService)
    }

    suspend fun cast(
        applicationContext: Context,
        initialValue: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int = 0,
        onValueChange: (Float) -> Unit,
        menuText: Pair<String, Point>? = null,
        menuSubtext: Pair<MutableStateFlow<String>, Point>? = null,
        dockingDelayText: Pair<MutableStateFlow<String>, Point>? = null,
        menuBarVisibilityText: Pair<MutableStateFlow<String>, Point>? = null,
        menuBarConfigText: Pair<MutableStateFlow<String>, Point>? = null,
        speechRateText: Pair<MutableStateFlow<Float>, Point>? = null,
        onSamplePlayback: (() -> Unit)? = null,
        playDefaultSample: Boolean = false,
        onDismissRequest: () -> Unit,
    ) {
        menuText?.let {
            SettingsMenuTextView.INSTANCE.cast(applicationContext, it.first, it.second)
        }
        menuSubtext?.let {
            SettingsMenuSubtextView.INSTANCE.cast(applicationContext, it.first, it.second)
        }
        dockingDelayText?.let {
            SettingsDockingDelayView.INSTANCE.cast(applicationContext, it.first, it.second)
        }
        menuBarVisibilityText?.let {
            SettingsMenuBarTransparencyView.INSTANCE.cast(applicationContext, it.first, it.second)
        }
        menuBarConfigText?.let {
            SettingsMenuBarConfigView.INSTANCE.cast(applicationContext, it.first, it.second)
        }
        speechRateText?.let {
            SettingsTTSSpeechRateView.INSTANCE.cast(applicationContext, it.first, it.second)
        }

        this.initialValue.value = initialValue
        this.valueRange.value = valueRange
        this.steps.intValue = steps
        this.onValueChange.value = onValueChange
        this.onDismissRequest.value = onDismissRequest
        this.speechRateText.value = if (speechRateText != null) menuText?.first else null
        this.onSamplePlayback.value = onSamplePlayback
        this.playDefaultSample.value = playDefaultSample
        super.cast(applicationContext)

        onSamplePlayback?.invoke() ?: run {
            if (this.playDefaultSample.value) {
                viewModel.playSampleVoice()
            }
        }

        liveStateFlow.value = true
    }

    override fun clear() {
        liveStateFlow.value = false
        SettingsMenuTextView.INSTANCE.clear()
        SettingsMenuSubtextView.INSTANCE.clear()
        SettingsDockingDelayView.INSTANCE.clear()
        SettingsMenuBarTransparencyView.INSTANCE.clear()
        SettingsMenuBarConfigView.INSTANCE.clear()
        SettingsTTSSpeechRateView.INSTANCE.clear()
        onSamplePlayback.value = null
        playDefaultSample.value = false
        super.clear()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun SliderDialog(
        isDarkMode: Boolean,
        initialValue: Float,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int,
        onValueChange: (Float) -> Unit,
        onDismissRequest: () -> Unit
    ) {
        val configuration = LocalConfiguration.current
        val horizontalPadding = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) 18.dp else 160.dp
        val cardColor = if (isDarkMode) colorResource(R.color.settings_card_dark) else colorResource(R.color.settings_card_light)
        val borderColor = if (isDarkMode) colorResource(R.color.settings_border_dark) else colorResource(R.color.settings_border_light)
        val activeTrackColor = if (isDarkMode) {
            colorResource(R.color.settings_slider_active_track_dark)
        } else {
            colorResource(R.color.settings_slider_active_track_light)
        }
        val inactiveTrackColor = if (isDarkMode) {
            colorResource(R.color.settings_slider_inactive_track_dark)
        } else {
            colorResource(R.color.settings_slider_inactive_track_light)
        }
        val thumbColor = if (isDarkMode) {
            colorResource(R.color.settings_slider_thumb_dark)
        } else {
            colorResource(R.color.settings_slider_thumb_light)
        }
        val thumbBorderColor = if (isDarkMode) {
            colorResource(R.color.settings_slider_thumb_border_dark)
        } else {
            colorResource(R.color.settings_slider_thumb_border_light)
        }
        val hiddenTickColor = colorResource(R.color.settings_slider_tick_hidden)
        val scrimColor = colorResource(R.color.settings_dialog_scrim)
        val dialogRadius = dimensionResource(R.dimen.settings_dialog_radius)
        val dialogHeight = dimensionResource(R.dimen.settings_slider_sheet_height)
        val sliderPosition = remember { mutableFloatStateOf(initialValue) }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrimColor)
        ) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .padding(horizontal = horizontalPadding)
                    .height(dialogHeight)
                    .fillMaxWidth(),
                color = cardColor,
                shape = RoundedCornerShape(dialogRadius),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
            ) {
                val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Slider(
                        thumb = {
                            Box(
                                modifier = Modifier
                                    .size(if (isDarkMode) 24.dp else 22.dp)
                                    .shadow(
                                        elevation = if (isDarkMode) 8.dp else 6.dp,
                                        shape = CircleShape,
                                        ambientColor = Color.Black.copy(alpha = if (isDarkMode) 0.34f else 0.12f),
                                        spotColor = Color.Black.copy(alpha = if (isDarkMode) 0.34f else 0.12f)
                                    )
                                    .background(
                                        color = thumbColor,
                                        shape = CircleShape
                                    )
                                    .border(
                                        width = if (isDarkMode) 1.5.dp else 2.dp,
                                        color = thumbBorderColor,
                                        shape = CircleShape
                                    )
                            )
                        },
                        value = sliderPosition.floatValue,
                        steps = steps,
                        onValueChange = {
                            sliderPosition.floatValue = it
                            Timber.tag(TAG).d("onValueChange $it")
                            onValueChange(it)
                        },
                        colors = SliderDefaults.colors(
                            activeTrackColor = activeTrackColor,
                            inactiveTrackColor = inactiveTrackColor,
                            activeTickColor = hiddenTickColor,
                            inactiveTickColor = hiddenTickColor,
                            thumbColor = thumbColor,
                        ),
                        valueRange = valueRange,
                        onValueChangeFinished = {
                            onSamplePlayback.value?.invoke() ?: run {
                                if (playDefaultSample.value) {
                                    viewModel.playSampleVoice()
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .matchParentSize()
                    .padding(bottom = 24.dp + dialogHeight)
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onDismissRequest()
                        }
                    }
            )
        }
    }
}

















