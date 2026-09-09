package com.ben.emberr.domain.ai.chat

import com.ben.emberr.data.local.room.ChatSessionDao
import com.ben.emberr.data.local.room.ChatSessionEntity
import com.ben.emberr.domain.sync.AutoSyncTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class ChatSessionRepositoryImpl(
    private val chatSessionDao: ChatSessionDao
) : ChatSessionRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val messageListSerializer = ListSerializer(ChatMessage.serializer())

    override fun getAllSessions(): Flow<List<ChatSession>> =
        chatSessionDao.getAllSessions().map { entities -> entities.map { it.toDomain() } }

    override suspend fun getSession(sessionId: String): ChatSession? =
        withContext(Dispatchers.IO) { chatSessionDao.getSession(sessionId)?.toDomain() }

    override suspend fun saveSession(session: ChatSession) {
        withContext(Dispatchers.IO) {
            chatSessionDao.upsertSession(
                ChatSessionEntity(
                    id = session.id,
                    title = session.title,
                    messagesJson = json.encodeToString(messageListSerializer, session.messages),
                    createdAt = session.createdAt,
                    updatedAt = session.updatedAt
                )
            )
        }
        AutoSyncTrigger.requestSync()
    }

    override suspend fun deleteSession(sessionId: String) {
        withContext(Dispatchers.IO) { chatSessionDao.softDeleteSession(sessionId, System.currentTimeMillis()) }
        AutoSyncTrigger.requestSync()
    }

    override suspend fun renameSession(sessionId: String, title: String) {
        withContext(Dispatchers.IO) { chatSessionDao.renameSession(sessionId, title, System.currentTimeMillis()) }
        AutoSyncTrigger.requestSync()
    }

    private fun ChatSessionEntity.toDomain(): ChatSession = ChatSession(
        id = id,
        title = title,
        messages = try {
            json.decodeFromString(messageListSerializer, messagesJson)
        } catch (cause: SerializationException) {
            emptyList()
        },
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}
