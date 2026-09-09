package com.ben.emberr.domain.model

sealed class PendingShare {
    data class Link(val url: String) : PendingShare()
    data class Image(val uriString: String) : PendingShare()
    data class Document(val uriString: String, val mimeType: String, val fileName: String) : PendingShare()
    data class Multiple(val items: List<PendingShare>) : PendingShare()
}
