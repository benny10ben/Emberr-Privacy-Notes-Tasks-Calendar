// Test double for VaultNoteImporter that records the last file it was asked to import.

package com.emberr.domain.ai.tools

import com.emberr.domain.vault.VaultImportOutcome
import com.emberr.domain.vault.VaultImportReport
import com.emberr.domain.vault.VaultNoteImporter
import java.io.File

internal class FakeVaultNoteImporter(
    private val outcome: VaultImportOutcome = VaultImportOutcome.IMPORTED
) : VaultNoteImporter {

    var lastImportedFile: File? = null
        private set

    override suspend fun importFile(file: File): VaultImportReport {
        lastImportedFile = file
        return VaultImportReport(outcome, file.nameWithoutExtension)
    }
}
