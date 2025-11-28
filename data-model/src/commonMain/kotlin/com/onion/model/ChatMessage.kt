package com.onion.model

data class ChatMessage(
        val message: String,
        val isUser: Boolean,
        val image: ByteArray? = null
)