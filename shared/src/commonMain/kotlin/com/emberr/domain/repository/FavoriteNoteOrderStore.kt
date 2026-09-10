package com.emberr.domain.repository

import com.emberr.data.local.prefs.SettingsManager
import com.emberr.domain.model.FavoriteNoteOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

class FavoriteNoteOrderStore(
    private val settingsManager: SettingsManager
) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val orderedNoteIdsFlow: Flow<List<String>> =
        settingsManager.favoriteNoteOrderJsonFlow.map { decodeOrder(it).noteIds }

    fun getOrder(): FavoriteNoteOrder = decodeOrder(settingsManager.getFavoriteNoteOrderJson())

    fun saveOrder(noteIds: List<String>, updatedAt: Long = System.currentTimeMillis()) {
        persistOrder(FavoriteNoteOrder(noteIds = noteIds, updatedAt = updatedAt))
    }

    fun applyRemoteOrder(remoteOrder: FavoriteNoteOrder): Boolean {
        if (remoteOrder.updatedAt <= getOrder().updatedAt) return false
        persistOrder(remoteOrder)
        return true
    }

    private fun persistOrder(order: FavoriteNoteOrder) {
        settingsManager.saveFavoriteNoteOrderJson(json.encodeToString(order))
    }

    private fun decodeOrder(rawJson: String): FavoriteNoteOrder {
        if (rawJson.isBlank()) return FavoriteNoteOrder()
        return try {
            json.decodeFromString<FavoriteNoteOrder>(rawJson)
        } catch (cause: Exception) {
            FavoriteNoteOrder()
        }
    }
}
