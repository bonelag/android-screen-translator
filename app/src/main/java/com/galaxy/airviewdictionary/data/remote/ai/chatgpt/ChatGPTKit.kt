package com.galaxy.airviewdictionary.data.remote.ai.chatgpt

import android.content.Context
import com.galaxy.airviewdictionary.data.local.secure.ApiKeyInfo
import com.galaxy.airviewdictionary.data.remote.ai.CorrectionKit
import com.galaxy.airviewdictionary.di.ChatGPTRetrofit
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ChatGPTKit @Inject constructor(@ApplicationContext val context: Context, @ChatGPTRetrofit private val chatGPTService: ChatGPTService) : CorrectionKit() {

    override fun available(): Boolean {
        return ApiKeyInfo.apiKeyAvailable(context)
    }

    private fun getSystemMessage(languageCode: String): String {
        return when (languageCode) {
            "en" -> "Optimize English text by expanding contractions and fixing slang, informal words, and incomplete sentences."
            "zh" -> "Optimize Chinese text by replacing internet slang and abbreviations with standard expressions and ensuring proper sentence structure."
            "es" -> "Optimize Spanish text by expanding abbreviations, replacing informal phrases, and fixing incomplete sentences."
            "hi" -> "Optimize Hindi text by expanding informal contractions, fixing slang, and ensuring proper sentence structure."
            "ar" -> "Optimize Arabic text by replacing dialect slang with Modern Standard Arabic and fixing incomplete sentences."
            "pt" -> "Optimize Portuguese text by expanding contractions, replacing informal words, and fixing incomplete or fragmented sentences."
            "bn" -> "Optimize Bengali text by expanding contractions, fixing informal expressions, and ensuring clear sentence structure."
            "ru" -> "Optimize Russian text by expanding informal phrases, replacing slang, and fixing incomplete or ambiguous sentences."
            "ja" -> "Optimize Japanese text by replacing casual phrases, internet slang, and ambiguous terms with formal equivalents."
            "ko" -> "Optimize Korean text by expanding contractions, fixing informal phrases, and correcting fragmented or ambiguous sentences."
            "fr" -> "Optimize French text by expanding contractions, replacing slang, and fixing incomplete or casual phrases."
            "de" -> "Optimize German text by expanding abbreviations, fixing informal phrases, and ensuring complete sentence structure."
            "it" -> "Optimize Italian text by expanding contractions, replacing informal phrases, and fixing incomplete or fragmented sentences."
            "tr" -> "Optimize Turkish text by expanding contractions, replacing informal words, and fixing fragmented sentences."
            "vi" -> "Optimize Vietnamese text by replacing internet slang, expanding abbreviations, and ensuring sentence clarity."
            "ta" -> "Optimize Tamil text by expanding contractions, fixing informal words, and correcting sentence structure."
            "ur" -> "Optimize Urdu text by expanding contractions, fixing slang, and ensuring formal sentence structure."
            "fa" -> "Optimize Persian text by replacing slang, expanding informal contractions, and ensuring complete and proper sentences."
            "nl" -> "Optimize Dutch text by expanding abbreviations, replacing slang, and fixing incomplete or fragmented sentences."
            "th" -> "Optimize Thai text by replacing slang, expanding informal phrases, and fixing ambiguous or incomplete sentences."
            else -> "Optimize text by expanding contractions and fixing slang, informal words, and incomplete sentences."
        }
    }

    private val organizationId = "org-HdV72AmAarbyJLig4SZEIixq"

    override suspend fun request(
        sourceLanguageCode: String,
        sourceText: String,
    ): String {
        // 요청 메시지 구성
        val systemMessage = mapOf(
            "role" to "system",
            "content" to getSystemMessage(sourceLanguageCode)
        )

        val userMessage = mapOf(
            "role" to "user",
            "content" to """{"sentence": "$sourceText"}""".trimIndent()
        )

        // JSON 요청 본문 생성
        val requestBody = mapOf(
            "model" to "gpt-3.5-turbo",
            "messages" to listOf(systemMessage, userMessage),
            "max_tokens" to 500
        )
        Timber.tag(TAG).d("requestBody : $requestBody")

        val jsonRequestBody = Gson().toJson(requestBody)
            .toRequestBody("application/json".toMediaType())

        // API 호출
        return try {
            val response: ChatGPTResponse = chatGPTService.send(
                apiKey = "Bearer ${ApiKeyInfo.getApiKeyChatgpt(context) ?: "unknown_key"}",
                organizationId = organizationId,
                body = jsonRequestBody
            )

            /*
                {
                  "id": "chatcmpl-AzLBT2DeI07xN0MykumYlB63SOitc",
                  "object": "chat.completion",
                  "created": 1739182803,
                  "model": "gpt-3.5-turbo-0125",
                  "choices": [
                    {
                      "index": 0,
                      "message": {
                        "role": "assistant",
                        "content": "Provide haptic feedback for detection.",
                        "refusal": null
                      },
                      "logprobs": null,
                      "finish_reason": "stop"
                    }
                  ],
                  "usage": {
                    "prompt_tokens": 44,
                    "completion_tokens": 8,
                    "total_tokens": 52,
                    "prompt_tokens_details": {
                      "cached_tokens": 0,
                      "audio_tokens": 0
                    },
                    "completion_tokens_details": {
                      "reasoning_tokens": 0,
                      "audio_tokens": 0,
                      "accepted_prediction_tokens": 0,
                      "rejected_prediction_tokens": 0
                    }
                  },
                  "service_tier": "default",
                  "system_fingerprint": null
                }
             */
            val resultText = response.choices[0].message.content
            Timber.tag("TargetHandleViewModel").i("ChatGPTKit resultText : $resultText")
            val splitResult = resultText.split("->")
            return when (splitResult.size) {
                2 -> splitResult[1].trim().removeSurrounding("\"")
                else -> splitResult[0].trim().removeSurrounding("\"")
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d("Error : ${e.message}")
            sourceText
        }
    }

    companion object {
        const val BASE_URL = "https://api.openai.com/"
    }
}

