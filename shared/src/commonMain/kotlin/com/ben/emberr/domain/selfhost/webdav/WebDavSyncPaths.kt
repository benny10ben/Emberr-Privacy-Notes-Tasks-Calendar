package com.ben.emberr.domain.selfhost.webdav

object WebDavSyncPaths {
    const val ROOT = "/emberr_sync"
    const val SALT_FILE = "$ROOT/salt.txt"
    const val MANIFEST_FILE = "$ROOT/manifest.json"
    const val NOTES_DIR = "$ROOT/notes"
    const val DAILY_DIR = "$ROOT/daily"
    const val MEDIA_DIR = "$ROOT/media"
    const val CHAT_SESSIONS_DIR = "$ROOT/chat_sessions"
    const val FOLDERS_FILE = "$ROOT/folders.json"
    const val TAGS_FILE = "$ROOT/tags.json"
    const val CATEGORIES_FILE = "$ROOT/categories.json"
    const val API_CONFIGS_FILE = "$ROOT/api_configs.json"

    fun notePath(noteId: String) = "$NOTES_DIR/note_$noteId.enc"
    fun dailyPath(dateString: String) = "$DAILY_DIR/daily_$dateString.enc"
    fun mediaPath(mediaId: String) = "$MEDIA_DIR/img_$mediaId.enc"
    fun chatSessionPath(sessionId: String) = "$CHAT_SESSIONS_DIR/chat_$sessionId.enc"
}