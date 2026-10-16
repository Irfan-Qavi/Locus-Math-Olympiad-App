package com.locus.olympiad.network

import com.locus.olympiad.network.model.ChatCompletionRequest
import com.locus.olympiad.network.model.ChatCompletionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Retrofit API definition for standard LLM chat completion endpoints.
 */
interface AiTutorApiService {

    @POST("v1/chat/completions")
    suspend fun getChatCompletion(
        @Body request: ChatCompletionRequest
    ): Response<ChatCompletionResponse>
}
