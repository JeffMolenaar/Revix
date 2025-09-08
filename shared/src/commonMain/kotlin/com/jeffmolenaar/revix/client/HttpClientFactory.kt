package com.jeffmolenaar.revix.client

import io.ktor.client.*

/**
 * Platform-specific HTTP client factory
 */
internal expect fun createPlatformHttpClient(config: RevixClientConfig): HttpClient