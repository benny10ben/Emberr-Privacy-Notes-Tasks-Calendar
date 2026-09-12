package com.emberr.domain.util.task

import com.emberr.domain.model.ParsedTask

interface TaskExtractor {
    fun extractTasks(transcript: String): List<ParsedTask>
}