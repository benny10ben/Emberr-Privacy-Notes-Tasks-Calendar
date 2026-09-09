package com.ben.emberr.domain.util

interface AudioRecorder {
    fun startRecording()
    fun stopRecording(cancel: Boolean = false): Pair<String, Int>?
    fun play(fileName: String, onCompletion: () -> Unit)
    fun stopPlaying()
}