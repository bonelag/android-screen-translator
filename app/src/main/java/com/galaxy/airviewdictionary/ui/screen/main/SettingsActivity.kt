package com.galaxy.airviewdictionary.ui.screen.main


import android.annotation.SuppressLint
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Point
import android.os.Build
import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.view.WindowInsets
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FiberNew
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VoiceChat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.galaxy.airviewdictionary.BuildConfig
import com.galaxy.airviewdictionary.R
import com.galaxy.airviewdictionary.data.local.capture.CaptureRepository
import com.galaxy.airviewdictionary.data.local.screen.ScreenInfoHolder
import com.galaxy.airviewdictionary.data.local.tts.TtsEngineInfo
import com.galaxy.airviewdictionary.data.local.tts.TtsLanguageInfo
import com.galaxy.airviewdictionary.data.local.vision.TextDetectMode
import com.galaxy.airviewdictionary.data.remote.ai.CorrectionKitType
import com.galaxy.airviewdictionary.data.remote.translation.TranslationKitType
import com.galaxy.airviewdictionary.extensions.finishService
import com.galaxy.airviewdictionary.extensions.gotoStore
import com.galaxy.airviewdictionary.extensions.toPx
import com.galaxy.airviewdictionary.extensions.vibrate
import com.galaxy.airviewdictionary.ui.common.fontDimensionResource
import com.galaxy.airviewdictionary.ui.screen.AVDActivity
import com.galaxy.airviewdictionary.ui.screen.overlay.dialog.DialogView
import com.galaxy.airviewdictionary.ui.screen.overlay.languagelist.LanguageListView
import com.galaxy.airviewdictionary.ui.screen.overlay.menubar.MenuBar
import com.galaxy.airviewdictionary.ui.screen.overlay.menubar.MenuBarView
import com.galaxy.airviewdictionary.ui.screen.overlay.menubar.MenuConfig
import com.galaxy.airviewdictionary.ui.screen.overlay.settings.HelpTextDetectModeView
import com.galaxy.airviewdictionary.ui.screen.overlay.settings.HelpTranslationKitView
import com.galaxy.airviewdictionary.ui.screen.overlay.settings.SliderDialogView
import com.galaxy.airviewdictionary.ui.screen.overlay.targethandle.TargetHandleView
import com.galaxy.airviewdictionary.ui.screen.overlay.voicelist.VoiceListView
import com.galaxy.airviewdictionary.ui.screen.permissions.ScreenCapturePermissionRequesterActivity
import com.galaxy.airviewdictionary.ui.theme.LocalAppDarkTheme
import com.galaxy.airviewdictionary.ui.theme.ScreenTranslatorTheme
import com.google.firebase.Firebase
import com.google.firebase.analytics.analytics
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round
import kotlin.math.roundToInt


@AndroidEntryPoint
class SettingsActivity : AVDActivity() {

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, SettingsActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        val liveStateFlow = MutableStateFlow(false)

        val screenTranslatorRunningFlow = MutableStateFlow(false)

        val menuBarViewSettlePositionFlow = MutableStateFlow<Point?>(null)
    }

    private val viewModel: SettingsViewModel by viewModels()
    private var currentUiDarkTheme: Boolean = true

    private val settingFloatFlow = MutableStateFlow(1.0f)

    private val settingStringFlow = MutableStateFlow("")

    private val screenCapturePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        Timber.tag(TAG).d("screenCapturePermissionLauncher resultCode=${result.resultCode} token=${CaptureRepository.mediaProjectionToken}")
        if (result.resultCode == Activity.RESULT_OK && CaptureRepository.mediaProjectionToken != null) {
            lifecycleScope.launch {
                startScreenTranslator()
            }
        } else {
            screenTranslatorRunningFlow.value = false
        }
    }

