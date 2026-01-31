package com.mobileagent.policy.model

import kotlinx.serialization.Serializable

/**
 * Represents a complete policy document.
 */
@Serializable
data class Policy(
    val version: Int = 1,
    val default: PolicyEffect = PolicyEffect.DENY,
    val rules: List<PolicyRule> = emptyList()
)

/**
 * Represents a single policy rule.
 */
@Serializable
data class PolicyRule(
    val id: String,
    val effect: PolicyEffect,
    val match: MatchCriteria,
    val constraints: CapabilityConstraints? = null,
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Policy effect: allow or deny.
 */
@Serializable
enum class PolicyEffect {
    @kotlinx.serialization.SerialName("allow")
    ALLOW,

    @kotlinx.serialization.SerialName("deny")
    DENY
}

/**
 * Criteria for matching a skill execution request.
 */
@Serializable
data class MatchCriteria(
    val skillId: String? = null,
    val skillIdPattern: String? = null,
    val skillVersion: String? = null
) {
    fun matches(skillId: String, skillVersion: String): Boolean {
        val idMatches = when {
            this.skillId != null -> this.skillId == skillId
            skillIdPattern != null -> skillId.matches(Regex(skillIdPattern))
            else -> true
        }

        val versionMatches = this.skillVersion == null || this.skillVersion == skillVersion

        return idMatches && versionMatches
    }
}

/**
 * Constraints on capabilities that can be granted.
 */
@Serializable
data class CapabilityConstraints(
    val network: NetworkConstraints? = null,
    val readCalendar: Boolean? = null,
    val readNotifications: NotificationConstraints? = null,
    val writeLocalStorage: Boolean? = null
)

@Serializable
data class NetworkConstraints(
    val hosts: List<String> = emptyList(),
    val methods: List<String> = emptyList()
)

@Serializable
data class NotificationConstraints(
    val apps: List<String> = emptyList()
)
