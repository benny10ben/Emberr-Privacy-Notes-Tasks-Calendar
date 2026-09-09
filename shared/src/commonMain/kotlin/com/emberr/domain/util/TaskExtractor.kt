package com.emberr.domain.util

import com.emberr.domain.model.ParsedTask

interface TaskExtractor {
    fun extractTasks(transcript: String): List<ParsedTask>
}