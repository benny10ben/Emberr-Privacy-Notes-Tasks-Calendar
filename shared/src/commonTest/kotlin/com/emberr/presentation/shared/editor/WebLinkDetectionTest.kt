package com.emberr.presentation.shared.editor

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WebLinkDetectionTest {

    private fun detectedUrlsIn(text: String) = findWebLinks(text).map { it.url }

    @Test
    fun aPastedLinkWithASchemeIsDetected() {
        assertEquals(
            listOf("https://emberr.app/docs?page=2"),
            detectedUrlsIn("read https://emberr.app/docs?page=2 later")
        )
    }

    @Test
    fun linksWithoutASchemeAreDetected() {
        assertEquals(listOf("www.google.com"), detectedUrlsIn("open www.google.com now"))
        assertEquals(listOf("notion.so/page"), detectedUrlsIn("open notion.so/page now"))
    }

    @Test
    fun severalLinksInOneBlockAreAllDetected() {
        assertEquals(
            listOf("https://a.io", "https://b.dev"),
            detectedUrlsIn("compare https://a.io and https://b.dev")
        )
    }

    @Test
    fun sentencePunctuationIsNotPartOfTheLink() {
        assertEquals(listOf("https://emberr.app"), detectedUrlsIn("done at https://emberr.app."))
        assertEquals(listOf("https://emberr.app"), detectedUrlsIn("see (https://emberr.app), thanks"))
    }

    @Test
    fun everydayTextIsNotMistakenForALink() {
        assertTrue(detectedUrlsIn("fix the bug.Comment below").isEmpty())
        assertTrue(detectedUrlsIn("that release was great.Company wide").isEmpty())
        assertTrue(detectedUrlsIn("open note.md and check v1.2").isEmpty())
        assertTrue(detectedUrlsIn("meeting at 3.30 pm").isEmpty())
    }

    @Test
    fun emailAddressesAreNotTreatedAsLinks() {
        assertTrue(detectedUrlsIn("write to ben@northeastern.edu today").isEmpty())
    }

    @Test
    fun noteMentionMarkdownIsNotTreatedAsALink() {
        assertTrue(detectedUrlsIn("[Roadmap](emberr://note/note-1)").none { it.contains("emberr://note") })
    }

    @Test
    fun linkPositionsPointAtTheLinkInsideTheText() {
        val text = "go to emberr.app now"
        val link = findWebLinks(text).single()
        assertEquals("emberr.app", text.substring(link.start, link.end))
    }

    @Test
    fun aMissingSchemeDefaultsToHttps() {
        assertEquals("https://emberr.app", toOpenableWebLink("emberr.app"))
        assertEquals("http://localhost:8080", toOpenableWebLink("http://localhost:8080"))
        assertEquals("https://emberr.app", toOpenableWebLink("https://emberr.app"))
    }
}
