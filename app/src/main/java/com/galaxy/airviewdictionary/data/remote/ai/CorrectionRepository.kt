package com.galaxy.airviewdictionary.data.remote.ai

import com.galaxy.airviewdictionary.data.AVDRepository
import com.galaxy.airviewdictionary.data.remote.ai.chatgpt.ChatGPTKit
import javax.inject.Inject
import javax.inject.Singleton


/**
 *
 */
@Singleton
class CorrectionRepository @Inject constructor(
    private val chatGPTKit: ChatGPTKit,
) : AVDRepository() {

    private fun getCorrectionKit(kitType: CorrectionKitType): CorrectionKit {
        return when (kitType) {
            CorrectionKitType.CHAT_GPT -> chatGPTKit
        }
    }

    suspend fun request(
        sourceLanguageCode: String,
        sourceText: String,
        correctionKitType: CorrectionKitType,
    ): String {
        val correctionKit: CorrectionKit = getCorrectionKit(correctionKitType)

        return correctionKit.request(
            sourceLanguageCode = sourceLanguageCode,
            sourceText = sourceText,
        )
    }

    override fun onZeroReferences() {

    }
}
