package com.ben.emberr.domain.template

import com.ben.emberr.domain.model.NoteContent

/** Default "Projects" starter template. Ships with empty content for now. */
object ProjectsTemplate : PredefinedTemplate {
    override val templateId: String = "predefined_template_projects"
    override val title: String = "Projects"
    override val icon: String? = null
    override fun buildContent(): NoteContent = NoteContent(blocks = emptyList())
}
