package com.ben.emberr.domain.repository

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class EmojiCategoryDto(
    val name: String,
    val slug: String? = null,
    val emojis: List<EmojiDto> = emptyList()
)

@Serializable
data class EmojiDto(
    val emoji: String,
    val name: String,
    val slug: String? = null,
    val skin_tone_support: Boolean? = null,
    val unicode_version: String? = null,
    val emoji_version: String? = null
)

/**
 * Handles parsing the massive JSON file off the main UI thread.
 * Holds the parsed data in memory so it's instantaneous after the first load.
 */
object EmojiRepository {
    var isLoaded by mutableStateOf(false)
        private set

    var categories by mutableStateOf<List<String>>(emptyList())
        private set
    var flatList by mutableStateOf<List<Pair<String, String>>>(emptyList())
        private set
    var categoryEmojiLists by mutableStateOf<List<List<String>>>(emptyList())
        private set

    // Ignore extra fields in the JSON to prevent crashes
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun initialize(jsonString: String) {
        if (isLoaded) return
        withContext(Dispatchers.Default) {
            try {
                val data = json.decodeFromString<List<EmojiCategoryDto>>(jsonString)

                categories = data.map { it.name }
                categoryEmojiLists = data.map { cat -> cat.emojis.map { it.emoji } }
                // Flatten to (emoji -> name) pairs for ultra-fast searching
                flatList = data.flatMap { cat -> cat.emojis.map { it.emoji to it.name } }

                isLoaded = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}