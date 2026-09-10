package com.emberr.domain.repository

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.model.BookmarkCategoryOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class BookmarkCategoryOrderStore(
    private val settingsManager: SettingsManager
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val orderFlow: Flow<BookmarkCategoryOrder> =
        settingsManager.bookmarkCategoryOrderJsonFlow.map { decodeOrder(it) }

    fun getOrder(): BookmarkCategoryOrder = decodeOrder(settingsManager.getBookmarkCategoryOrderJson())

    fun saveOrder(categories: List<String>, updatedAt: Long = System.currentTimeMillis()) {
        persistOrder(BookmarkCategoryOrder(categories = categories, updatedAt = updatedAt))
    }

    fun applyRemoteOrder(remoteOrder: BookmarkCategoryOrder): Boolean {
        if (remoteOrder.updatedAt <= getOrder().updatedAt) return false
        persistOrder(remoteOrder)
        return true
    }

    private fun persistOrder(order: BookmarkCategoryOrder) {
        settingsManager.saveBookmarkCategoryOrderJson(json.encodeToString(order))
    }

    private fun decodeOrder(rawJson: String): BookmarkCategoryOrder {
        if (rawJson.isBlank()) return BookmarkCategoryOrder()
        return try {
            json.decodeFromString<BookmarkCategoryOrder>(rawJson)
        } catch (cause: Exception) {
            BookmarkCategoryOrder()
        }
    }
}
