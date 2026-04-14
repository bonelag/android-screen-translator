package com.galaxy.airviewdictionary.data.remote.ai.chatgpt

import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface ChatGPTService {

    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun send(
        @Header("Authorization") apiKey: String,
        @Header("OpenAI-Organization") organizationId: String,  // Service Account ID 사용
        @Body body: RequestBody
    ): ChatGPTResponse
}

data class ChatGPTResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

data class Message(
    val role: String,
    val content: String
)


