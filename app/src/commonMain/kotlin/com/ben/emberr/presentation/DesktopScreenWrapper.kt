package com.ben.emberr.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.ben.emberr.domain.model.NoteBlock
import com.ben.emberr.presentation.rag.RagViewModel

@Composable
expect fun DesktopMainScreenWrapper(
    isSidebarVisible: Boolean,
    sidebarWidth: Dp,
    onToggleSidebar: () -> Unit,
    onSelectionModeChange: (Boolean) -> Unit,
    onPickImage: ((String) -> Unit) -> Unit,
    onTakePhoto: ((String) -> Unit) -> Unit,
    onPickDocument: ((String) -> Unit) -> Unit,
    onOpenFile: (String, String) -> Unit,
    onExportMarkdown: (String, String) -> Unit,
    onExportPdf: (String, String, List<NoteBlock>) -> Unit,
    onExportBackup: () -> Unit,
    onImportBackupClick: () -> Unit,
    onAiIconTap: () -> Unit,
    isRagChatVisible: Boolean = false,
    ragViewModel: RagViewModel? = null,
    onDismissRagChat: () -> Unit = {}
)