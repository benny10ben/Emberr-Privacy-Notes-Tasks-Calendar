package com.ben.emberr.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ben.emberr.presentation.mobile.home.note.NoteScreen

@Composable
fun OnboardingEditorPreview(viewModel: OnboardingViewModel, modifier: Modifier = Modifier) {
    val noteId by viewModel.previewNoteId.collectAsState()

    val previewShape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = 24.dp, bottom = 24.dp, end = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(previewShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, onboardingHairlineColor(), previewShape)
        ) {
            val currentNoteId = noteId
            if (currentNoteId != null) {
                NoteScreen(
                    noteId = currentNoteId,
                    onNavigateBack = {},
                    showBackButton = false
                )
            } else {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
