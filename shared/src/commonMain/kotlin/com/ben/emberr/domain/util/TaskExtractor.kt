package com.ben.emberr.domain.util

import com.ben.emberr.domain.model.ParsedTask

interface TaskExtractor {
    fun extractTasks(transcript: String): List<ParsedTask>
}