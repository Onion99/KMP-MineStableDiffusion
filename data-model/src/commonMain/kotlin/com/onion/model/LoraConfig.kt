package com.onion.model

import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class LoraConfig(
    val id: String = Random.nextLong().toString(),
    val path: String,
    val name: String,
    var strength: Float = 1.0f,
    var isEnabled: Boolean = true
)