//    private val snackMessageFlow = MutableStateFlow("")

    private fun applySettingsWindowTheme(isDarkMode: Boolean) {
        currentUiDarkTheme = isDarkMode
        val darkColor = getColor(R.color.settings_background_dark)
        val lightColor = getColor(R.color.settings_background_light)
        val windowColor = if (isDarkMode) darkColor else lightColor

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            window.decorView.setOnApplyWindowInsetsListener { view, insets ->
                val statusBarInsets = insets.getInsets(WindowInsets.Type.statusBars())
                view.setBackgroundColor(windowColor)
                view.setPadding(0, statusBarInsets.top, 0, 0)
                insets
            }
            window.decorView.requestApplyInsets()
        } else {
            window.statusBarColor = windowColor
            window.navigationBarColor = windowColor
        }

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !isDarkMode
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightNavigationBars = !isDarkMode
    }

    private suspend fun startScreenTranslator() {
        if (!MenuBarView.INSTANCE.isRunning.get()) {
            MenuBarView.INSTANCE.cast(applicationContext)
        }
        if (!TargetHandleView.INSTANCE.isRunning.get()) {
            TargetHandleView.INSTANCE.cast(applicationContext)
        }
        screenTranslatorRunningFlow.value = true
    }

    private fun requestScreenCaptureAndStart() {
        if (CaptureRepository.mediaProjectionToken != null) {
            lifecycleScope.launch {
                startScreenTranslator()
            }
            return
        }

        val intent = Intent(this, ScreenCapturePermissionRequesterActivity::class.java)
        screenCapturePermissionLauncher.launch(intent)
    }

    private fun stopScreenTranslator() {
        closeTranslation()
        MenuBarView.INSTANCE.clear()
        TargetHandleView.INSTANCE.clear()
        applicationContext.finishService()
        screenTranslatorRunningFlow.value = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ScreenInfoHolder.collectAndStoreScreenInfo(this)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            val policy = ThreadPolicy.Builder().permitAll().build()
            StrictMode.setThreadPolicy(policy)
        }

        setContent {
            val isDarkMode by viewModel.preferenceRepository.uiDarkThemeFlow.collectAsStateWithLifecycle(
                initialValue = true
            )

            SideEffect {
                applySettingsWindowTheme(isDarkMode)
            }

            ScreenTranslatorTheme(
                darkTheme = isDarkMode,
                dynamicColor = false
            ) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val backgroundColor = if (isDarkMode) {
                        colorResource(R.color.settings_background_dark)
                    } else {
                        colorResource(R.color.settings_background_light)
                    }

                    val snackBarHostState = remember { SnackbarHostState() }

//                    LaunchedEffect(snackMessageFlow) {
//                        snackMessageFlow.collect { message ->
//                            if (message.isNotEmpty()) {
//                                lifecycleScope.launch {
//                                    snackBarHostState.showSnackbar(
//                                        message = message,
//                                        duration = SnackbarDuration.Short,
////                                        actionLabel = "snackbar"
//                                    )
//                                }
//                            }
//                        }
//                    }

                    var selectedTab by remember { mutableIntStateOf(0) }

                    Scaffold(
                        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
                        bottomBar = {
                            NavigationBar(
                                containerColor = backgroundColor,
                            ) {
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Home, contentDescription = "Trang chủ") },
                                    label = { Text("Trang chủ") },
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Translate, contentDescription = "Dịch") },
                                    label = { Text("Dịch") },
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 }
                                )
                                NavigationBarItem(
                                    icon = { Icon(Icons.Default.Settings, contentDescription = "Cài đặt") },
                                    label = { Text("Cài đặt") },
                                    selected = selectedTab == 2,
                                    onClick = { selectedTab = 2 }
                                )
                            }
                        }
                    ) { _paddingValues: PaddingValues ->
                        val paddingValues = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            PaddingValues(bottom = _paddingValues.calculateBottomPadding())
                        } else {
                            _paddingValues
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(backgroundColor)
                                .padding(paddingValues)
                        ) {
                            when (selectedTab) {
                                0 -> HomeTab(
                                    viewModel = viewModel,
                                    paddingValues = paddingValues,
                                    screenTranslatorRunningFlow = screenTranslatorRunningFlow,
                                    onStartStopClick = {
                                        if (screenTranslatorRunningFlow.value) {
                                            stopScreenTranslator()
                                        } else {
                                            requestScreenCaptureAndStart()
                                        }
                                    }
                                )
                                1 -> TranslateTab(
                                    viewModel = viewModel,
                                    paddingValues = paddingValues
                                )
                                2 -> Settings(
                                    paddingValues = paddingValues
                                )
                            }
                        }
                    }

                }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (LanguageListView.INSTANCE.isRunning.get()) {
                    LanguageListView.INSTANCE.clear()
                } else if (HelpTextDetectModeView.INSTANCE.isRunning.get()) {
                    HelpTextDetectModeView.INSTANCE.clear()
                } else if (HelpTranslationKitView.INSTANCE.isRunning.get()) {
                    HelpTranslationKitView.INSTANCE.clear()
                } else if (SliderDialogView.INSTANCE.isRunning.get()) {
                    closeTranslation()
                    SliderDialogView.INSTANCE.clear()
                } else if (VoiceListView.INSTANCE.isRunning.get()) {
                    VoiceListView.INSTANCE.clear()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            applySettingsWindowTheme(currentUiDarkTheme)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    override fun onResume() {
        super.onResume()

        liveStateFlow.value = true
        screenTranslatorRunningFlow.value =
            MenuBarView.INSTANCE.isRunning.get() || TargetHandleView.INSTANCE.isRunning.get()
    }

    override fun onPause() {
        closeTranslation()
        LanguageListView.INSTANCE.clear()
        SliderDialogView.INSTANCE.clear()
        VoiceListView.INSTANCE.clear()
        HelpTextDetectModeView.INSTANCE.clear()
        HelpTranslationKitView.INSTANCE.clear()
        liveStateFlow.value = false
        super.onPause()
    }

    private var _textDetectMode: TextDetectMode? = null

    private fun runTranslation(point: Point, textDetectMode: TextDetectMode) {
        _textDetectMode = textDetectMode
        TargetHandleView.INSTANCE.runTranslation(point, textDetectMode)
    }

    private fun closeTranslation() {
        TargetHandleView.INSTANCE.closeTranslation(_textDetectMode)
    }

    enum class MenuItemPosition {
        Single,
        Top,
        Middle,
        Bottom,
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                          Composable                                        //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////
    @SuppressLint("LocalContextConfigurationRead")
    @Composable
    fun Settings(
        paddingValues: PaddingValues,
    ) {
        val context = LocalContext.current
        val localView = LocalView.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val coroutineScope = rememberCoroutineScope()

        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionCode: Long = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }

        val latestVersionCode by viewModel.latestVersionCodeFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = 0
        )

        val layoutDirection = LocalLayoutDirection.current
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val configuration = LocalConfiguration.current
        val isPortrait = configuration.orientation == Configuration.ORIENTATION_PORTRAIT

        val menuExpandAnimationDuration = 300

        val contentPadding = dimensionResource(R.dimen.settings_screen_horizontal_padding)
        val sectionSpacing = dimensionResource(R.dimen.settings_section_spacing)
        val cornerRound = dimensionResource(R.dimen.settings_card_radius)
        val appBarHeight = dimensionResource(R.dimen.settings_appbar_height)
        val headerChipSize = dimensionResource(R.dimen.settings_header_chip_size)
        val rowMinHeight = dimensionResource(R.dimen.settings_row_min_height)
        val startPadding = paddingValues.calculateLeftPadding(layoutDirection).toPx(context)

        val isDarkMode = LocalAppDarkTheme.current
        val backgroundColor = if (isDarkMode) colorResource(R.color.settings_background_dark) else colorResource(R.color.settings_background_light)
        val appBarColor = if (isDarkMode) colorResource(R.color.settings_appbar_dark) else colorResource(R.color.settings_appbar_light)
        val contentColor = if (isDarkMode) colorResource(R.color.settings_text_primary_dark) else colorResource(R.color.settings_text_primary_light)
        val subContentColor = if (isDarkMode) colorResource(R.color.settings_text_secondary_dark) else colorResource(R.color.settings_text_secondary_light)
        val accentColor = colorResource(R.color.settings_accent)
        val accentSoftColor = if (isDarkMode) colorResource(R.color.settings_accent_soft) else colorResource(R.color.settings_accent_soft_light)
        val dividerColor = if (isDarkMode) colorResource(R.color.settings_divider_dark) else colorResource(R.color.settings_divider_light)

        // Pointer docking delay
        val dockingDelayTextOffset = remember { mutableStateOf(Point(0, 0)) }
        val dockingDelaySubtextOffset = remember { mutableStateOf(Point(0, 0)) }
        val dockingDelay by viewModel.preferenceRepository.dockingDelayFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = 3000L
        )

        // Haptic feedback to detection
        val dragHandleHaptic by viewModel.preferenceRepository.dragHandleHapticFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = false
        )

        // Menubar Visibility
        val menuBarVisibility by viewModel.preferenceRepository.menuBarVisibilityFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = true
        )

        // Menubar transparency
        val menuBarTransparencyTextOffset = remember { mutableStateOf(Point(0, 0)) }
        val menuBarTransparencySubtextOffset = remember { mutableStateOf(Point(0, 0)) }
        val menuBarTransparency by viewModel.preferenceRepository.menuBarTransparencyFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = 1.0f
        )

        // Menubar Composition
        val menuBarConfigTextOffset = remember { mutableStateOf(Point(0, 0)) }
        val menuBarConfigSubOffset = remember { mutableStateOf(Point(0, 0)) }
        val menuBarConfig by viewModel.preferenceRepository.menuBarConfigFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = MenuConfig.WHOLE
        )

        // Translation transparency
        val translationTransparencyTextOffset = remember { mutableStateOf(Point(0, 0)) }
        val translationTransparencySubtextOffset = remember { mutableStateOf(Point(0, 0)) }
        val translationPoint = remember { mutableStateOf(Point(0, 0)) }
        val translationTransparency by viewModel.preferenceRepository.translationTransparencyFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = 1.0f
        )

        // Translation close delay
        val translationCloseDelayTextOffset = remember { mutableStateOf(Point(0, 0)) }
        val translationCloseDelaySubtextOffset = remember { mutableStateOf(Point(0, 0)) }
        val translationCloseDelay by viewModel.preferenceRepository.translationCloseDelayFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = 1600L
        )

        // Reply transparency
        val replyTransparencyTextOffset = remember { mutableStateOf(Point(0, 0)) }
        val replyTransparencySubtextOffset = remember { mutableStateOf(Point(0, 0)) }
        val replyTransparency by viewModel.preferenceRepository.replyTransparencyFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = 1.0f
        )

        // AI text correction
        val useCorrectionKit by viewModel.preferenceRepository.useCorrectionKitFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = false
        )

        val correctionKit by viewModel.preferenceRepository.correctionKitTypeFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = CorrectionKitType.CHAT_GPT
        )

        // Automatic translation playback
        val automaticTranslationPlayback by viewModel.preferenceRepository.automaticTranslationPlaybackFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = false
        )

        // TTS Speech rate
        val ttsSpeechRateTextOffset = remember { mutableStateOf(Point(0, 0)) }
        val ttsSpeechRateSubtextOffset = remember { mutableStateOf(Point(0, 0)) }
        val ttsSpeechRate by viewModel.preferenceRepository.ttsSpeechRateFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = 1.0f
        )

        // TTS Pitch
        val ttsPitchTextOffset = remember { mutableStateOf(Point(0, 0)) }
        val ttsPitchSubtextOffset = remember { mutableStateOf(Point(0, 0)) }
        val ttsPitch by viewModel.preferenceRepository.ttsPitchFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = 1.0f
        )

        // TTS Voices
        val ttsAvailableVoices by viewModel.ttsRepository.availableVoicesFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = emptyList()
        )

        val ttsAvailableLanguages by viewModel.ttsRepository.availableLanguagesFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = emptyList()
        )

        val ttsAvailableEngines by viewModel.ttsRepository.availableEnginesFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = emptyList()
        )

        val ttsSelectedLanguageTag by viewModel.ttsRepository.selectedLanguageTagFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = ""
        )

        val ttsSelectedVoice by viewModel.ttsRepository.selectedVoiceFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = null
        )

        val savedTtsVoiceName by viewModel.preferenceRepository.ttsVoiceNameFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = ""
        )

        val ttsCurrentEnginePackage by viewModel.ttsRepository.currentEnginePackageFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = ""
        )

        val ttsSelectedLanguage = ttsAvailableLanguages.firstOrNull { language ->
            language.languageTag.equals(ttsSelectedLanguageTag, ignoreCase = true)
        }

        val ttsVoicesForSelectedLanguage = remember(ttsAvailableVoices, ttsSelectedLanguageTag) {
            ttsAvailableVoices.filter { voice ->
                val languageTag = voice.locale.toLanguageTag()
                languageTag.equals(ttsSelectedLanguageTag, ignoreCase = true) ||
                    (
                        ttsSelectedLanguageTag.isNotBlank() &&
                            voice.locale.language.equals(
                                java.util.Locale.forLanguageTag(ttsSelectedLanguageTag).language,
                                ignoreCase = true
                            )
                        )
            }
        }

        var showTtsEngineDialog by remember { mutableStateOf(false) }
        var showTtsLanguageDialog by remember { mutableStateOf(false) }
        var showTtsVoiceDialog by remember { mutableStateOf(false) }

        val screenTranslatorRunning by screenTranslatorRunningFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = false
        )

        LaunchedEffect(Unit) {
            viewModel.ttsRepository.primeEngineState(delayMs = 250L)
        }

        if (showTtsEngineDialog) {
            TtsSelectionDialog(
                title = getString(R.string.settings_menu_tts_engine),
                options = ttsAvailableEngines,
                optionKey = { engine -> engine.packageName },
                optionLabel = { engine -> engine.label },
                optionSupportingText = { engine ->
                    "${engine.languageCount} languages • ${engine.voiceCount} voices"
                },
                isSelected = { engine -> engine.packageName == ttsCurrentEnginePackage },
                onSelect = { engine ->
                    viewModel.ttsRepository.setEnginePackage(engine.packageName)
                },
                onDismiss = { showTtsEngineDialog = false }
            )
        }

        if (showTtsLanguageDialog) {
            TtsSelectionDialog(
                title = getString(R.string.settings_menu_tts_language),
                options = ttsAvailableLanguages,
                optionKey = { language -> language.languageTag },
                optionLabel = { language -> language.displayName },
                optionSupportingText = { language ->
                    "${language.languageTag} • ${language.voiceCount} voices"
                },
                isSelected = { language ->
                    language.languageTag.equals(ttsSelectedLanguageTag, ignoreCase = true)
                },
                onSelect = { language ->
                    viewModel.ttsRepository.selectLanguage(language.languageTag)
                },
                searchEnabled = true,
                searchPlaceholder = getString(android.R.string.search_go),
                onDismiss = { showTtsLanguageDialog = false }
            )
        }

        if (showTtsVoiceDialog) {
            TtsSelectionDialog(
                title = getString(R.string.settings_menu_tts_voices),
                options = ttsVoicesForSelectedLanguage,
                optionKey = { voice -> voice.name },
                optionLabel = { voice -> voice.name },
                optionSupportingText = { voice ->
                    voice.locale.getDisplayName()
                },
                isSelected = { voice ->
                    ttsSelectedVoice?.name.equals(voice.name, ignoreCase = true)
                },
                onSelect = { voice ->
                    viewModel.ttsRepository.selectVoice(voice.name)
                },
                onOptionAction = { voice ->
                    viewModel.playTtsVoicePreview(voice)
                },
                optionActionContentDescription = { voice ->
                    "Preview ${voice.name}"
                },
                onDismiss = { showTtsVoiceDialog = false }
            )
        }

        // Text detect mode
        val textDetectMode by viewModel.preferenceRepository.textDetectModeFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = TextDetectMode.SENTENCE
        )

        // source language
        val sourceLanguageCode by viewModel.preferenceRepository.sourceLanguageCodeFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = "auto"
        )
        val sourceLanguage = viewModel.translationRepository.getSupportedSourceLanguage(sourceLanguageCode)

        // target language
        val targetLanguageCode by viewModel.preferenceRepository.targetLanguageCodeFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                context.resources.configuration.locales.get(0).language
            } else {
                @Suppress("DEPRECATION")
                context.resources.configuration.locale.language
            }
        )
        val targetLanguage = viewModel.translationRepository.getSupportedTargetLanguage(targetLanguageCode)

        // translationKit Type
        val kitType by viewModel.preferenceRepository.translationKitTypeFlow.collectAsStateWithLifecycle(
            lifecycle = lifecycleOwner.lifecycle,
            initialValue = TranslationKitType.GOOGLE
        )

        fun getTransparencyValueText(transparency: Float): String {
            return "${ceil((1.0f - transparency) * 100).toInt()}%"
        }

        fun getSecondValueText(second: Long): String {
            return "${round(second / 1000.0 * 10) / 10} sec"
        }

        fun snapToStep(
            value: Float,
            step: Float,
            min: Float,
            max: Float,
            base: Float = 0f,
        ): Float {
            val snapped = (round(((value - base) / step).toDouble()) * step + base).toFloat()
            return snapped.coerceIn(min, max)
        }

        fun formatSingleDecimal(value: Float): String {
            return BigDecimal(value.toDouble()).setScale(1, RoundingMode.HALF_UP).toString()
        }

        fun formatTtsRateValue(value: Float): String {
            return "${formatSingleDecimal(value)}x"
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = appBarColor
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(appBarHeight)
                            .padding(horizontal = contentPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(headerChipSize)
                                .background(accentSoftColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_menubar),
                                contentDescription = "Settings",
                                modifier = Modifier.size(18.dp),
                                colorFilter = ColorFilter.tint(accentColor)
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.settings_title),
                            color = accentColor,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .weight(1f)
                        )

                        SettingsTopBarActionButton(
                            onClick = {
                                viewModel.updateUiDarkTheme(!isDarkMode)
                            }
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = if (isDarkMode) {
                                    stringResource(R.string.settings_action_switch_to_light)
                                } else {
                                    stringResource(R.string.settings_action_switch_to_dark)
                                },
                                modifier = Modifier.size(18.dp),
                                tint = accentColor
                            )
                        }
                    }
                    HorizontalDivider(color = dividerColor, thickness = 1.dp)
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(0.dp)
                        .onGloballyPositioned { layoutCoordinates ->
                            val center = layoutCoordinates.boundsInRoot().center
                            val endPadding = paddingValues.calculateRightPadding(layoutDirection).toPx(context)
                            val posX = (endPadding - startPadding) / 2
                            val topPadding = paddingValues.calculateTopPadding().toPx(context)
                            val rawPosY = center.y.toInt() - topPadding - (if (isPortrait) 0.dp else 12.dp).toPx(context)
                            val minTopMargin = 28.dp.toPx(context)
                            val posY = max(rawPosY, minTopMargin)
                            menuBarViewSettlePositionFlow.value = Point(posX, posY)
                        }
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(viewModel.scrollState)
                        .padding(horizontal = contentPadding, vertical = sectionSpacing)
                ) {
                SectionCard(
                    painter = painterResource(id = R.drawable.ic_drag_handle),
                    categoryName = getString(R.string.settings_menu_cat_pointer),
                    iconSize = 19.dp,
                    isRtl = isRtl,
                    modifier = Modifier.padding(bottom = sectionSpacing)
                ) {
                    MenuTextItem(
                        menuItemPosition = MenuItemPosition.Top,
                        text = getString(R.string.settings_menu_pointer_docking_delay),
                        paddingValues = paddingValues,
                        onTextPositioned = { offset ->
                            dockingDelayTextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        subText = if (dockingDelay < 15000L) getSecondValueText(dockingDelay) else "∞",
                        onSubtextPositioned = { offset ->
                            dockingDelaySubtextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        onClick = {
                            coroutineScope.launch {
                                settingStringFlow.value = getSecondValueText(dockingDelay)
                                SliderDialogView.INSTANCE.cast(
                                    applicationContext = applicationContext,
                                    initialValue = dockingDelay.toFloat(),
                                    valueRange = 1000f..15000f,
                                    steps = 13,
                                    onValueChange = { value ->
                                        viewModel.updateDockingDelay(value.toLong())
                                        viewModel.updateDragHandleDocking(value < 15000.0f)
                                        settingStringFlow.value = getSecondValueText(value.toLong())
                                    },
                                    menuText = Pair(getString(R.string.settings_menu_pointer_docking_delay), dockingDelayTextOffset.value),
                                    dockingDelayText = Pair(settingStringFlow, dockingDelaySubtextOffset.value),
                                    onDismissRequest = {
                                        SliderDialogView.INSTANCE.clear()
                                    },
                                )
                            }
                        }
                    )
                    MenuItem(
                        menuItemPosition = MenuItemPosition.Bottom,
                        onClick = {
                            if (!dragHandleHaptic) {
                                context.vibrate()
                            }
                            viewModel.updateDragHandleHaptic(!dragHandleHaptic)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MenuText(text = getString(R.string.settings_menu_haptic_feedback_to_detection))
                            SettingsSwitchControl(
                                checked = dragHandleHaptic,
                                onCheckedChange = { value ->
                                    if (value) {
                                        context.vibrate()
                                    }
                                    viewModel.updateDragHandleHaptic(value)
                                },
                                onContentDescription = "Haptic feedback to detection on",
                                offContentDescription = "Haptic feedback to detection off"
                            )
                        }
                    }
                }

                SectionCard(
                    painter = painterResource(id = R.drawable.ic_menubar),
                    categoryName = getString(R.string.settings_menu_cat_menubar),
                    iconSize = 20.dp,
                    isRtl = isRtl,
                    modifier = Modifier.padding(bottom = sectionSpacing)
                ) {
                    MenuTextItem(
                        menuItemPosition = if (menuBarVisibility) MenuItemPosition.Top else MenuItemPosition.Single,
                        text = getString(R.string.settings_menu_menubar_transparency),
                        paddingValues = paddingValues,
                        onTextPositioned = { offset ->
                            menuBarTransparencyTextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        subText = if ((1.0f - menuBarTransparency) > 0.49f) {
                            context.getString(android.R.string.cancel)
                        } else {
                            getTransparencyValueText(menuBarTransparency)
                        },
                        onSubtextPositioned = { offset ->
                            menuBarTransparencySubtextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        onClick = {
                            coroutineScope.launch {
                                settingStringFlow.value = getTransparencyValueText(menuBarTransparency)
                                SliderDialogView.INSTANCE.cast(
                                    applicationContext = applicationContext,
                                    initialValue = 1.0f - menuBarTransparency,
                                    valueRange = 0.0f..0.50f,
                                    onValueChange = { value ->
                                        viewModel.updateMenuBarTransparency(1.0f - value)
                                        viewModel.updateMenuBarVisibility(value < 0.5f)
                                        settingStringFlow.value = getTransparencyValueText(1.0f - value)
                                    },
                                    menuText = Pair(getString(R.string.settings_menu_menubar_transparency), menuBarTransparencyTextOffset.value),
                                    menuBarVisibilityText = Pair(settingStringFlow, menuBarTransparencySubtextOffset.value),
                                    onDismissRequest = {
                                        SliderDialogView.INSTANCE.clear()
                                    },
                                )
                            }
                        }
                    )

                    AnimatedVisibility(
                        visible = menuBarVisibility,
                        enter = expandVertically(animationSpec = tween(menuExpandAnimationDuration)),
                        exit = shrinkVertically(animationSpec = tween(menuExpandAnimationDuration)),
                    ) {
                        fun onClick() {
                            coroutineScope.launch {
                                settingStringFlow.value = menuBarConfig.name
                                SliderDialogView.INSTANCE.cast(
                                    applicationContext = applicationContext,
                                    initialValue = when (menuBarConfig) {
                                        MenuConfig.WHOLE -> 0.0f
                                        MenuConfig.DETECT_MODE_LANGUAGE -> 1.0f
                                        MenuConfig.LANGUAGE_TRANSLATION_KIT -> 2.0f
                                        MenuConfig.LANGUAGE -> 3.0f
                                        MenuConfig.WHOLE_SHORT -> 4.0f
                                        MenuConfig.DETECT_MODE_LANGUAGE_SHORT -> 5.0f
                                        MenuConfig.LANGUAGE_SHORT_TRANSLATION_KIT -> 6.0f
                                        MenuConfig.LANGUAGE_SHORT -> 7.0f
                                        MenuConfig.DETECT_MODE_TRANSLATION_KIT -> 8.0f
                                        MenuConfig.DETECT_MODE -> 9.0f
                                        MenuConfig.TRANSLATION_KIT -> 10.0f
                                        MenuConfig.V_DETECT_MODE -> 11.0f
                                        MenuConfig.V_DETECT_MODE_TRANSLATION_KIT -> 12.0f
                                        MenuConfig.V_LANGUAGE -> 13.0f
                                        MenuConfig.V_LANGUAGE_TRANSLATION_KIT -> 14.0f
                                        MenuConfig.V_DETECT_MODE_LANGUAGE -> 15.0f
                                        MenuConfig.V_WHOLE -> 16.0f
                                    },
                                    valueRange = 0.0f..16.0f,
                                    steps = 15,
                                    onValueChange = { value ->
                                        val updatedMenuBarConfig = when (value.roundToInt().toFloat()) {
                                            0.0f -> MenuConfig.WHOLE
                                            1.0f -> MenuConfig.DETECT_MODE_LANGUAGE
                                            2.0f -> MenuConfig.LANGUAGE_TRANSLATION_KIT
                                            3.0f -> MenuConfig.LANGUAGE
                                            4.0f -> MenuConfig.WHOLE_SHORT
                                            5.0f -> MenuConfig.DETECT_MODE_LANGUAGE_SHORT
                                            6.0f -> MenuConfig.LANGUAGE_SHORT_TRANSLATION_KIT
                                            7.0f -> MenuConfig.LANGUAGE_SHORT
                                            8.0f -> MenuConfig.DETECT_MODE_TRANSLATION_KIT
                                            9.0f -> MenuConfig.DETECT_MODE
                                            10.0f -> MenuConfig.TRANSLATION_KIT
                                            11.0f -> MenuConfig.V_DETECT_MODE
                                            12.0f -> MenuConfig.V_DETECT_MODE_TRANSLATION_KIT
                                            13.0f -> MenuConfig.V_LANGUAGE
                                            14.0f -> MenuConfig.V_LANGUAGE_TRANSLATION_KIT
                                            15.0f -> MenuConfig.V_DETECT_MODE_LANGUAGE
                                            16.0f -> MenuConfig.V_WHOLE
                                            else -> MenuConfig.WHOLE
                                        }
                                        viewModel.updateMenuBarConfig(updatedMenuBarConfig)
                                        settingStringFlow.value = updatedMenuBarConfig.name
                                    },
                                    menuText = Pair(getString(R.string.settings_menu_menubar_composition), menuBarConfigTextOffset.value),
                                    menuBarConfigText = Pair(settingStringFlow, menuBarConfigSubOffset.value),
                                    onDismissRequest = {
                                        SliderDialogView.INSTANCE.clear()
                                    },
                                )
                            }
                        }

                        MenuItem(
                            menuItemPosition = MenuItemPosition.Bottom,
                            onClick = { onClick() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = rowMinHeight + 14.dp)
                                    .onGloballyPositioned { layoutCoordinates ->
                                        val offset = layoutCoordinates.positionOnScreen()
                                        val rowStartPadding = paddingValues.calculateLeftPadding(layoutDirection).toPx(context)
                                        val posX = offset.x.toInt() + layoutCoordinates.size.width - rowStartPadding
                                        menuBarConfigSubOffset.value = Point(posX, offset.y.toInt())
                                    },
                            ) {
                                MenuText(
                                    text = getString(R.string.settings_menu_menubar_composition),
                                    onTextPositioned = { offset ->
                                        menuBarConfigTextOffset.value = Point(offset.x - startPadding, offset.y)
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(top = 6.dp)
                                )
                                val scaleFactor = 0.44f
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .padding(top = 6.dp, bottom = 6.dp)
                                        .wrapContentSize()
                                        .layout { measurable, constraints ->
                                            val placeable = measurable.measure(constraints)
                                            val width = (placeable.width * scaleFactor).toInt()
                                            val height = (placeable.height * scaleFactor).toInt()
                                            layout(width, height) {
                                                placeable.placeRelative(0, 0)
                                            }
                                        }
                                ) {
                                    MenuBar(
                                        menuConfig = menuBarConfig,
                                        scaleFactor = scaleFactor,
                                        shadowPadding = 0.dp,
                                        borderWidth = 1.2.dp,
                                        textDetectMode = textDetectMode,
                                        sourceLanguageCode = sourceLanguageCode,
                                        sourceLanguage = sourceLanguage,
                                        targetLanguageCode = targetLanguageCode,
                                        targetLanguage = targetLanguage,
                                        translationKitType = kitType,
                                        isSwappable = { sourceLanguageCode, targetLanguageCode, kitType ->
                                            viewModel.isLanguageSwappable(sourceLanguageCode, targetLanguageCode, kitType)
                                        },
                                        modifier = Modifier.semantics {
                                            contentDescription = "Menu composition"
                                        }
                                    )
                                    Box(
                                        modifier = Modifier
                                            .matchParentSize()
                                            .clickable(
                                                onClick = { onClick() },
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            )
                                    )
                                }
                            }
                        }
                    }
                }

                SectionCard(
                    painter = painterResource(id = R.drawable.ic_ai),
                    categoryName = getString(R.string.settings_menu_cat_ai),
                    iconSize = 20.dp,
                    isRtl = isRtl,
                    modifier = Modifier.padding(bottom = sectionSpacing)
                ) {
                    MenuItem(
                        menuItemPosition = MenuItemPosition.Single,
                        onClick = { viewModel.updateUseCorrectionKit(!useCorrectionKit) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MenuText(text = "ChatGPT")
                            SettingsSwitchControl(
                                checked = useCorrectionKit,
                                onCheckedChange = { value -> viewModel.updateUseCorrectionKit(value) },
                                onContentDescription = "ChatGPT text correction on",
                                offContentDescription = "ChatGPT text correction off"
                            )
                        }
                    }
                }

                SectionCard(
                    painter = painterResource(id = R.drawable.ic_translation_window),
                    categoryName = getString(R.string.settings_menu_cat_translation),
                    iconSize = 20.dp,
                    isRtl = isRtl,
                    modifier = Modifier.padding(bottom = sectionSpacing)
                ) {
                    MenuTextItem(
                        menuItemPosition = MenuItemPosition.Top,
                        text = getString(R.string.settings_menu_translation_transparency),
                        paddingValues = paddingValues,
                        onTextPositioned = { offset ->
                            translationTransparencyTextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        onGloballyPositioned = { layoutCoordinates ->
                            val center = layoutCoordinates.boundsInWindow().center
                            val rowStartPadding = paddingValues.calculateLeftPadding(layoutDirection).toPx(context)
                            val posX = center.x.toInt() - rowStartPadding
                            translationPoint.value = Point(posX, center.y.toInt())
                        },
                        subText = getTransparencyValueText(translationTransparency),
                        onSubtextPositioned = { offset ->
                            translationTransparencySubtextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        onClick = {
                            if (CaptureRepository.mediaProjectionToken == null) {
                                val intent = Intent(context, ScreenCapturePermissionRequesterActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } else {
                                runTranslation(translationPoint.value, textDetectMode)
                                coroutineScope.launch {
                                    settingStringFlow.value = getTransparencyValueText(translationTransparency)
                                    SliderDialogView.INSTANCE.cast(
                                        applicationContext = applicationContext,
                                        initialValue = 1.0f - translationTransparency,
                                        valueRange = 0.0f..0.5f,
                                        onValueChange = { value ->
                                            viewModel.updateTranslationTransparency(1.0f - value)
                                            settingStringFlow.value = getTransparencyValueText(1.0f - value)
                                        },
                                        menuText = Pair(getString(R.string.settings_menu_translation_transparency), translationTransparencyTextOffset.value),
                                        menuSubtext = Pair(settingStringFlow, translationTransparencySubtextOffset.value),
                                        onDismissRequest = {
                                            closeTranslation()
                                            SliderDialogView.INSTANCE.clear()
                                        },
                                    )
                                }
                            }
                        }
                    )
                    MenuTextItem(
                        menuItemPosition = MenuItemPosition.Middle,
                        text = getString(R.string.settings_menu_translation_close_delay),
                        paddingValues = paddingValues,
                        onTextPositioned = { offset ->
                            translationCloseDelayTextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        subText = getSecondValueText(translationCloseDelay),
                        onSubtextPositioned = { offset ->
                            translationCloseDelaySubtextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        onClick = {
                            coroutineScope.launch {
                                settingStringFlow.value = getSecondValueText(translationCloseDelay)
                                SliderDialogView.INSTANCE.cast(
                                    applicationContext = applicationContext,
                                    initialValue = translationCloseDelay.toFloat(),
                                    valueRange = 500.0f..7000.0f,
                                    steps = 12,
                                    onValueChange = { value ->
                                        viewModel.updateTranslationCloseDelay(value.toLong())
                                        settingStringFlow.value = getSecondValueText(value.toLong())
                                    },
                                    menuText = Pair(getString(R.string.settings_menu_translation_close_delay), translationCloseDelayTextOffset.value),
                                    menuSubtext = Pair(settingStringFlow, translationCloseDelaySubtextOffset.value),
                                    onDismissRequest = {
                                        SliderDialogView.INSTANCE.clear()
                                    },
                                )
                            }
                        }
                    )
                    MenuItem(
                        menuItemPosition = MenuItemPosition.Middle,
                        onClick = { viewModel.updateAutomaticTranslationPlayback(!automaticTranslationPlayback) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MenuText(text = getString(R.string.settings_menu_automated_read_aloud))
                            SettingsSwitchControl(
                                checked = automaticTranslationPlayback,
                                onCheckedChange = { value -> viewModel.updateAutomaticTranslationPlayback(value) },
                                onContentDescription = "Automated read aloud on",
                                offContentDescription = "Automated read aloud off"
                            )
                        }
                    }
                    MenuTextItem(
                        menuItemPosition = MenuItemPosition.Bottom,
                        text = getString(R.string.settings_menu_reply_transparency),
                        paddingValues = paddingValues,
                        onTextPositioned = { offset ->
                            replyTransparencyTextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        subText = getTransparencyValueText(replyTransparency),
                        onSubtextPositioned = { offset ->
                            replyTransparencySubtextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        onClick = {
                            coroutineScope.launch {
                                settingStringFlow.value = getTransparencyValueText(replyTransparency)
                                SliderDialogView.INSTANCE.cast(
                                    applicationContext = applicationContext,
                                    initialValue = 1.0f - replyTransparency,
                                    valueRange = 0.0f..0.5f,
                                    onValueChange = { value ->
                                        viewModel.updateReplyTransparency(1.0f - value)
                                        settingStringFlow.value = getTransparencyValueText(1.0f - value)
                                    },
                                    menuText = Pair(getString(R.string.settings_menu_reply_transparency), replyTransparencyTextOffset.value),
                                    menuSubtext = Pair(settingStringFlow, replyTransparencySubtextOffset.value),
                                    onDismissRequest = {
                                        SliderDialogView.INSTANCE.clear()
                                    },
                                )
                            }
                        }
                    )
                }

                SectionCard(
                    icon = Icons.Default.VoiceChat,
                    categoryName = getString(R.string.settings_menu_cat_tts),
                    iconSize = 19.dp,
                    isRtl = isRtl,
                    modifier = Modifier.padding(bottom = sectionSpacing)
                ) {
                    MenuTextItem(
                        menuItemPosition = MenuItemPosition.Top,
                        text = getString(R.string.settings_menu_tts_engine),
                        paddingValues = paddingValues,
                        subText = ttsAvailableEngines.firstOrNull { it.packageName == ttsCurrentEnginePackage }?.label
                            ?: ttsCurrentEnginePackage.takeIf { it.isNotBlank() }
                            ?: "Loading...",
                        onClick = {
                            if (ttsAvailableEngines.isNotEmpty()) {
                                showTtsEngineDialog = true
                            } else {
                                viewModel.ttsRepository.refreshAvailableEngines()
                            }
                        }
                    )
                    MenuTextItem(
                        menuItemPosition = MenuItemPosition.Middle,
                        text = getString(R.string.settings_menu_tts_language),
                        paddingValues = paddingValues,
                        subText = ttsSelectedLanguage?.displayName
                            ?: ttsSelectedLanguageTag.takeIf { it.isNotBlank() }
                            ?: "Loading...",
                        onClick = {
                            if (ttsAvailableLanguages.isNotEmpty()) {
                                showTtsLanguageDialog = true
                            }
                        }
                    )
                    MenuTextItem(
                        menuItemPosition = MenuItemPosition.Middle,
                        text = getString(R.string.settings_menu_tts_voices),
                        paddingValues = paddingValues,
                        subText = ttsSelectedVoice?.name
                            ?: savedTtsVoiceName.takeIf { it.isNotBlank() }
                            ?: "Loading...",
                        onClick = {
                            if (ttsVoicesForSelectedLanguage.isNotEmpty()) {
                                showTtsVoiceDialog = true
                            }
                        }
                    )
                    MenuTextItem(
                        menuItemPosition = MenuItemPosition.Middle,
                        text = getString(R.string.settings_menu_tts_rate),
                        paddingValues = paddingValues,
                        onTextPositioned = { offset ->
                            ttsSpeechRateTextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        subText = formatTtsRateValue(ttsSpeechRate),
                        onSubtextPositioned = { offset ->
                            ttsSpeechRateSubtextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        onClick = {
                            coroutineScope.launch {
                                settingFloatFlow.value = ttsSpeechRate
                                settingStringFlow.value = formatTtsRateValue(ttsSpeechRate)
                                SliderDialogView.INSTANCE.cast(
                                    applicationContext = applicationContext,
                                    initialValue = ttsSpeechRate,
                                    valueRange = 0.5f..5.0f,
                                    steps = 8,
                                    onValueChange = { value ->
                                        val snappedValue = snapToStep(
                                            value = value,
                                            step = 0.5f,
                                            min = 0.5f,
                                            max = 5.0f,
                                        )
                                        viewModel.updateTtsSpeechRate(snappedValue)
                                        settingFloatFlow.value = snappedValue
                                        settingStringFlow.value = formatTtsRateValue(snappedValue)
                                    },
                                    menuText = Pair(getString(R.string.settings_menu_tts_rate), ttsSpeechRateTextOffset.value),
                                    menuSubtext = Pair(settingStringFlow, ttsSpeechRateSubtextOffset.value),
                                    playDefaultSample = true,
                                    onDismissRequest = {
                                        SliderDialogView.INSTANCE.clear()
                                    },
                                )
                            }
                        }
                    )
                    MenuTextItem(
                        menuItemPosition = MenuItemPosition.Bottom,
                        text = getString(R.string.settings_menu_tts_pitch),
                        paddingValues = paddingValues,
                        onTextPositioned = { offset ->
                            ttsPitchTextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        subText = formatSingleDecimal(ttsPitch),
                        onSubtextPositioned = { offset ->
                            ttsPitchSubtextOffset.value = Point(offset.x - startPadding, offset.y)
                        },
                        onClick = {
                            coroutineScope.launch {
                                settingFloatFlow.value = ttsPitch
                                settingStringFlow.value = formatSingleDecimal(ttsPitch)
                                SliderDialogView.INSTANCE.cast(
                                    applicationContext = applicationContext,
                                    initialValue = ttsPitch,
                                    valueRange = 0.5f..2.0f,
                                    steps = 0,
                                    onValueChange = { value ->
                                        val snappedValue = snapToStep(
                                            value = value,
                                            step = 0.2f,
                                            min = 0.5f,
                                            max = 2.0f,
                                        )
                                        viewModel.updateTtsPitch(snappedValue)
                                        settingFloatFlow.value = snappedValue
                                        settingStringFlow.value = formatSingleDecimal(snappedValue)
                                    },
                                    menuText = Pair(getString(R.string.settings_menu_tts_pitch), ttsPitchTextOffset.value),
                                    menuSubtext = Pair(settingStringFlow, ttsPitchSubtextOffset.value),
                                    playDefaultSample = true,
                                    onDismissRequest = {
                                        SliderDialogView.INSTANCE.clear()
                                    },
                                )
                            }
                        }
                    )
                }

                SectionCard(
                    icon = Icons.Outlined.Info,
                    categoryName = getString(R.string.settings_menu_cat_about),
                    iconSize = 18.dp,
                    isRtl = isRtl,
                    modifier = Modifier.padding(bottom = sectionSpacing)
                ) {
                    MenuTextItem(
                        menuItemPosition = MenuItemPosition.Single,
                        text = getString(R.string.settings_menu_app_version),
                        paddingValues = paddingValues,
                        subText = if (latestVersionCode > versionCode) {
                            "${packageInfo.versionName} New"
                        } else {
                            "${packageInfo.versionName} ${getString(R.string.settings_menu_app_version_latest)}"
                        },
                        onClick = {
                            context.gotoStore(
                                newTask = false,
                                finishService = false
                            )
                            viewModel.analyticsRepository.screenViewReport("AppVersion")
                        },
                        showChevron = false
                    )
                }
                }
            }
        }
    }

    @Composable
    fun <T> TtsSelectionDialog(
        title: String,
        options: List<T>,
        optionKey: (T) -> String,
        optionLabel: (T) -> String,
        optionSupportingText: (T) -> String,
        isSelected: (T) -> Boolean,
        onSelect: (T) -> Unit,
        onOptionAction: ((T) -> Unit)? = null,
        optionActionContentDescription: (T) -> String = { "" },
        searchEnabled: Boolean = false,
        searchPlaceholder: String = title,
        onDismiss: () -> Unit,
    ) {
        var searchQuery by remember(title, options) { mutableStateOf("") }
        val filteredOptions = remember(options, searchEnabled, searchQuery) {
            if (!searchEnabled || searchQuery.isBlank()) {
                options
            } else {
                options.filter { option ->
                    optionLabel(option).contains(searchQuery, ignoreCase = true) ||
                        optionSupportingText(option).contains(searchQuery, ignoreCase = true)
                }
            }
        }
        val isDarkMode = LocalAppDarkTheme.current
        val containerColor = if (isDarkMode) colorResource(R.color.settings_card_dark) else colorResource(R.color.settings_card_light)
        val dividerColor = if (isDarkMode) colorResource(R.color.settings_divider_dark) else colorResource(R.color.settings_divider_light)
        val titleColor = if (isDarkMode) colorResource(R.color.settings_text_primary_dark) else colorResource(R.color.settings_text_primary_light)
        val supportingColor = if (isDarkMode) colorResource(R.color.settings_text_secondary_dark) else colorResource(R.color.settings_text_secondary_light)
        val accentColor = colorResource(R.color.settings_accent)
        val accentSoftColor = if (isDarkMode) colorResource(R.color.settings_accent_soft) else colorResource(R.color.settings_accent_soft_light)
        val dialogRadius = dimensionResource(R.dimen.settings_dialog_radius)

        AlertDialog(
            onDismissRequest = onDismiss,
            shape = RoundedCornerShape(dialogRadius),
            containerColor = containerColor,
            titleContentColor = titleColor,
            textContentColor = titleColor,
            title = {
                Text(
                    text = title,
                    color = titleColor,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    if (searchEnabled) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            singleLine = true,
                            label = {
                                Text(text = searchPlaceholder)
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = dividerColor,
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = supportingColor,
                                focusedTextColor = titleColor,
                                unfocusedTextColor = titleColor,
                                cursorColor = accentColor,
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                    ) {
                        LazyColumn {
                            items(filteredOptions, key = optionKey) { option ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = if (isSelected(option)) accentSoftColor else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            onSelect(option)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 6.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected(option),
                                        onClick = {
                                            onSelect(option)
                                            onDismiss()
                                        },
                                        colors = androidx.compose.material3.RadioButtonDefaults.colors(
                                            selectedColor = accentColor,
                                            unselectedColor = supportingColor
                                        )
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 8.dp)
                                    ) {
                                        Text(
                                            text = optionLabel(option),
                                            color = titleColor,
                                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
                                        )
                                        Text(
                                            text = optionSupportingText(option),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = supportingColor
                                        )
                                    }
                                    onOptionAction?.let { action ->
                                        IconButton(
                                            onClick = {
                                                action(option)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.PlayArrow,
                                                contentDescription = optionActionContentDescription(option),
                                                tint = accentColor
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    thickness = 1.dp,
                                    color = dividerColor.copy(alpha = 0.55f)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(android.R.string.cancel),
                        color = accentColor
                    )
                }
            }
        )
    }

    @Composable
    fun SettingsTopBarActionButton(
        onClick: () -> Unit,
        content: @Composable () -> Unit,
    ) {
        val isDarkMode = LocalAppDarkTheme.current
        val borderColor = if (isDarkMode) colorResource(R.color.settings_border_dark) else colorResource(R.color.settings_border_light)
        Box(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(36.dp)
                .background(Color.Transparent, CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.Transparent, CircleShape)
            )
            content()
        }
    }

    @Composable
    fun SectionCard(
        icon: ImageVector? = null,
        painter: Painter? = null,
        iconSize: Dp = 22.dp,
        categoryName: String,
        isRtl: Boolean,
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit,
    ) {
        val isDarkMode = LocalAppDarkTheme.current
        val cardRadius = dimensionResource(R.dimen.settings_card_radius)
        val headerChipSize = dimensionResource(R.dimen.settings_header_chip_size)
        val cardColor = if (isDarkMode) colorResource(R.color.settings_card_dark) else colorResource(R.color.settings_card_light)
        val borderColor = if (isDarkMode) colorResource(R.color.settings_border_dark) else colorResource(R.color.settings_border_light)
        val dividerColor = if (isDarkMode) colorResource(R.color.settings_divider_dark) else colorResource(R.color.settings_divider_light)
        val titleColor = if (isDarkMode) colorResource(R.color.settings_text_primary_dark) else colorResource(R.color.settings_text_primary_light)
        val accentColor = colorResource(R.color.settings_accent)
        val accentSoftColor = if (isDarkMode) colorResource(R.color.settings_accent_soft) else colorResource(R.color.settings_accent_soft_light)

        Surface(
            modifier = modifier.fillMaxWidth(),
            color = cardColor,
            shape = RoundedCornerShape(cardRadius),
            border = BorderStroke(1.dp, borderColor)
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
                            .background(accentSoftColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (icon != null) {
                            Icon(
                                imageVector = icon,
                                contentDescription = "$categoryName Icon",
                                modifier = Modifier
                                    .size(iconSize)
                                    .graphicsLayer {
                                        if (isRtl) rotationY = 180f
                                    },
                                tint = accentColor
                            )
                        }
                        if (painter != null) {
                            Image(
                                modifier = Modifier
                                    .size(iconSize)
                                    .graphicsLayer {
                                        if (isRtl) rotationY = 180f
                                    },
                                painter = painter,
                                contentDescription = "$categoryName Image",
                                contentScale = ContentScale.Fit,
                                colorFilter = ColorFilter.tint(accentColor)
                            )
                        }
                    }
                    Text(
                        modifier = Modifier.padding(start = 10.dp),
                        text = categoryName,
                        color = titleColor,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = dividerColor
                )
                content()
            }
        }
    }

    @Composable
    fun MenuItem(
        menuItemPosition: MenuItemPosition,
        onClick: (() -> Unit)? = null,
        composable: @Composable () -> Unit,
    ) {
        val isDarkMode = LocalAppDarkTheme.current
        val dividerColor = if (isDarkMode) colorResource(R.color.settings_divider_dark) else colorResource(R.color.settings_divider_light)
        val rowMinHeight = dimensionResource(R.dimen.settings_row_min_height)
        val interactionSource = remember { MutableInteractionSource() }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (menuItemPosition == MenuItemPosition.Middle || menuItemPosition == MenuItemPosition.Bottom) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = dividerColor
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
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = rowMinHeight),
                ) {
                    composable()
                }
            }
        }
    }

    @Composable
    fun MenuTextItem(
        menuItemPosition: MenuItemPosition,
        text: String,
        paddingValues: PaddingValues,
        onTextPositioned: ((Point) -> Unit)? = null,
        onGloballyPositioned: ((LayoutCoordinates) -> Unit)? = null,
        subText: String? = null,
        onSubtextPositioned: ((Point) -> Unit)? = null,
        onClick: (() -> Unit)? = null,
        menuTextModifier: Modifier = Modifier,
        showChevron: Boolean = onClick != null,
    ) {
        MenuItem(
            menuItemPosition = menuItemPosition,
            onClick = onClick,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = dimensionResource(R.dimen.settings_row_min_height)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MenuText(
                    text = text,
                    onGloballyPositioned = onGloballyPositioned,
                    onTextPositioned = onTextPositioned,
                    modifier = menuTextModifier.weight(1f)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    subText?.let {
                        MenuSubText(
                            text = it,
                            paddingValues = paddingValues,
                            onSubtextPositioned = onSubtextPositioned
                        )
                    }
                    if (showChevron) {
                        val chevronTint = if (LocalAppDarkTheme.current) {
                            colorResource(R.color.settings_text_secondary_dark)
                        } else {
                            colorResource(R.color.settings_text_secondary_light)
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = chevronTint
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun MenuText(
        modifier: Modifier = Modifier,
        text: String,
        onGloballyPositioned: ((LayoutCoordinates) -> Unit)? = null,
        onTextPositioned: ((Point) -> Unit)? = null,
    ) {
        val isDarkMode = LocalAppDarkTheme.current
        val textColor = if (isDarkMode) colorResource(R.color.settings_text_primary_dark) else colorResource(R.color.settings_text_primary_light)
        val fontSize = fontDimensionResource(R.dimen.settings_menu_text_size)

        Text(
            text = text,
            color = textColor,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier.onGloballyPositioned { layoutCoordinates ->
                onGloballyPositioned?.let { it(layoutCoordinates) }
                val offset = layoutCoordinates.positionOnScreen()
                onTextPositioned?.let { it(Point(offset.x.toInt(), offset.y.toInt())) }
            },
        )
    }

    @Composable
    fun MenuSubText(
        text: String,
        paddingValues: PaddingValues,
        onSubtextPositioned: ((Point) -> Unit)? = null,
    ) {
        val context = LocalContext.current
        val layoutDirection = LocalLayoutDirection.current
        val subTextColor = colorResource(R.color.settings_accent)
        val fontSize = fontDimensionResource(R.dimen.settings_menu_subtext_size)

        Text(
            modifier = Modifier
                .padding(end = 4.dp)
                .onGloballyPositioned { layoutCoordinates ->
                    val offset = layoutCoordinates.positionOnScreen()
                    val startPadding = paddingValues
                        .calculateLeftPadding(layoutDirection)
                        .toPx(context)
                    val posX = offset.x.toInt() + layoutCoordinates.size.width - startPadding
                    onSubtextPositioned?.let { it(Point(offset.x.toInt() + layoutCoordinates.size.width, offset.y.toInt())) }
                },
            text = text,
            color = subTextColor,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = fontSize,
                fontWeight = FontWeight.SemiBold
            ),
        )
    }

    @Composable
    fun SettingsSwitchControl(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        onContentDescription: String,
        offContentDescription: String,
    ) {
        val isDarkMode = LocalAppDarkTheme.current
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colorResource(R.color.settings_switch_thumb),
                uncheckedThumbColor = colorResource(R.color.settings_switch_thumb),
                checkedTrackColor = if (isDarkMode) {
                    colorResource(R.color.settings_switch_checked_track_dark)
                } else {
                    colorResource(R.color.settings_switch_checked_track)
                },
                uncheckedTrackColor = if (isDarkMode) {
                    colorResource(R.color.settings_switch_unchecked_track_dark)
                } else {
                    colorResource(R.color.settings_switch_unchecked_track)
                },
                checkedBorderColor = Color.Transparent,
                uncheckedBorderColor = Color.Transparent
            ),
            modifier = Modifier
                .scale(0.82f)
                .semantics {
                    contentDescription = if (checked) onContentDescription else offContentDescription
                }
        )
    }
}
