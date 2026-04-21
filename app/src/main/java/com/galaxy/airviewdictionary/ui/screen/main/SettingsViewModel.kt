package com.galaxy.airviewdictionary.ui.screen.main

import android.speech.tts.Voice
import androidx.compose.foundation.ScrollState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.galaxy.airviewdictionary.data.local.preference.PreferenceRepository
import com.galaxy.airviewdictionary.data.local.secure.SecureRepository
import com.galaxy.airviewdictionary.data.local.tts.TTSRepository
import com.galaxy.airviewdictionary.data.remote.ai.CorrectionKitType
import com.galaxy.airviewdictionary.data.remote.firebase.AnalyticsRepository
import com.galaxy.airviewdictionary.data.remote.firebase.RemoteConfigRepository
import com.galaxy.airviewdictionary.data.remote.translation.TranslationKitType
import com.galaxy.airviewdictionary.data.remote.translation.TranslationResponse
import com.galaxy.airviewdictionary.data.remote.translation.TranslationRepository
import com.galaxy.airviewdictionary.extensions.language
import com.galaxy.airviewdictionary.ui.screen.overlay.menubar.MenuConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val secureRepository: SecureRepository,
    val remoteConfigRepository: RemoteConfigRepository,
    val preferenceRepository: PreferenceRepository,
    val translationRepository: TranslationRepository,
    val ttsRepository: TTSRepository,
    val analyticsRepository: AnalyticsRepository,
) : ViewModel() {

    private val TAG = javaClass.simpleName

    val scrollState = ScrollState(initial = 0)

    fun updateIsReviewDone() {
        preferenceRepository.update(PreferenceRepository.IS_REVIEW_DONE, true)
    }

    fun updateDragHandleDocking(dragHandleDocking: Boolean) {
        preferenceRepository.update(PreferenceRepository.DRAG_HANDLE_DOCKING, dragHandleDocking)
    }

    fun updateDockingDelay(dockingDelay: Long) {
        preferenceRepository.update(PreferenceRepository.DOCKING_DELAY, dockingDelay)
    }

    fun updateDragHandleHaptic(dragHandleHaptic: Boolean) {
        preferenceRepository.update(PreferenceRepository.DRAG_HANDLE_HAPTIC, dragHandleHaptic)
    }

    fun updateMenuBarVisibility(menuVisibility: Boolean) {
        preferenceRepository.update(PreferenceRepository.MENU_BAR_VISIBILITY, menuVisibility)
    }

    fun updateMenuBarTransparency(transparency: Float) {
        preferenceRepository.update(PreferenceRepository.MENU_BAR_TRANSPARENCY, transparency)
    }

    fun updateMenuBarConfig(menuBarConfig: MenuConfig) {
        preferenceRepository.update(PreferenceRepository.MENU_BAR_COMPOSITION, menuBarConfig.name)
    }

    fun updateTranslationTransparency(transparency: Float) {
        preferenceRepository.update(PreferenceRepository.TRANSLATION_TRANSPARENCY, transparency)
    }

    fun updateTranslationCloseDelay(translationCloseDelay: Long) {
        preferenceRepository.update(PreferenceRepository.TRANSLATION_CLOSE_DELAY, translationCloseDelay)
    }

    fun updateReplyTransparency(transparency: Float) {
        preferenceRepository.update(PreferenceRepository.REPLY_TRANSPARENCY, transparency)
    }

    fun updateUseCorrectionKit(useCorrectionKit: Boolean) {
        preferenceRepository.update(PreferenceRepository.USE_CORRECTION_KIT, useCorrectionKit)
    }

    fun updateCorrectionKitType(correctionKitType: CorrectionKitType) {
        preferenceRepository.update(PreferenceRepository.CORRECTION_KIT_TYPE, correctionKitType.name)
    }

    fun updateAutomaticTranslationPlayback(automaticTranslationPlayback: Boolean) {
        preferenceRepository.update(PreferenceRepository.AUTOMATIC_TRANSLATION_PLAYBACK, automaticTranslationPlayback)
    }

    fun updateTtsSpeechRate(speechRate: Float) {
        preferenceRepository.update(PreferenceRepository.TTS_SPEECH_RATE, speechRate)
    }

    fun updateTtsPitch(pitch: Float) {
        preferenceRepository.update(PreferenceRepository.TTS_PITCH, pitch)
    }

    fun playTtsVoicePreview(
        voice: Voice,
        sampleText: String = "It's a voice like this.",
    ) {
        viewModelScope.launch {
            val ttsSpeechRate = preferenceRepository.ttsSpeechRateFlow.first()
            val ttsPitch = preferenceRepository.ttsPitchFlow.first()
            var text = sampleText

            if (voice.language.code != "en") {
                val response: TranslationResponse = translationRepository.request(
                    TranslationKitType.GOOGLE,
                    "en",
                    voice.language.code,
                    text
                )
                if (response is TranslationResponse.Success) {
                    text = response.result.resultText ?: text
                }
            }

            ttsRepository.playTestTTS(
                text = text,
                speechRate = ttsSpeechRate,
                pitch = ttsPitch,
                voice = voice,
            )
        }
    }

    fun isLanguageSwappable(sourceLanguageCode: String, targetLanguageCode: String, kitType: TranslationKitType): Boolean {
        return translationRepository.isLanguageSwappable(sourceLanguageCode, targetLanguageCode, kitType)
    }

    val latestVersionCodeFlow: Flow<Long> =
        remoteConfigRepository.remoteConfigFlow
            .map { remoteConfig ->
                remoteConfig[RemoteConfigRepository.LATEST_VERSION_CODE_KEY]?.asLong() ?: 0
            }

    init {
        Timber.tag(TAG).i("#### init ####")
        translationRepository.acquire()
        ttsRepository.acquire()
    }

    override fun onCleared() {
        translationRepository.release()
        ttsRepository.release()
        super.onCleared()
    }
}
