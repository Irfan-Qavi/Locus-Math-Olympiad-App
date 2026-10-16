package com.locus.olympiad.network.model

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    @SerializedName("model")
    val model: String = "gpt-4o-mini",

    @SerializedName("messages")
    val messages: List<ChatMessage>,

    @SerializedName("temperature")
    val temperature: Double = 0.2,

    @SerializedName("max_tokens")
    val maxTokens: Int = 150
)

data class ChatMessage(
    @SerializedName("role")
    val role: String, // "system" or "user"

    @SerializedName("content")
    val content: String
)

data class ChatCompletionResponse(
    @SerializedName("id")
    val id: String?,

    @SerializedName("choices")
    val choices: List<ChatChoice>?
)

data class ChatChoice(
    @SerializedName("index")
    val index: Int,

    @SerializedName("message")
    val message: ChatMessage?
)
