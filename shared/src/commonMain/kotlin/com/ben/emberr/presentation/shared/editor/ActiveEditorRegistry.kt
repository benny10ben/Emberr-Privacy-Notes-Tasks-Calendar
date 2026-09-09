package com.ben.emberr.presentation.shared.editor

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

object ActiveEditorRegistry {

    private val activeEditors = MutableStateFlow<Set<BaseEditorViewModel>>(emptySet())

    fun register(viewModel: BaseEditorViewModel) {
        activeEditors.update { it + viewModel }
    }

    fun unregister(viewModel: BaseEditorViewModel) {
        activeEditors.update { it - viewModel }
    }

    fun discardAllPendingWrites() {
        activeEditors.value.forEach { editor -> editor.discardPendingWrites() }
    }

    suspend fun flushAllPending() {
        activeEditors.value.forEach { editor ->
            try {
                editor.flushPendingSave()
            } catch (cause: Exception) {
                cause.printStackTrace()
            }
        }
    }
}
