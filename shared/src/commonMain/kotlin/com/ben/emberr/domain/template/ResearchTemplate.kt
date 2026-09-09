package com.ben.emberr.domain.template

import com.ben.emberr.domain.model.NoteContent

/** Default "Research" starter template. Ships with empty content for now. */
object ResearchTemplate : PredefinedTemplate {
    override val templateId: String = "predefined_template_research"
    override val title: String = "Research"
    override val icon: String? = null
    override fun buildContent(): NoteContent = NoteContent(blocks = emptyList())
}
