package com.ben.emberr.presentation.reminders

interface ReminderScheduler {
    fun schedule(blockId: String, noteTitle: String, text: String, timestamp: Long)
    fun cancel(blockId: String)
}