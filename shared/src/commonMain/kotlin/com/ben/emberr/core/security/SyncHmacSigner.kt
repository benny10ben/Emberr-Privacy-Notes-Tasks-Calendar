package com.ben.emberr.core.security

// Multiplatform contract for computing the HMAC-SHA256 signature that authenticates sync requests
interface SyncHmacSigner {
    fun sign(path: String, timestampMillis: Long, secretKey: String): String
}
