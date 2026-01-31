package com.mobileagent.sample.skills

import com.mobileagent.policy.model.Capability
import com.mobileagent.policy.model.Skill
import com.mobileagent.policy.model.SkillContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * Skill that calls a webhook endpoint.
 */
class WebhookCallSkill : Skill<WebhookInput, WebhookResult> {
    override val id: String = "webhook.call"
    override val version: String = "1.0.0"
    override val description: String = "Call a webhook endpoint"
    override val requiredCapabilities: Set<Capability> = setOf(
        Capability.Network()
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    override suspend fun execute(input: WebhookInput, context: SkillContext): WebhookResult {
        // Validate that the network capability allows this host
        val networkCap = context.allowedCapabilities
            .filterIsInstance<Capability.Network>()
            .firstOrNull()

        if (networkCap != null) {
            val host = extractHost(input.url)
            if (!networkCap.allowsHost(host)) {
                return WebhookResult(
                    success = false,
                    statusCode = 0,
                    error = "Host $host not allowed by policy"
                )
            }

            if (!networkCap.allowsMethod(input.method)) {
                return WebhookResult(
                    success = false,
                    statusCode = 0,
                    error = "Method ${input.method} not allowed by policy"
                )
            }
        }

        return withContext(Dispatchers.IO) {
            try {
                val requestBody = input.payload.toRequestBody("application/json".toMediaType())
                val request = Request.Builder()
                    .url(input.url)
                    .method(input.method, if (input.method != "GET") requestBody else null)
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string() ?: ""

                WebhookResult(
                    success = response.isSuccessful,
                    statusCode = response.code,
                    responseBody = responseBody.take(500) // Limit response size
                )
            } catch (e: Exception) {
                WebhookResult(
                    success = false,
                    statusCode = 0,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }

    private fun extractHost(url: String): String {
        return try {
            val uri = java.net.URI(url)
            uri.host ?: url
        } catch (e: Exception) {
            url
        }
    }
}

data class WebhookInput(
    val url: String,
    val method: String = "POST",
    val payload: String = "{}"
)

data class WebhookResult(
    val success: Boolean,
    val statusCode: Int,
    val responseBody: String = "",
    val error: String? = null
)
