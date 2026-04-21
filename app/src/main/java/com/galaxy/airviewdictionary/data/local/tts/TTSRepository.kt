package com.galaxy.airviewdictionary.data.local.tts

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.galaxy.airviewdictionary.data.AVDRepository
import com.galaxy.airviewdictionary.data.local.preference.PreferenceRepository
import com.galaxy.airviewdictionary.ui.screen.overlay.translation.TTSStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TTSRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferenceRepository: PreferenceRepository,
) : AVDRepository() {

    companion object {
        private const val INIT_TIMEOUT_MS = 5000L
        private const val INIT_SETTLE_DELAY_MS = 150L
    }

    private data class EngineInventory(
        val languages: List<TtsLanguageInfo>,
        val voices: List<Voice>,
        val defaultVoice: Voice?,
    ) {
        companion object {
            val EMPTY = EngineInventory(
                languages = emptyList(),
                voices = emptyList(),
                defaultVoice = null,
            )
        }
    }

    private data class VoiceCandidate(
        val voice: Voice,
        val preferredRank: Int,
        val localeMatchRank: Int,
        val missingDataRank: Int,
        val qualityRank: Int,
        val latencyRank: Int,
    )

    private val repositoryMutex = Mutex()

    private var tts: TextToSpeech? = null
    private var ttsForText: TextToSpeech? = null
    private var engineInventories: Map<String, EngineInventory> = emptyMap()
    private var defaultEnginePackage: String? = null
    @Volatile
    private var isPrimeScheduled = false
    @Volatile
    private var isPrimeComplete = false

    val ttsStatusFlow = MutableStateFlow(TTSStatus.Uninitialized)
    val currentVoiceFlow = MutableStateFlow<Voice?>(null)
    val selectedVoiceFlow = MutableStateFlow<Voice?>(null)
    val availableVoicesFlow = MutableStateFlow<List<Voice>>(emptyList())
    val availableLanguagesFlow = MutableStateFlow<List<TtsLanguageInfo>>(emptyList())
    val availableEnginesFlow = MutableStateFlow<List<TtsEngineInfo>>(emptyList())
    val currentEnginePackageFlow = MutableStateFlow("")
    val selectedLanguageTagFlow = MutableStateFlow("")
    val debugStateFlow = MutableStateFlow("")

    private fun debug(message: String) {
        debugStateFlow.value = message
        Timber.tag(TAG).i(message)
    }

    private fun normalizeRequestedEnginePackage(enginePackageName: String?): String? {
        return enginePackageName?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun normalizeLanguageCode(languageCode: String): String {
        return languageCode.trim().replace('_', '-')
    }

    private fun normalizeVoiceName(voiceName: String?): String? {
        return voiceName?.trim()?.takeIf { it.isNotBlank() }
    }

    private fun localeTag(locale: Locale?): String {
        if (locale == null) return ""
        val tag = locale.toLanguageTag()
        return if (tag.equals("und", ignoreCase = true)) "" else tag
    }

    private fun isUsableVoice(voice: Voice?): Boolean {
        if (voice == null) return false
        if (voice.name.isNullOrBlank()) return false
        return localeTag(voice.locale).isNotBlank()
    }

    private fun buildPreferredVoiceNames(
        requestedPreferredVoiceNames: List<String> = emptyList(),
        selectedVoiceName: String? = null,
        selectedLanguageTag: String? = null,
    ): List<String> {
        val normalizedSelectedLanguageTag = selectedLanguageTag?.takeIf { it.isNotBlank() }
        val preferredVoiceNames = LinkedHashSet<String>()

        if (selectedVoiceName != null) {
            val selectedVoice = availableVoicesFlow.value.firstOrNull { voice ->
                voice.name.equals(selectedVoiceName, ignoreCase = true)
            }
            val selectedVoiceMatchesLanguage = normalizedSelectedLanguageTag == null ||
                (selectedVoice != null && matchesVoiceLocale(selectedVoice, normalizedSelectedLanguageTag))
            if (selectedVoiceMatchesLanguage) {
                preferredVoiceNames += selectedVoiceName
            }
        }

        requestedPreferredVoiceNames
            .mapNotNull(::normalizeVoiceName)
            .forEach { preferredVoiceNames += it }

        return preferredVoiceNames.toList()
    }

    private suspend fun buildPreferredVoiceNamesForLanguage(
        languageCode: String,
        requestedPreferredVoiceNames: List<String> = emptyList(),
    ): List<String> {
        val normalizedLanguageCode = normalizeLanguageCode(languageCode)
        val persistedLanguageTag = normalizeLanguageCode(preferenceRepository.ttsLanguageTagFlow.first())
            .takeIf { it.isNotBlank() }
        val selectedLanguageTag = persistedLanguageTag ?: normalizedLanguageCode
        val selectedVoiceName = normalizeVoiceName(preferenceRepository.ttsVoiceNameFlow.first())
            ?: normalizeVoiceName(selectedVoiceFlow.value?.name)
            ?: normalizeVoiceName(currentVoiceFlow.value?.name)

        return buildPreferredVoiceNames(
            requestedPreferredVoiceNames = requestedPreferredVoiceNames,
            selectedVoiceName = selectedVoiceName,
            selectedLanguageTag = selectedLanguageTag,
        )
    }

    private suspend fun applyVoiceToInstance(
        textToSpeech: TextToSpeech,
        voice: Voice,
        instanceName: String,
    ): Voice? {
        return withContext(Dispatchers.Main.immediate) {
            val languageResult = textToSpeech.setLanguage(voice.locale)
            val voiceResult = textToSpeech.setVoice(voice)
            val actualVoice = runCatching { textToSpeech.voice }.getOrNull()
            debug(
                "applyVoiceToInstance instance=$instanceName requested=${voice.name} locale=${voice.locale} setLanguage=$languageResult setVoice=$voiceResult actual=${actualVoice?.name} actualLocale=${actualVoice?.locale}"
            )

            when {
                actualVoice != null && isUsableVoice(actualVoice) -> actualVoice
                voiceResult == TextToSpeech.SUCCESS -> voice
                else -> null
            }
        }
    }

    private suspend fun syncSelectedLanguageAndVoice(
        enginePackageName: String,
        preferredVoiceNamesOverride: List<String> = emptyList(),
    ) {
        val inventory = engineInventories[enginePackageName] ?: EngineInventory.EMPTY
        val savedLanguageTag = normalizeLanguageCode(preferenceRepository.ttsLanguageTagFlow.first())
            .takeIf { it.isNotBlank() }
        val savedVoiceName = normalizeVoiceName(preferenceRepository.ttsVoiceNameFlow.first())

        val resolvedLanguageTag = inventory.languages.firstOrNull { language ->
            language.languageTag.equals(savedLanguageTag, ignoreCase = true)
        }?.languageTag
            ?: inventory.defaultVoice?.let { voice -> localeTag(voice.locale) }?.takeIf { it.isNotBlank() }
            ?: inventory.languages.firstOrNull()?.languageTag
            ?: ""

        selectedLanguageTagFlow.value = resolvedLanguageTag
        preferenceRepository.update(PreferenceRepository.TTS_LANGUAGE_TAG, resolvedLanguageTag)

        val preferredVoiceNames = buildPreferredVoiceNames(
            requestedPreferredVoiceNames = preferredVoiceNamesOverride,
            selectedVoiceName = savedVoiceName,
            selectedLanguageTag = resolvedLanguageTag,
        )

        val resolvedVoice = inventory.voices.firstOrNull { voice ->
            savedVoiceName != null &&
                voice.name.equals(savedVoiceName, ignoreCase = true) &&
                (resolvedLanguageTag.isBlank() || matchesVoiceLocale(voice, resolvedLanguageTag))
        }
            ?: resolvedLanguageTag.takeIf { it.isNotBlank() }?.let { languageTag ->
                findBestVoice(languageTag, preferredVoiceNames)
            }
            ?: inventory.defaultVoice
            ?: inventory.voices.firstOrNull()

        if (resolvedVoice != null) {
            applyVoiceSelectionInternal(resolvedVoice, persistSelection = true)
        } else {
            selectedVoiceFlow.value = null
            currentVoiceFlow.value = null
            preferenceRepository.update(PreferenceRepository.TTS_VOICE_NAME, "")
        }
    }

    private suspend fun applyVoiceSelectionInternal(
        voice: Voice,
        persistSelection: Boolean,
    ) {
        val appliedPlaybackVoice = tts?.let { playbackTts ->
            applyVoiceToInstance(playbackTts, voice, "playback")
        }

        val appliedPreviewVoice = ttsForText?.let { previewTts ->
            applyVoiceToInstance(previewTts, voice, "preview")
        }

        val resolvedVoice = appliedPlaybackVoice
            ?: appliedPreviewVoice
            ?: voice

        currentVoiceFlow.value = resolvedVoice
        selectedVoiceFlow.value = resolvedVoice

        if (persistSelection) {
            val resolvedLanguageTag = localeTag(resolvedVoice.locale)
            selectedLanguageTagFlow.value = resolvedLanguageTag
            preferenceRepository.update(PreferenceRepository.TTS_LANGUAGE_TAG, resolvedLanguageTag)
            preferenceRepository.update(PreferenceRepository.TTS_VOICE_NAME, resolvedVoice.name)
        }
    }

    private fun queryInstalledEnginesFromPackageManager(): List<TtsEngineInfo> {
        val packageManager = context.packageManager
        val intent = Intent(TextToSpeech.Engine.INTENT_ACTION_TTS_SERVICE)
        return packageManager.queryIntentServices(intent, PackageManager.GET_META_DATA)
            .mapNotNull { resolveInfo ->
                val serviceInfo = resolveInfo.serviceInfo ?: return@mapNotNull null
                val packageName = serviceInfo.packageName ?: return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()
                    ?: serviceInfo.name
                    ?: packageName
                TtsEngineInfo(
                    packageName = packageName,
                    label = label,
                )
            }
            .distinctBy { it.packageName.lowercase(Locale.ROOT) }
    }

    private suspend fun createInitializedTextToSpeech(enginePackageName: String?): TextToSpeech? {
        return withContext(Dispatchers.Main.immediate) {
            val initDeferred = CompletableDeferred<Int>()
            val requestedEnginePackage = normalizeRequestedEnginePackage(enginePackageName)
            val listener = TextToSpeech.OnInitListener { status ->
                if (!initDeferred.isCompleted) {
                    initDeferred.complete(status)
                }
            }

            val textToSpeech = if (requestedEnginePackage == null) {
                TextToSpeech(context, listener)
            } else {
                TextToSpeech(context, listener, requestedEnginePackage)
            }

            val status = withTimeoutOrNull(INIT_TIMEOUT_MS) {
                initDeferred.await()
            }

            if (status == TextToSpeech.SUCCESS) {
                withContext(Dispatchers.IO) {
                    kotlinx.coroutines.delay(INIT_SETTLE_DELAY_MS)
                }
                textToSpeech
            } else {
                runCatching { textToSpeech.shutdown() }
                null
            }
        }
    }

    private suspend fun shutdownTextToSpeech(textToSpeech: TextToSpeech?) {
        if (textToSpeech == null) return
        withContext(Dispatchers.Main.immediate) {
            runCatching { textToSpeech.stop() }
            runCatching { textToSpeech.shutdown() }
        }
    }

    private suspend fun hydrateStoredSelectionState() {
        currentEnginePackageFlow.value =
            normalizeRequestedEnginePackage(preferenceRepository.ttsEnginePackageFlow.first()).orEmpty()
        selectedLanguageTagFlow.value =
            normalizeLanguageCode(preferenceRepository.ttsLanguageTagFlow.first()).takeIf { it.isNotBlank() }.orEmpty()
    }

    private suspend fun loadInstalledEngines(): List<TtsEngineInfo> {
        var bootstrapTts: TextToSpeech? = null
        return try {
            bootstrapTts = createInitializedTextToSpeech(null)
            defaultEnginePackage = runCatching { bootstrapTts?.defaultEngine }
                .getOrNull()
                ?.takeIf { it.isNotBlank() }

            val enginesFromApi = runCatching {
                bootstrapTts?.engines.orEmpty().mapNotNull { engineInfo ->
                    val packageName = engineInfo.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val label = engineInfo.label?.takeIf { it.isNotBlank() } ?: packageName
                    TtsEngineInfo(
                        packageName = packageName,
                        label = label,
                    )
                }
            }.getOrDefault(emptyList())

            val fallbackEngines = queryInstalledEnginesFromPackageManager()
            (enginesFromApi + fallbackEngines)
                .distinctBy { it.packageName.lowercase(Locale.ROOT) }
                .map { engine ->
                    engine.copy(isDefault = engine.packageName == defaultEnginePackage)
                }
                .sortedWith(
                    compareBy<TtsEngineInfo>(
                        { !it.isDefault },
                        { it.label.lowercase(Locale.ROOT) },
                    )
                )
        } finally {
            shutdownTextToSpeech(bootstrapTts)
        }
    }

    private suspend fun loadEngineInventory(engine: TtsEngineInfo): EngineInventory {
        val inventoryTts = createInitializedTextToSpeech(engine.packageName)
        if (inventoryTts == null) {
            debug("Unable to initialize engine inventory for ${engine.packageName}")
            return EngineInventory.EMPTY
        }

        return try {
            val voices = runCatching {
                inventoryTts.voices?.filter { voice -> isUsableVoice(voice) }.orEmpty()
            }.getOrDefault(emptySet())
                .distinctBy { voice -> voice.name.lowercase(Locale.ROOT) }
                .sortedWith(
                    compareBy<Voice>(
                        { voice -> voice.locale.getDisplayName(getCurrentLocale()) },
                        { voice -> localeTag(voice.locale) },
                        { voice -> voice.name.lowercase(Locale.ROOT) },
                    )
                )

            val localesByLanguageTag = linkedMapOf<String, Locale>()

            runCatching {
                inventoryTts.availableLanguages?.filterNotNull().orEmpty()
            }.getOrDefault(emptySet()).forEach { locale ->
                val languageTag = localeTag(locale)
                if (languageTag.isNotBlank()) {
                    localesByLanguageTag.putIfAbsent(languageTag, locale)
                }
            }

            voices.forEach { voice ->
                val languageTag = localeTag(voice.locale)
                if (languageTag.isNotBlank()) {
                    localesByLanguageTag.putIfAbsent(languageTag, voice.locale)
                }
            }

            val languages = localesByLanguageTag.values
                .map { locale ->
                    buildLanguageInfo(
                        textToSpeech = inventoryTts,
                        locale = locale,
                        voices = voices,
                    )
                }
                .sortedBy { language -> language.displayName.lowercase(Locale.ROOT) }

            val defaultVoice = runCatching { inventoryTts.defaultVoice }
                .getOrNull()
                ?.takeIf { voice -> isUsableVoice(voice) }

            EngineInventory(
                languages = languages,
                voices = voices,
                defaultVoice = defaultVoice,
            )
        } finally {
            shutdownTextToSpeech(inventoryTts)
        }
    }

    private suspend fun buildLanguageInfo(
        textToSpeech: TextToSpeech,
        locale: Locale,
        voices: List<Voice>,
    ): TtsLanguageInfo {
        val supportLevel = withContext(Dispatchers.Main.immediate) {
            runCatching { textToSpeech.isLanguageAvailable(locale) }
                .getOrDefault(TextToSpeech.LANG_NOT_SUPPORTED)
        }
        val languageTag = localeTag(locale)
        return TtsLanguageInfo(
            locale = locale,
            languageTag = languageTag,
            displayName = locale.getDisplayName(getCurrentLocale()).ifBlank {
                locale.displayName.ifBlank { languageTag }
            },
            supportLevel = supportLevel,
            voiceCount = voices.count { voice ->
                localeTag(voice.locale).equals(languageTag, ignoreCase = true)
            },
        )
    }

    private fun resolveSelectedEnginePackage(
        requestedEnginePackage: String?,
        savedEnginePackage: String?,
        installedEngines: List<TtsEngineInfo>,
    ): String? {
        val installedPackages = installedEngines.map { it.packageName }.toSet()

        return when {
            requestedEnginePackage != null && requestedEnginePackage in installedPackages -> requestedEnginePackage
            savedEnginePackage != null && savedEnginePackage in installedPackages -> savedEnginePackage
            defaultEnginePackage != null && defaultEnginePackage in installedPackages -> defaultEnginePackage
            else -> installedEngines.firstOrNull()?.packageName
        }
    }

    private suspend fun initializeActiveTextToSpeech(
        enginePackageName: String,
        locale: Locale,
    ): Boolean {
        shutdownTextToSpeech(tts)
        shutdownTextToSpeech(ttsForText)
        tts = null
        ttsForText = null

        val playbackTts = createInitializedTextToSpeech(enginePackageName)
        if (playbackTts == null) {
            debug("Failed to initialize playback TTS for engine=$enginePackageName")
            ttsStatusFlow.value = TTSStatus.Uninitialized
            return false
        }

        configurePlaybackTts(
            textToSpeech = playbackTts,
            enginePackageName = enginePackageName,
            locale = locale,
        )

        tts = playbackTts
        ttsForText = null
        ttsStatusFlow.value = TTSStatus.Idle
        return true
    }

    private suspend fun configurePlaybackTts(
        textToSpeech: TextToSpeech,
        enginePackageName: String,
        locale: Locale,
    ) {
        withContext(Dispatchers.Main.immediate) {
            val languageResult = textToSpeech.setLanguage(locale)
            debug(
                "configurePlaybackTts engine=$enginePackageName setLanguage=$languageResult voice=${textToSpeech.voice?.name} locale=${textToSpeech.voice?.locale}"
            )
            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    debug(
                        "tts onStart utteranceId=$utteranceId engine=${currentEnginePackageFlow.value} voice=${tts?.voice?.name}"
                    )
                    ttsStatusFlow.value = TTSStatus.Playing
                }

                override fun onDone(utteranceId: String?) {
                    debug("tts onDone utteranceId=$utteranceId")
                    ttsStatusFlow.value = TTSStatus.Idle
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    debug("tts onError utteranceId=$utteranceId voice=${tts?.voice?.name} locale=${tts?.voice?.locale}")
                    ttsStatusFlow.value = TTSStatus.Idle
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    debug("tts onStop utteranceId=$utteranceId interrupted=$interrupted")
                    ttsStatusFlow.value = TTSStatus.Idle
                }
            })
        }
    }

    private suspend fun configurePreviewTts(
        textToSpeech: TextToSpeech,
        enginePackageName: String,
        locale: Locale,
    ) {
        withContext(Dispatchers.Main.immediate) {
            val languageResult = textToSpeech.setLanguage(locale)
            debug(
                "configurePreviewTts engine=$enginePackageName setLanguage=$languageResult voice=${textToSpeech.voice?.name} locale=${textToSpeech.voice?.locale}"
            )
        }
    }

    private fun updateAvailableEnginesState(installedEngines: List<TtsEngineInfo>) {
        availableEnginesFlow.value = installedEngines.map { engine ->
            val inventory = engineInventories[engine.packageName]
            engine.copy(
                languageCount = inventory?.languages?.size ?: 0,
                voiceCount = inventory?.voices?.size ?: 0,
            )
        }
    }

    private suspend fun loadAndStoreEngineInventoryLocked(
        enginePackageName: String,
        installedEngines: List<TtsEngineInfo>,
    ): EngineInventory {
        engineInventories[enginePackageName]?.let { cachedInventory ->
            return cachedInventory
        }

        val engine = installedEngines.firstOrNull { installedEngine ->
            installedEngine.packageName.equals(enginePackageName, ignoreCase = true)
        } ?: TtsEngineInfo(
            packageName = enginePackageName,
            label = enginePackageName,
        )

        val inventory = loadEngineInventory(engine)
        engineInventories = engineInventories.toMutableMap().apply {
            put(engine.packageName, inventory)
        }
        updateAvailableEnginesState(installedEngines)
        if (currentEnginePackageFlow.value.equals(engine.packageName, ignoreCase = true)) {
            applyCurrentEngineInventory(engine.packageName)
        }
        return inventory
    }

    private fun warmRemainingEngineInventories(
        activeEnginePackage: String,
        installedEngines: List<TtsEngineInfo>,
    ) {
        launchInAVDCoroutineScope {
            installedEngines.forEach { engine ->
                if (engine.packageName.equals(activeEnginePackage, ignoreCase = true)) {
                    return@forEach
                }

                repositoryMutex.withLock {
                    if (!engineInventories.containsKey(engine.packageName)) {
                        loadAndStoreEngineInventoryLocked(engine.packageName, installedEngines)
                    }
                }
            }
        }
    }

    private suspend fun ensurePreviewTextToSpeechLocked(): TextToSpeech? {
        ttsForText?.let { return it }

        val enginePackageName = currentEnginePackageFlow.value.takeIf { it.isNotBlank() } ?: return tts
        val previewLocale = selectedVoiceFlow.value?.locale
            ?: currentVoiceFlow.value?.locale
            ?: getCurrentLocale()
        val previewTts = createInitializedTextToSpeech(enginePackageName) ?: return tts

        configurePreviewTts(
            textToSpeech = previewTts,
            enginePackageName = enginePackageName,
            locale = previewLocale,
        )

        selectedVoiceFlow.value?.let { selectedVoice ->
            applyVoiceToInstance(previewTts, selectedVoice, "preview-lazy")
        }

        ttsForText = previewTts
        return previewTts
    }

    private fun applyCurrentEngineInventory(enginePackageName: String) {
        val inventory = engineInventories[enginePackageName] ?: EngineInventory.EMPTY
        availableLanguagesFlow.value = inventory.languages
        availableVoicesFlow.value = inventory.voices
        currentEnginePackageFlow.value = enginePackageName
        currentVoiceFlow.value = tts?.voice
            ?.takeIf { voice -> inventory.voices.any { it.name.equals(voice.name, ignoreCase = true) } }
            ?: inventory.defaultVoice
            ?: inventory.voices.firstOrNull()
        selectedVoiceFlow.value = currentVoiceFlow.value
        selectedLanguageTagFlow.value = currentVoiceFlow.value?.let { voice -> localeTag(voice.locale) }.orEmpty()
    }

    private suspend fun refreshEngineStateLocked(requestedEnginePackage: String? = null) {
        val installedEngines = loadInstalledEngines()
        val savedEnginePackage = normalizeRequestedEnginePackage(preferenceRepository.ttsEnginePackageFlow.first())

        if (installedEngines.isEmpty()) {
            debug("No installed TTS engines detected")
            engineInventories = emptyMap()
            availableEnginesFlow.value = emptyList()
            availableLanguagesFlow.value = emptyList()
            availableVoicesFlow.value = emptyList()
            currentEnginePackageFlow.value = ""
            currentVoiceFlow.value = null
            selectedVoiceFlow.value = null
            selectedLanguageTagFlow.value = ""
            ttsStatusFlow.value = TTSStatus.Uninitialized
            shutdownTextToSpeech(tts)
            shutdownTextToSpeech(ttsForText)
            tts = null
            ttsForText = null
            isPrimeComplete = true
            return
        }

        updateAvailableEnginesState(installedEngines)

        val desiredEnginePackage = resolveSelectedEnginePackage(
            requestedEnginePackage = normalizeRequestedEnginePackage(requestedEnginePackage),
            savedEnginePackage = savedEnginePackage,
            installedEngines = installedEngines,
        )

        if (desiredEnginePackage == null) {
            debug("Could not resolve an active TTS engine")
            return
        }

        currentEnginePackageFlow.value = desiredEnginePackage

        val activeEnginePackage = if (initializeActiveTextToSpeech(desiredEnginePackage, getCurrentLocale())) {
            desiredEnginePackage
        } else {
            val fallbackEnginePackage = resolveSelectedEnginePackage(
                requestedEnginePackage = defaultEnginePackage,
                savedEnginePackage = null,
                installedEngines = installedEngines.filter { engine ->
                    !engine.packageName.equals(desiredEnginePackage, ignoreCase = true)
                },
            )
            if (fallbackEnginePackage != null && initializeActiveTextToSpeech(fallbackEnginePackage, getCurrentLocale())) {
                debug("Falling back to engine=$fallbackEnginePackage after failing to initialize $desiredEnginePackage")
                fallbackEnginePackage
            } else {
                debug("Failed to initialize any active TTS engine")
                ttsStatusFlow.value = TTSStatus.Uninitialized
                return
            }
        }

        preferenceRepository.update(PreferenceRepository.TTS_ENGINE_PACKAGE, activeEnginePackage)
        currentEnginePackageFlow.value = activeEnginePackage
        loadAndStoreEngineInventoryLocked(activeEnginePackage, installedEngines)
        applyCurrentEngineInventory(activeEnginePackage)
        syncSelectedLanguageAndVoice(
            enginePackageName = activeEnginePackage,
            preferredVoiceNamesOverride = preferenceRepository.ttsOrderedVoiceNamesFlow.first(),
        )
        debug(
            "Active TTS engine=$activeEnginePackage languages=${availableLanguagesFlow.value.size} voices=${availableVoicesFlow.value.size}"
        )
        isPrimeComplete = true
    }

    private suspend fun refreshEngineState(requestedEnginePackage: String? = null) {
        repositoryMutex.withLock {
            refreshEngineStateLocked(requestedEnginePackage)
        }
    }

    private suspend fun ensureEngineStateLoadedLocked() {
        if (tts != null && availableEnginesFlow.value.isNotEmpty() && availableLanguagesFlow.value.isNotEmpty()) {
            return
        }
        refreshEngineStateLocked(currentEnginePackageFlow.value.takeIf { it.isNotBlank() })
    }

    fun primeEngineState(delayMs: Long = 0L) {
        if (isPrimeComplete || isPrimeScheduled) {
            return
        }
        isPrimeScheduled = true
        launchInAVDCoroutineScope {
            try {
                if (delayMs > 0L) {
                    delay(delayMs)
                }
                refreshEngineState(currentEnginePackageFlow.value.takeIf { it.isNotBlank() })
            } finally {
                isPrimeScheduled = false
            }
        }
    }

    fun refreshAvailableEngines() {
        launchInAVDCoroutineScope {
            refreshEngineState(currentEnginePackageFlow.value.takeIf { it.isNotBlank() })
        }
    }

    fun stopTTS() {
        launchInAVDCoroutineScope {
            repositoryMutex.withLock {
                val playbackTts = tts ?: return@withLock
                debug("------------ stopTTS --------------")
                withContext(Dispatchers.Main.immediate) {
                    playbackTts.stop()
                }
            }
        }
    }

    private suspend fun clearTTS() {
        repositoryMutex.withLock {
            debug("------------ clearTTS --------------")
            ttsStatusFlow.value = TTSStatus.Uninitialized
            currentVoiceFlow.value = null
            selectedVoiceFlow.value = null
            availableVoicesFlow.value = emptyList()
            availableLanguagesFlow.value = emptyList()
            currentEnginePackageFlow.value = ""
            selectedLanguageTagFlow.value = ""
            shutdownTextToSpeech(tts)
            shutdownTextToSpeech(ttsForText)
            tts = null
            ttsForText = null
        }
    }

    private fun getCurrentLocale(): Locale {
        return context.resources.configuration.locales.get(0)
    }

    fun setEnginePackage(enginePackageName: String?) {
        launchInAVDCoroutineScope {
            debug("setEnginePackage requested=${enginePackageName ?: "default"}")
            refreshEngineState(enginePackageName)
        }
    }

    private fun matchesVoiceLocale(voice: Voice, languageCode: String): Boolean {
        val normalizedLanguageCode = normalizeLanguageCode(languageCode)
        if (normalizedLanguageCode.isBlank() || normalizedLanguageCode.equals("auto", ignoreCase = true)) {
            return false
        }

        val requestedLocale = Locale.forLanguageTag(normalizedLanguageCode)
        val voiceLocale = voice.locale
        val voiceLanguageTag = localeTag(voiceLocale)

        return voiceLanguageTag.equals(normalizedLanguageCode, ignoreCase = true) ||
            (
                voiceLocale.language.equals(requestedLocale.language, ignoreCase = true) &&
                    (
                        requestedLocale.country.isBlank() ||
                            voiceLocale.country.isBlank() ||
                            voiceLocale.country.equals(requestedLocale.country, ignoreCase = true)
                        )
                )
    }

    private fun localeMatchRank(voice: Voice, requestedLocale: Locale): Int {
        val voiceLocale = voice.locale
        val voiceLanguageTag = localeTag(voiceLocale)
        val requestedLanguageTag = localeTag(requestedLocale)

        return when {
            voiceLanguageTag.equals(requestedLanguageTag, ignoreCase = true) -> 0
            voiceLocale.language.equals(requestedLocale.language, ignoreCase = true) &&
                voiceLocale.country.equals(requestedLocale.country, ignoreCase = true) -> 1
            voiceLocale.language.equals(requestedLocale.language, ignoreCase = true) -> 2
            else -> Int.MAX_VALUE
        }
    }

    private fun missingDataRank(voice: Voice): Int {
        return if (voice.features?.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) == true) 1 else 0
    }

    private fun findBestVoice(languageCode: String, preferredVoiceNames: List<String> = emptyList()): Voice? {
        val availableVoices = availableVoicesFlow.value
        if (availableVoices.isEmpty()) {
            return null
        }

        val normalizedLanguageCode = normalizeLanguageCode(languageCode)
        val requestedLocale = Locale.forLanguageTag(normalizedLanguageCode)
        val preferredVoiceRankMap = preferredVoiceNames
            .mapIndexed { index, name -> name.lowercase(Locale.ROOT) to index }
            .toMap()

        return availableVoices
            .asSequence()
            .filter { voice -> matchesVoiceLocale(voice, normalizedLanguageCode) }
            .map { voice ->
                VoiceCandidate(
                    voice = voice,
                    preferredRank = preferredVoiceRankMap[voice.name.lowercase(Locale.ROOT)] ?: Int.MAX_VALUE,
                    localeMatchRank = localeMatchRank(voice, requestedLocale),
                    missingDataRank = missingDataRank(voice),
                    qualityRank = -voice.quality,
                    latencyRank = voice.latency,
                )
            }
            .sortedWith(
                compareBy<VoiceCandidate>(
                    { candidate -> candidate.preferredRank },
                    { candidate -> candidate.localeMatchRank },
                    { candidate -> candidate.missingDataRank },
                    { candidate -> candidate.qualityRank },
                    { candidate -> candidate.latencyRank },
                    { candidate -> candidate.voice.name.lowercase(Locale.ROOT) },
                )
            )
            .map { candidate -> candidate.voice }
            .firstOrNull()
    }

    private suspend fun applyLanguageSelection(
        textToSpeech: TextToSpeech,
        languageCode: String,
        preferredVoiceNames: List<String> = emptyList(),
    ): Voice? {
        val normalizedLanguageCode = normalizeLanguageCode(languageCode)
        if (normalizedLanguageCode.isBlank() || normalizedLanguageCode.equals("auto", ignoreCase = true)) {
            return withContext(Dispatchers.Main.immediate) { textToSpeech.voice }
        }

        val bestVoice = findBestVoice(normalizedLanguageCode, preferredVoiceNames)
        if (bestVoice != null) {
            val languageResult = withContext(Dispatchers.Main.immediate) {
                textToSpeech.setLanguage(bestVoice.locale)
            }
            val voiceResult = withContext(Dispatchers.Main.immediate) {
                textToSpeech.setVoice(bestVoice)
            }
            debug(
                "applyLanguageSelection voice=${bestVoice.name} locale=${bestVoice.locale} setLanguage=$languageResult setVoice=$voiceResult"
            )
            if (voiceResult == TextToSpeech.SUCCESS) {
                currentVoiceFlow.value = bestVoice
                return bestVoice
            }
        }

        val requestedLocale = Locale.forLanguageTag(normalizedLanguageCode)
        val supportLevel = withContext(Dispatchers.Main.immediate) {
            textToSpeech.isLanguageAvailable(requestedLocale)
        }
        val languageResult = withContext(Dispatchers.Main.immediate) {
            textToSpeech.setLanguage(requestedLocale)
        }
        debug(
            "applyLanguageSelection fallback locale=$requestedLocale supportLevel=$supportLevel setLanguage=$languageResult"
        )

        if (languageResult >= TextToSpeech.LANG_AVAILABLE) {
            currentVoiceFlow.value = withContext(Dispatchers.Main.immediate) { textToSpeech.voice } ?: currentVoiceFlow.value
        }

        return withContext(Dispatchers.Main.immediate) { textToSpeech.voice }
    }

    fun setVoice(voiceName: String) {
        launchInAVDCoroutineScope {
            repositoryMutex.withLock {
                ensureEngineStateLoadedLocked()
                val matchingVoice = availableVoicesFlow.value.firstOrNull { voice ->
                    voice.name.equals(voiceName, ignoreCase = true)
                }
                debug("setVoice requested=$voiceName matchingVoice=$matchingVoice")
                if (matchingVoice == null) {
                    return@withLock
                }
                applyVoiceSelectionInternal(matchingVoice, persistSelection = false)
            }
        }
    }

    fun selectLanguage(languageTag: String) {
        launchInAVDCoroutineScope {
            repositoryMutex.withLock {
                ensureEngineStateLoadedLocked()
                val normalizedLanguageTag = normalizeLanguageCode(languageTag)
                if (normalizedLanguageTag.isBlank()) {
                    return@withLock
                }

                val matchingLanguage = availableLanguagesFlow.value.firstOrNull { language ->
                    language.languageTag.equals(normalizedLanguageTag, ignoreCase = true)
                } ?: return@withLock

                val savedVoiceName = normalizeVoiceName(preferenceRepository.ttsVoiceNameFlow.first())
                val preferredVoiceNames = buildPreferredVoiceNames(
                    selectedVoiceName = savedVoiceName,
                    selectedLanguageTag = matchingLanguage.languageTag,
                )

                preferenceRepository.update(PreferenceRepository.TTS_LANGUAGE_TAG, matchingLanguage.languageTag)
                selectedLanguageTagFlow.value = matchingLanguage.languageTag

                val resolvedVoice = findBestVoice(matchingLanguage.languageTag, preferredVoiceNames)
                    ?: availableVoicesFlow.value.firstOrNull { voice ->
                        matchesVoiceLocale(voice, matchingLanguage.languageTag)
                    }

                if (resolvedVoice != null) {
                    applyVoiceSelectionInternal(resolvedVoice, persistSelection = true)
                } else {
                    tts?.let { playbackTts ->
                        applyLanguageSelection(playbackTts, matchingLanguage.languageTag)
                    }
                    ttsForText?.let { previewTts ->
                        applyLanguageSelection(previewTts, matchingLanguage.languageTag)
                    }
                    val fallbackVoice = tts?.voice ?: ttsForText?.voice
                    currentVoiceFlow.value = fallbackVoice
                    selectedVoiceFlow.value = fallbackVoice
                    preferenceRepository.update(PreferenceRepository.TTS_VOICE_NAME, fallbackVoice?.name.orEmpty())
                }
            }
        }
    }

    fun selectVoice(voiceName: String) {
        launchInAVDCoroutineScope {
            repositoryMutex.withLock {
                ensureEngineStateLoadedLocked()
                val matchingVoice = availableVoicesFlow.value.firstOrNull { voice ->
                    voice.name.equals(voiceName, ignoreCase = true)
                } ?: return@withLock
                debug("selectVoice requested=$voiceName matchingVoice=$matchingVoice")
                applyVoiceSelectionInternal(matchingVoice, persistSelection = true)
            }
        }
    }

    fun setLanguage(languageCode: String, preferredVoiceNames: List<String> = emptyList()) {
        launchInAVDCoroutineScope {
            repositoryMutex.withLock {
                ensureEngineStateLoadedLocked()
                val playbackTts = tts ?: run {
                    debug("setLanguage skipped because playback TTS is not initialized")
                    return@withLock
                }
                debug(
                    "setLanguage requested=$languageCode preferredVoiceNames=$preferredVoiceNames engine=${currentEnginePackageFlow.value}"
                )
                try {
                    val selectedVoicePreferredNames = buildPreferredVoiceNamesForLanguage(
                        languageCode = languageCode,
                        requestedPreferredVoiceNames = preferredVoiceNames,
                    )
                    val selectedPlaybackVoice = applyLanguageSelection(
                        playbackTts,
                        languageCode,
                        selectedVoicePreferredNames,
                    )
                    val selectedPreviewVoice = ttsForText?.let { previewTts ->
                        applyLanguageSelection(
                            previewTts,
                            languageCode,
                            selectedVoicePreferredNames,
                        )
                    }
                    val resolvedVoice = selectedPlaybackVoice ?: selectedPreviewVoice
                    if (resolvedVoice != null) {
                        currentVoiceFlow.value = resolvedVoice
                    } else {
                        debug("setLanguage did not select a voice for $languageCode")
                    }
                } catch (e: Exception) {
                    debug("setLanguage failed: ${e.message}")
                }
            }
        }
    }

    fun playTTS(
        text: String,
        speechRate: Float? = 1.0f,
        pitch: Float? = 1.0f,
        languageCode: String? = null,
    ) {
        launchInAVDCoroutineScope {
            repositoryMutex.withLock {
                ensureEngineStateLoadedLocked()
                val playbackTts = tts ?: run {
                    debug("playTTS skipped because playback TTS is not initialized")
                    return@withLock
                }
                if (text.isBlank()) {
                    debug("playTTS skipped because text is blank")
                    return@withLock
                }

                debug(
                    "playTTS textLength=${text.length} languageCode=${languageCode ?: "none"} engine=${currentEnginePackageFlow.value} voice=${currentVoiceFlow.value?.name}"
                )
                val normalizedLanguageCode = languageCode?.takeIf { it.isNotBlank() }
                    ?: selectedLanguageTagFlow.value.takeIf { it.isNotBlank() }
                if (normalizedLanguageCode != null) {
                    val preferredVoiceNames = buildPreferredVoiceNamesForLanguage(normalizedLanguageCode)
                    val resolvedVoice = applyLanguageSelection(
                        playbackTts,
                        normalizedLanguageCode,
                        preferredVoiceNames,
                    )
                    if (resolvedVoice != null) {
                        currentVoiceFlow.value = resolvedVoice
                    }
                }

                val utteranceId = UUID.randomUUID().toString()
                val result = withContext(Dispatchers.Main.immediate) {
                    speechRate?.let { playbackTts.setSpeechRate(it) }
                    pitch?.let { playbackTts.setPitch(it) }
                    playbackTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                }
                debug("playTTS speak result=$result voice=${playbackTts.voice?.name} locale=${playbackTts.voice?.locale}")
                if (result == TextToSpeech.ERROR) {
                    debug("playTTS returned ERROR for engine=${currentEnginePackageFlow.value}")
                }
            }
        }
    }

    fun playTestTTS(
        text: String,
        speechRate: Float? = 1.0f,
        pitch: Float? = 1.0f,
        voice: Voice? = null,
    ) {
        launchInAVDCoroutineScope {
            repositoryMutex.withLock {
                ensureEngineStateLoadedLocked()
                val previewTts = ensurePreviewTextToSpeechLocked() ?: tts ?: run {
                    debug("playTestTTS skipped because no TTS instance is initialized")
                    return@withLock
                }
                if (text.isBlank()) {
                    debug("playTestTTS skipped because text is blank")
                    return@withLock
                }

                debug("playTestTTS textLength=${text.length} requestedVoice=${voice?.name}")
                val utteranceId = UUID.randomUUID().toString()
                val result = withContext(Dispatchers.Main.immediate) {
                    speechRate?.let { previewTts.setSpeechRate(it) }
                    pitch?.let { previewTts.setPitch(it) }
                    voice?.let {
                        previewTts.setLanguage(it.locale)
                        previewTts.setVoice(it)
                    }
                    previewTts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                }
                debug("playTestTTS speak result=$result voice=${previewTts.voice?.name} locale=${previewTts.voice?.locale}")
                if (result == TextToSpeech.ERROR) {
                    debug("playTestTTS returned ERROR voice=${voice?.name} locale=${voice?.locale}")
                }
            }
        }
    }

    override fun onZeroReferences() {
        Timber.tag(TAG).i("onZeroReferences called, cancelling all coroutines and clearing TTS resources")
        runBlocking {
            clearTTS()
        }
    }

    init {
        launchInAVDCoroutineScope {
            hydrateStoredSelectionState()
        }
    }
}
