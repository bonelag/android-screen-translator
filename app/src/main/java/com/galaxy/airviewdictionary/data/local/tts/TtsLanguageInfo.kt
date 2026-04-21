package com.galaxy.airviewdictionary.data.local.tts

import java.util.Locale

data class TtsLanguageInfo(
    val locale: Locale,
    val languageTag: String,
    val displayName: String,
    val supportLevel: Int,
    val voiceCount: Int,
)
