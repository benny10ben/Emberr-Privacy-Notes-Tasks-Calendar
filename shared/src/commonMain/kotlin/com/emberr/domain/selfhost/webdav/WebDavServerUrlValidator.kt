package com.emberr.domain.selfhost.webdav

import com.emberr.domain.util.LocalNetworkHostValidator
import io.ktor.http.Url

object WebDavServerUrlValidator {

    fun isLocalNetworkHost(host: String): Boolean = LocalNetworkHostValidator.isLocalNetworkHost(host)

    fun validate(serverUrl: String) {
        val url = try {
            Url(serverUrl)
        } catch (_: Exception) {
            throw WebDavConfigurationException("Server URL '$serverUrl' is not a valid URL")
        }

        val isHttps = url.protocol.name.equals("https", ignoreCase = true)
        val isPlainHttpOnLocalNetwork = url.protocol.name.equals("http", ignoreCase = true) &&
            isLocalNetworkHost(url.host)

        if (!isHttps && !isPlainHttpOnLocalNetwork) {
            throw WebDavConfigurationException(
                "Server URL '$serverUrl' must use https:// — plain http:// is only permitted for " +
                        "local network addresses such as localhost, 127.0.0.1, or 192.168.x.x"
            )
        }
    }
}