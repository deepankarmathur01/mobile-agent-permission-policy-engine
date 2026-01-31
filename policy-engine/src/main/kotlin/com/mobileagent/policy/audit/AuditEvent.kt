package com.mobileagent.policy.audit

import com.mobileagent.policy.model.Capability
import com.mobileagent.policy.model.Decision
import kotlinx.serialization.Serializable

/**
 * Represents an audit event for a skill execution.
 */
@Serializable
data class AuditEvent(
    val timestamp: Long,
    val skillId: String,
    val skillVersion: String,
    val decision: DecisionRecord,
    val requestedCapabilities: List<String>,
    val inputMetadata: InputMetadata,
    val execution: ExecutionRecord?
) {
    companion object {
        fun create(
            skillId: String,
            skillVersion: String,
            decision: Decision,
            requestedCapabilities: Set<Capability>,
            inputSize: Int,
            inputType: String
        ): AuditEvent {
            return AuditEvent(
                timestamp = System.currentTimeMillis(),
                skillId = skillId,
                skillVersion = skillVersion,
                decision = DecisionRecord(
                    allowed = decision.allowed,
                    reason = decision.reason,
                    matchedRuleIds = decision.matchedRuleIds,
                    deniedCapabilities = decision.deniedCapabilities.map { it.toString() }
                ),
                requestedCapabilities = requestedCapabilities.map { it.toString() },
                inputMetadata = InputMetadata(
                    size = inputSize,
                    type = inputType
                ),
                execution = null
            )
        }
    }

    fun withExecution(started: Long, ended: Long, success: Boolean, error: String? = null): AuditEvent {
        return copy(
            execution = ExecutionRecord(
                started = started,
                ended = ended,
                durationMs = ended - started,
                success = success,
                error = error
            )
        )
    }
}

@Serializable
data class DecisionRecord(
    val allowed: Boolean,
    val reason: String,
    val matchedRuleIds: List<String>,
    val deniedCapabilities: List<String>
)

@Serializable
data class InputMetadata(
    val size: Int,
    val type: String
)

@Serializable
data class ExecutionRecord(
    val started: Long,
    val ended: Long,
    val durationMs: Long,
    val success: Boolean,
    val error: String? = null
)
