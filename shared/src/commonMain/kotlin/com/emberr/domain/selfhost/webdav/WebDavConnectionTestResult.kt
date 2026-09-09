package com.emberr.domain.selfhost.webdav

sealed class WebDavConnectionTestResult {
    data object Success : WebDavConnectionTestResult()
    data object InvalidCredentials : WebDavConnectionTestResult()
    data class ServerError(val statusCode: Int) : WebDavConnectionTestResult()
    data class NetworkFailure(val cause: Throwable) : WebDavConnectionTestResult()
    data class InvalidConfiguration(val message: String) : WebDavConnectionTestResult()
}