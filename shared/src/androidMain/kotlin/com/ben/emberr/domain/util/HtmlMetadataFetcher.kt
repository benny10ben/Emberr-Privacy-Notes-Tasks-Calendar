package com.ben.emberr.domain.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

actual object HtmlMetadataFetcher {
    actual suspend fun fetchMetadata(url: String): UrlMetadata = withContext(Dispatchers.IO) {
        val urlRegex = "(https?://[^\\s]+)".toRegex()
        val extractedUrl = urlRegex.find(url)?.value ?: url.trim()

        val validUrl = if (!extractedUrl.startsWith("http://") && !extractedUrl.startsWith("https://")) {
            "https://$extractedUrl"
        } else {
            extractedUrl
        }

        try {
            val document = Jsoup.connect(validUrl)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "max-age=0")
                .header("Sec-Ch-Ua", "\"Chromium\";v=\"124\", \"Google Chrome\";v=\"124\", \"Not-A.Brand\";v=\"99\"")
                .header("Sec-Ch-Ua-Mobile", "?0")
                .header("Sec-Ch-Ua-Platform", "\"Windows\"")
                .header("Upgrade-Insecure-Requests", "1")
                .ignoreHttpErrors(true)
                .ignoreContentType(true)
                .timeout(10000)
                .followRedirects(true)
                .get()

            val title = document.title().takeIf { it.isNotBlank() }
                ?: document.select("meta[property=og:title], meta[name=og:title]").attr("content").takeIf { it.isNotBlank() }

            val description = document.select("meta[name=description]").attr("content").takeIf { it.isNotBlank() }
                ?: document.select("meta[property=og:description], meta[name=og:description]").attr("content").takeIf { it.isNotBlank() }

            var imageUrl: String? = document.select("meta[property=og:image], meta[name=og:image]").attr("abs:content").takeIf { it.isNotBlank() }
                ?: document.select("meta[name=twitter:image], meta[name=twitter:image:src]").attr("abs:content").takeIf { it.isNotBlank() }
                ?: document.select("meta[itemprop=image]").attr("abs:content").takeIf { it.isNotBlank() }
                ?: document.select("link[rel=image_src]").attr("abs:href").takeIf { it.isNotBlank() }

            if (imageUrl?.startsWith("http://") == true) {
                imageUrl = imageUrl.replace("http://", "https://")
            }

            UrlMetadata(title = title, description = description, imageUrl = imageUrl)

        } catch (e: Exception) {
            UrlMetadata(
                title = url,
                description = "Could not load preview",
                imageUrl = null
            )
        }
    }
}