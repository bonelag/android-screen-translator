package com.galaxy.airviewdictionary.data.local.tts

data class TtsEngineInfo(
    val packageName: String,
    val label: String,
    val isDefault: Boolean = false,
    val languageCount: Int = 0,
    val voiceCount: Int = 0,
)
