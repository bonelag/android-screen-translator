package com.galaxy.airviewdictionary.core

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.galaxy.airviewdictionary.ACTION_SERVICE_CONTROL
import com.galaxy.airviewdictionary.EXTRA_NOTIFICATION_SOURCE_NEXT
import com.galaxy.airviewdictionary.EXTRA_NOTIFICATION_TARGET_NEXT
import com.galaxy.airviewdictionary.EXTRA_NOTIFICATION_TRANSLATOR_NEXT
import com.galaxy.airviewdictionary.EXTRA_SERVICE_STOP
import com.galaxy.airviewdictionary.FOREGROUND_SERVICE_NOTIFICATION_ID
import com.galaxy.airviewdictionary.NOTIFICATION_CHANNEL_ID
import com.galaxy.airviewdictionary.NOTIFICATION_CHANNEL_NAME
import com.galaxy.airviewdictionary.R
import com.galaxy.airviewdictionary.REQUEST_CODE_NOTIFICATION_SOURCE
import com.galaxy.airviewdictionary.REQUEST_CODE_NOTIFICATION_TARGET
import com.galaxy.airviewdictionary.REQUEST_CODE_NOTIFICATION_TRANSLATOR
import com.galaxy.airviewdictionary.REQUEST_CODE_SERVICE_STOP
import com.galaxy.airviewdictionary.data.local.capture.CaptureRepository
import com.galaxy.airviewdictionary.data.local.preference.PreferenceRepository
import com.galaxy.airviewdictionary.data.local.screen.ScreenInfoHolder
import com.galaxy.airviewdictionary.data.local.secure.SecureRepository
import com.galaxy.airviewdictionary.data.local.tts.TTSRepository
import com.galaxy.airviewdictionary.data.local.vision.VisionRepository
import com.galaxy.airviewdictionary.data.remote.ai.CorrectionRepository
import com.galaxy.airviewdictionary.data.remote.firebase.AnalyticsRepository
import com.galaxy.airviewdictionary.data.remote.firebase.RemoteConfigRepository
import com.galaxy.airviewdictionary.data.remote.translation.Language
import com.galaxy.airviewdictionary.data.remote.translation.TranslationKitType
import com.galaxy.airviewdictionary.data.remote.translation.TranslationRepository
import com.galaxy.airviewdictionary.ui.screen.intro.SplashActivity
import com.galaxy.airviewdictionary.ui.screen.main.SettingsActivity
import com.galaxy.airviewdictionary.ui.screen.overlay.Event
import com.galaxy.airviewdictionary.ui.screen.overlay.languagelist.LanguageListViewModel
import com.galaxy.airviewdictionary.ui.screen.overlay.languagelist.LanguageListViewModelFactory
import com.galaxy.airviewdictionary.ui.screen.overlay.menubar.MenuBarViewModel
import com.galaxy.airviewdictionary.ui.screen.overlay.menubar.MenuBarViewModelFactory
import com.galaxy.airviewdictionary.ui.screen.overlay.settings.SliderDialogViewModel
import com.galaxy.airviewdictionary.ui.screen.overlay.settings.SliderDialogViewModelFactory
import com.galaxy.airviewdictionary.ui.screen.overlay.targethandle.TargetHandleView
import com.galaxy.airviewdictionary.ui.screen.overlay.targethandle.TargetHandleViewModel
import com.galaxy.airviewdictionary.ui.screen.overlay.targethandle.TargetHandleViewModelFactory
import com.galaxy.airviewdictionary.ui.screen.overlay.voicelist.VoiceListViewModel
import com.galaxy.airviewdictionary.ui.screen.overlay.voicelist.VoiceListViewModelFactory
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject


/**
 * 오버레이 서비스
 * 스크린 번역 핸들 뷰 [TargetHandleView] 등 오버레이 뷰 [com.galaxy.airviewdictionary.ui.screen.overlay.OverlayView] 들의 생명 주기를 관장 하는 서비스.
 * 오버레이 뷰 에서 필요로 하는 ViewModel 의 공급자 역할도 한다.
 */
