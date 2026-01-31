package com.mobileagent.policy.model

/**
 * Represents a policy decision for a skill execution request.
 */
data class Decision(
    val allowed: Boolean,
    val reason: String,
    val matchedRuleIds: List<String> = emptyList(),
    val deniedCapabilities: List<Capability> = emptyList(),
    val allowedCapabilities: Set<Capability> = emptySet()
) {
    companion object {
        fun deny(reason: String, deniedCapabilities: List<Capability> = emptyList()): Decision {
            return Decision(
                allowed = false,
                reason = reason,
                deniedCapabilities = deniedCapabilities
            )
        }

        fun allow(reason: String, matchedRuleIds: List<String>, allowedCapabilities: Set<Capability>): Decision {
            return Decision(
                allowed = true,
                reason = reason,
                matchedRuleIds = matchedRuleIds,
                allowedCapabilities = allowedCapabilities
            )
        }
    }
}