@AndroidEntryPoint
class OverlayService : LifecycleService(), SavedStateRegistryOwner, ViewModelStoreOwner {

    private val TAG = javaClass.simpleName

    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val binder = LocalBinder()
    private var notificationObserverJob: Job? = null
    private var notificationObserverStarted = false
    private var isForegroundStarted = false
    private var notificationState = NotificationState()

    private data class NotificationState(
        val sourceLanguageCode: String = "",
        val targetLanguageCode: String = "",
        val translationKitType: TranslationKitType = TranslationKitType.GOOGLE,
    )

    inner class LocalBinder : Binder() {
        fun getService(): OverlayService = this@OverlayService
    }

    override val viewModelStore: ViewModelStore = ViewModelStore()

    override fun onCreate() {
        super.onCreate()
        Timber.tag(TAG).d("#################### OverlayService onCreate ####################")

        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Timber.tag(TAG).i("#### onStartCommand() ####")

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    FOREGROUND_SERVICE_NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION,
                )
            } else {
                startForeground(
                    FOREGROUND_SERVICE_NOTIFICATION_ID,
                    buildNotification(),
                )
            }
            isForegroundStarted = true
            ensureNotificationObserverStarted()
        } catch (e: SecurityException) {
            val intent = Intent(applicationContext, SplashActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to start foreground notification")
            stopSelf()
        }

        intent?.let {
            when (intent.getStringExtra(ACTION_SERVICE_CONTROL)) {
                EXTRA_SERVICE_STOP -> {
                    Timber.tag(TAG).d("EXTRA_SERVICE_STOP")
                    broadcastEvent(Event.Unbind)
                    viewModelStore.clear()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) { // API 24 이상
                        stopForeground(STOP_FOREGROUND_REMOVE)
                    } else {
                        @Suppress("DEPRECATION")
                        stopForeground(true) // Deprecated 되었지만 API 23 지원 위해 사용
                    }
                    stopSelf()
                }

                EXTRA_NOTIFICATION_SOURCE_NEXT -> {
                    lifecycleScope.launch {
                        cycleNotificationSourceLanguage()
                    }
                }

                EXTRA_NOTIFICATION_TRANSLATOR_NEXT -> {
                    lifecycleScope.launch {
                        cycleNotificationTranslator()
                    }
                }

                EXTRA_NOTIFICATION_TARGET_NEXT -> {
                    lifecycleScope.launch {
                        cycleNotificationTargetLanguage()
                    }
                }

                else -> {}
            }
        }

        /**
         * 메모리 공간 부족 으로 서비스 가 종료 되었을 때, 다음 세가지 플래그 에 따라 서비스 는 재 실행 또는 생성을 결정 한다.
         * START_STICKY : 재생성 과 onStartCommand() 호출(with null intent)
         * START_NOT_STICKY : 서비스 재실행 하지 않음
         * START_REDELIVER_INTENT : 재생성 과 onStartCommand() 호출(with same intent)
         */
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        Timber.tag(TAG).i("#### onBind() ####")
        return binder
    }

    override fun onUnbind(intent: Intent): Boolean {
        Timber.tag(TAG).i("#### onUnbind() ####")
        broadcastEvent(Event.Unbind)
        return true // allowRebind
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Timber.tag(TAG).i("#### onConfigurationChanged() ####")
        ScreenInfoHolder.updateScreenInfoInService(this)
        broadcastEvent(Event.ConfigurationChanged)
    }

    override fun onDestroy() {
        Timber.tag(TAG).i("#### onDestroy() ####")
        notificationObserverJob?.cancel()
        if (notificationObserverStarted) {
            translationRepository.release()
        }
        viewModelStore.clear()
        super.onDestroy()
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                   Event, Event Listeners                                   //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private var overlayServiceEventListeners = mutableListOf<OverlayServiceEventListener>()

    fun registerListener(listener: OverlayServiceEventListener) {
        unregisterListener(listener)
        overlayServiceEventListeners.add(listener)
    }

    fun broadcastEvent(event: Event) {
        val listenersSnapshot = ArrayList(overlayServiceEventListeners)
        listenersSnapshot.forEach { listener ->
            listener.onOverlayServiceEvent(this@OverlayService, event)
        }
    }

    fun unregisterListener(listener: OverlayServiceEventListener) {
        val tempList = ArrayList(overlayServiceEventListeners)
        tempList.remove(listener)
        overlayServiceEventListeners = tempList
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                          Repository                                        //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Inject
    lateinit var secureRepository: SecureRepository

    @Inject
    lateinit var remoteConfigRepository: RemoteConfigRepository

//    @Inject
//    lateinit var geoLocaleRepository: GeoLocaleRepository

    @Inject
    lateinit var preferenceRepository: PreferenceRepository

    @Inject
    lateinit var captureRepository: CaptureRepository

    @Inject
    lateinit var visionRepository: VisionRepository

    @Inject
    lateinit var correctionRepository: CorrectionRepository

    @Inject
    lateinit var translationRepository: TranslationRepository

    @Inject
    lateinit var ttsRepository: TTSRepository

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                               TargetHandleViewModel Provider                               //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private lateinit var targetHandleViewModel: TargetHandleViewModel

    fun getTargetHandleViewModel(): TargetHandleViewModel {
        if (!::targetHandleViewModel.isInitialized) {
            val viewModelFactory = TargetHandleViewModelFactory(
                applicationContext = applicationContext,
                secureRepository = secureRepository,
                remoteConfigRepository = remoteConfigRepository,
                preferenceRepository = preferenceRepository,
                captureRepository = captureRepository,
                visionRepository = visionRepository.apply { addObserver(lifecycle) },
                correctionRepository = correctionRepository,
                translationRepository = translationRepository,
                ttsRepository = ttsRepository,
                analyticsRepository = analyticsRepository,
            )
            targetHandleViewModel = ViewModelProvider(this, viewModelFactory)[TargetHandleViewModel::class.java]
        }
        return targetHandleViewModel
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                  MenuBarViewModel Provider                                 //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private lateinit var menuBarViewModel: MenuBarViewModel

    fun getMenuBarViewModel(): MenuBarViewModel {
        if (!::menuBarViewModel.isInitialized) {
            val viewModelFactory = MenuBarViewModelFactory(
                applicationContext = applicationContext,
                preferenceRepository = preferenceRepository,
                translationRepository = translationRepository,
            )
            menuBarViewModel = ViewModelProvider(this, viewModelFactory)[MenuBarViewModel::class.java]
        }
        return menuBarViewModel
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                LanguageListViewModel Provider                              //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private lateinit var languageListViewModel: LanguageListViewModel

    fun getLanguageListViewModel(): LanguageListViewModel {
        if (!::languageListViewModel.isInitialized) {
            val viewModelFactory = LanguageListViewModelFactory(
                applicationContext = applicationContext,
                preferenceRepository = preferenceRepository,
                translationRepository = translationRepository,
            )
            languageListViewModel = ViewModelProvider(this, viewModelFactory)[LanguageListViewModel::class.java]
        }
        return languageListViewModel
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                SliderDialogViewModel Provider                              //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private lateinit var sliderDialogViewModel: SliderDialogViewModel

    fun getSliderDialogViewModel(): SliderDialogViewModel {
        if (!::sliderDialogViewModel.isInitialized) {
            val viewModelFactory = SliderDialogViewModelFactory(
                preferenceRepository = preferenceRepository,
                translationRepository = translationRepository,
                ttsRepository = ttsRepository,
            )
            sliderDialogViewModel = ViewModelProvider(this, viewModelFactory)[SliderDialogViewModel::class.java]
        }
        return sliderDialogViewModel
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                  VoiceListViewModel Provider                               //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private lateinit var voiceListViewModel: VoiceListViewModel

    fun getVoiceListViewModel(): VoiceListViewModel {
        if (!::voiceListViewModel.isInitialized) {
            val viewModelFactory = VoiceListViewModelFactory(
                applicationContext = applicationContext,
                preferenceRepository = preferenceRepository,
                translationRepository = translationRepository,
                ttsRepository = ttsRepository,
            )
            voiceListViewModel = ViewModelProvider(this, viewModelFactory)[VoiceListViewModel::class.java]
        }
        return voiceListViewModel
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //                                                                                            //
    //                                         Notification                                       //
    //                                                                                            //
    ////////////////////////////////////////////////////////////////////////////////////////////////

    private fun observeNotificationState() {
        notificationObserverJob?.cancel()
        notificationObserverJob = lifecycleScope.launch {
            combine(
                preferenceRepository.sourceLanguageCodeFlow,
                preferenceRepository.targetLanguageCodeFlow,
                preferenceRepository.translationKitTypeFlow,
            ) { sourceLanguageCode, targetLanguageCode, translationKitType ->
                NotificationState(
                    sourceLanguageCode = sourceLanguageCode,
                    targetLanguageCode = targetLanguageCode,
                    translationKitType = translationKitType,
                )
            }.distinctUntilChanged()
                .collect { state ->
                    notificationState = state
                    updateForegroundNotification()
                }
        }
    }

    private fun ensureNotificationObserverStarted() {
        if (notificationObserverStarted) {
            return
        }
        notificationObserverStarted = true
        translationRepository.acquire()
        observeNotificationState()
    }

    private fun updateForegroundNotification() {
        if (!isForegroundStarted) {
            return
        }
        try {
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(FOREGROUND_SERVICE_NOTIFICATION_ID, buildNotification())
        } catch (e: Exception) {
            Timber.tag(TAG).e(e, "Failed to update foreground notification")
        }
    }

    private suspend fun cycleNotificationSourceLanguage() {
        val nextLanguageCode = getNextLanguageCode(
            isSourceLanguage = true,
            currentLanguageCode = notificationState.sourceLanguageCode,
            oppositeLanguageCode = notificationState.targetLanguageCode,
            kitType = notificationState.translationKitType,
        ) ?: return

        preferenceRepository.addOrUpdateLanguageHistory(nextLanguageCode, true)
    }

    private suspend fun cycleNotificationTargetLanguage() {
        val nextLanguageCode = getNextLanguageCode(
            isSourceLanguage = false,
            currentLanguageCode = notificationState.targetLanguageCode,
            oppositeLanguageCode = notificationState.sourceLanguageCode,
            kitType = notificationState.translationKitType,
        ) ?: return

        preferenceRepository.addOrUpdateLanguageHistory(nextLanguageCode, false)
    }

    private suspend fun cycleNotificationTranslator() {
        val supportedKitTypes = TranslationKitType.entries

        if (supportedKitTypes.isEmpty()) {
            return
        }

        val currentIndex = supportedKitTypes.indexOf(notificationState.translationKitType)
        val nextIndex = if (currentIndex >= 0) {
            (currentIndex + 1) % supportedKitTypes.size
        } else {
            0
        }
        preferenceRepository.update(
            PreferenceRepository.TRANSLATION_KIT_TYPE,
            supportedKitTypes[nextIndex].name
        )
    }

    private suspend fun getNextLanguageCode(
        isSourceLanguage: Boolean,
        currentLanguageCode: String,
        oppositeLanguageCode: String,
        kitType: TranslationKitType,
    ): String? {
        val historyCodes = if (isSourceLanguage) {
            preferenceRepository.sourceLanguageCodeHistoryFlow.first()
        } else {
            preferenceRepository.targetLanguageCodeHistoryFlow.first()
        }

        val supportedLanguages = if (isSourceLanguage) {
            translationRepository.supportedLanguagesAsSource.filter { language ->
                translationRepository.isSupportedAsSource(kitType, language.code, oppositeLanguageCode)
            }
        } else {
            translationRepository.supportedLanguagesAsTarget.filter { language ->
                translationRepository.isSupportedAsTarget(kitType, language.code, oppositeLanguageCode)
            }
        }

        val orderedCodes = mutableListOf<String>()
        fun addCode(code: String) {
            if (orderedCodes.none { it.equals(code, ignoreCase = true) }) {
                orderedCodes.add(code)
            }
        }

        historyCodes.forEach(::addCode)
        supportedLanguages.map(Language::code).forEach(::addCode)
        if (orderedCodes.none { it.equals(currentLanguageCode, ignoreCase = true) }) {
            orderedCodes.add(0, currentLanguageCode)
        }
        if (orderedCodes.size <= 1) {
            return orderedCodes.firstOrNull()
        }

        val currentIndex = orderedCodes.indexOfFirst { it.equals(currentLanguageCode, ignoreCase = true) }
        val safeIndex = if (currentIndex >= 0) currentIndex else 0
        return orderedCodes[(safeIndex + 1) % orderedCodes.size]
    }

    private fun buildNotificationRemoteViews(): RemoteViews {
        val sourceLabel = formatNotificationLanguageLabel(notificationState.sourceLanguageCode)
        val targetLabel = formatNotificationLanguageLabel(notificationState.targetLanguageCode)
        val translatorLabel = formatNotificationKitLabel(notificationState.translationKitType)

        return RemoteViews(packageName, R.layout.notification_translator_controls).apply {
            setTextViewText(R.id.notification_title, getString(R.string.notification_translator_title))
            setTextViewText(
                R.id.notification_summary,
                getString(R.string.notification_translator_summary_format, sourceLabel, translatorLabel, targetLabel)
            )

            setOnClickPendingIntent(R.id.notification_root, buildSettingsPendingIntent())
            setOnClickPendingIntent(R.id.notification_settings_action, buildSettingsPendingIntent())
            setOnClickPendingIntent(R.id.notification_stop_action, buildServicePendingIntent(REQUEST_CODE_SERVICE_STOP, EXTRA_SERVICE_STOP))
        }
    }

    private fun formatNotificationLanguageLabel(languageCode: String): String {
        if (languageCode.isBlank()) {
            return "--"
        }
        return if (languageCode.equals("auto", ignoreCase = true)) {
            getString(R.string.notification_translator_auto)
        } else {
            Language(languageCode).displayShortName
        }
    }

    private fun formatNotificationKitLabel(kitType: TranslationKitType): String {
        return when (kitType) {
            TranslationKitType.GOOGLE -> "Google"
            TranslationKitType.AZURE -> "Azure"
            TranslationKitType.DEEPL -> "DeepL"
            TranslationKitType.PAPAGO -> "Papago"
        }
    }

    private fun buildSettingsPendingIntent(): PendingIntent {
        return Intent(this, SettingsActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        }
    }

    private fun buildServicePendingIntent(requestCode: Int, actionExtra: String): PendingIntent {
        return PendingIntent.getService(
            applicationContext,
            requestCode,
            Intent(applicationContext, OverlayService::class.java).apply {
                putExtra(ACTION_SERVICE_CONTROL, actionExtra)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildNotification(): Notification {
        // NotificationChannel 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).apply {
                createNotificationChannel( // NotificationChannel 을 시스템에 등록
                    NotificationChannel(
                        NOTIFICATION_CHANNEL_ID, // NotificationChannel 의 고유 식별자
                        NOTIFICATION_CHANNEL_NAME, //  NotificationChannel 의 이름
                        NotificationManager.IMPORTANCE_DEFAULT // Notification 의 중요도 설정
                    )
                )
            }
        }

        val settingsPendingIntent = buildSettingsPendingIntent()
        val notificationViews = buildNotificationRemoteViews()

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_translator_title))
            .setContentText(
                getString(
                    R.string.notification_translator_summary_format,
                    formatNotificationLanguageLabel(notificationState.sourceLanguageCode),
                    formatNotificationKitLabel(notificationState.translationKitType),
                    formatNotificationLanguageLabel(notificationState.targetLanguageCode)
                )
            )
            .setSmallIcon(R.drawable.outline_translate_white_24)
            .setContentIntent(settingsPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCustomContentView(notificationViews)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }
}


